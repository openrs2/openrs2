package org.openrs2.archive.client

import com.github.michaelbull.logging.InlineLogger
import com.kichik.pecoff4j.PE
import com.kichik.pecoff4j.constant.MachineType
import com.kichik.pecoff4j.io.PEParser
import dorkbox.cabParser.CabParser
import dorkbox.cabParser.CabStreamSaver
import dorkbox.cabParser.structure.CabFileEntry
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.util.ByteProcessor
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.fornwall.jelf.ElfFile
import net.fornwall.jelf.ElfSymbol
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.openrs2.archive.cache.CacheExporter
import org.openrs2.archive.cache.CacheImporter
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.hasCode
import org.openrs2.asm.intConstant
import org.openrs2.asm.io.CabLibraryReader
import org.openrs2.asm.io.JarLibraryReader
import org.openrs2.asm.io.LibraryReader
import org.openrs2.asm.io.Pack200LibraryReader
import org.openrs2.asm.io.PackClassLibraryReader
import org.openrs2.buffer.use
import org.openrs2.compress.gzip.Gzip
import org.openrs2.db.Database
import org.openrs2.util.io.entries
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Pack200

@Singleton
public class ClientImporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator,
    private val packClassLibraryReader: PackClassLibraryReader,
    private val importer: CacheImporter
) {
    public suspend fun import(paths: Iterable<Path>) {
        alloc.buffer().use { buf ->
            for (path in paths) {
                buf.clear()

                Files.newInputStream(path).use { input ->
                    ByteBufOutputStream(buf).use { output ->
                        input.copyTo(output)
                    }
                }

                logger.info { "Importing $path" }
                import(parse(buf))
            }
        }
    }

    public suspend fun import(artifact: Artifact) {
        database.execute { connection ->
            importer.prepare(connection)
            import(connection, artifact)
        }
    }

    private fun import(connection: Connection, artifact: Artifact) {
        val id = importer.addBlob(connection, artifact)

        val gameId = connection.prepareStatement(
            """
            SELECT id
            FROM games
            WHERE name = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, artifact.game)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    throw IllegalArgumentException()
                }

                rows.getInt(1)
            }
        }

        val environmentId = connection.prepareStatement(
            """
            SELECT id
            FROM environments
            WHERE name = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, artifact.environment)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    throw IllegalArgumentException()
                }

                rows.getInt(1)
            }
        }

        connection.prepareStatement(
            """
            INSERT INTO artifacts (blob_id, game_id, environment_id, build_major, build_minor, timestamp, type, format, os, arch, jvm)
            VALUES (?, ?, ?, ?, ?, ?, ?::artifact_type, ?::artifact_format, ?::os, ?::arch, ?::jvm)
            ON CONFLICT (blob_id) DO UPDATE SET
                game_id = EXCLUDED.game_id,
                environment_id = EXCLUDED.environment_id,
                build_major = EXCLUDED.build_major,
                build_minor = EXCLUDED.build_minor,
                timestamp = EXCLUDED.timestamp,
                type = EXCLUDED.type,
                format = EXCLUDED.format,
                os = EXCLUDED.os,
                arch = EXCLUDED.arch,
                jvm = EXCLUDED.jvm
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.setInt(2, gameId)
            stmt.setInt(3, environmentId)
            stmt.setObject(4, artifact.build?.major, Types.INTEGER)
            stmt.setObject(5, artifact.build?.minor, Types.INTEGER)
            stmt.setObject(6, artifact.timestamp?.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
            stmt.setString(7, artifact.type.name.lowercase())
            stmt.setString(8, artifact.format.name.lowercase())
            stmt.setString(9, artifact.os.name.lowercase())
            stmt.setString(10, artifact.arch.name.lowercase())
            stmt.setString(11, artifact.jvm.name.lowercase())

            stmt.execute()
        }

        connection.prepareStatement(
            """
            DELETE FROM artifact_links
            WHERE blob_id = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setLong(1, id)
            stmt.execute()
        }

        connection.prepareStatement(
            """
            INSERT INTO artifact_links (blob_id, type, format, os, arch, jvm, sha1, crc32, size)
            VALUES (?, ?::artifact_type, ?::artifact_format, ?::os, ?::arch, ?::jvm, ?, ?, ?)
        """.trimIndent()
        ).use { stmt ->
            for (link in artifact.links) {
                stmt.setLong(1, id)
                stmt.setString(2, link.type.name.lowercase())
                stmt.setString(3, link.format.name.lowercase())
                stmt.setString(4, link.os.name.lowercase())
                stmt.setString(5, link.arch.name.lowercase())
                stmt.setString(6, link.jvm.name.lowercase())
                stmt.setBytes(7, link.sha1)
                stmt.setObject(8, link.crc32, Types.INTEGER)
                stmt.setObject(9, link.size, Types.INTEGER)

                stmt.addBatch()
            }

            stmt.executeBatch()
        }
    }

    public suspend fun refresh() {
        database.execute { connection ->
            importer.prepare(connection)

            var lastId: Long? = null
            val blobs = mutableListOf<ByteArray>()

            while (true) {
                blobs.clear()

                connection.prepareStatement(
                    """
                    SELECT a.blob_id, b.data
                    FROM artifacts a
                    JOIN blobs b ON b.id = a.blob_id
                    WHERE ? IS NULL OR a.blob_id > ?
                    ORDER BY a.blob_id ASC
                    LIMIT 1024
                """.trimIndent()
                ).use { stmt ->
                    stmt.setObject(1, lastId, Types.BIGINT)
                    stmt.setObject(2, lastId, Types.BIGINT)

                    stmt.executeQuery().use { rows ->
                        while (rows.next()) {
                            lastId = rows.getLong(1)
                            blobs += rows.getBytes(2)
                        }
                    }
                }

                if (blobs.isEmpty()) {
                    return@execute
                }

                for (blob in blobs) {
                    Unpooled.wrappedBuffer(blob).use { buf ->
                        import(connection, parse(buf))
                    }
                }
            }
        }
    }

    private fun parse(buf: ByteBuf): Artifact {
        return if (buf.hasPrefix(JAR)) {
            parseJar(buf)
        } else if (buf.hasPrefix(PACK200)) {
            parsePack200(buf)
        } else if (buf.hasPrefix(CAB)) {
            parseCab(buf)
        } else if (
            buf.hasPrefix(PACKCLASS_UNCOMPRESSED) ||
            buf.hasPrefix(PACKCLASS_BZIP2) ||
            buf.hasPrefix(PACKCLASS_GZIP)
        ) {
            parseLibrary(buf, packClassLibraryReader, ArtifactFormat.PACKCLASS)
        } else if (buf.hasPrefix(ELF)) {
            parseElf(buf)
        } else if (buf.hasPrefix(PE)) {
            parsePe(buf)
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun parseElf(buf: ByteBuf): Artifact {
        val elf = ElfFile.from(ByteBufInputStream(buf.slice()))

        val arch = when (elf.e_machine.toInt()) {
            ElfFile.ARCH_i386 -> Architecture.X86
            ElfFile.ARCH_X86_64 -> Architecture.AMD64
            ElfFile.ARCH_SPARC -> Architecture.SPARC
            ARCH_SPARCV9 -> Architecture.SPARCV9
            else -> throw IllegalArgumentException()
        }

        val comment = String(elf.firstSectionByName(".comment").data)
        val os = if (comment.contains(SOLARIS_COMMENT)) {
            OperatingSystem.SOLARIS
        } else {
            OperatingSystem.LINUX
        }

        val symbols = elf.dynamicSymbolTableSection ?: throw IllegalArgumentException()
        val type = getArtifactType(symbols.symbols.asSequence().mapNotNull(ElfSymbol::getName))

        return Artifact(
            buf.retain(),
            "shared",
            "live",
            null,
            null,
            type,
            ArtifactFormat.NATIVE,
            os,
            arch,
            Jvm.SUN,
            emptyList()
        )
    }

    private fun getArtifactType(symbols: Sequence<String>): ArtifactType {
        for (symbol in symbols) {
            var name = symbol
            if (name.startsWith('_')) {
                name = name.substring(1)
            }
            if (name.startsWith("Java_")) { // RNI methods don't have a Java_ prefix
                name = name.substring("Java_".length)
            }

            if (name.startsWith("jaggl_X11_dri_")) {
                return ArtifactType.JAGGL_DRI
            } else if (name.startsWith("jaggl_opengl_")) {
                return ArtifactType.JAGGL
            } else if (name.startsWith("com_sun_opengl_impl_GLImpl_")) {
                return ArtifactType.JOGL
            } else if (name.startsWith("com_sun_opengl_impl_JAWT_")) {
                return ArtifactType.JOGL_AWT
            } else if (name.startsWith("com_sun_gluegen_runtime_")) {
                return ArtifactType.GLUEGEN_RT
            } else if (name.startsWith("jagex3_jagmisc_jagmisc_")) {
                return ArtifactType.JAGMISC
            } else if (name.startsWith("nativeadvert_browsercontrol_")) {
                return ArtifactType.BROWSERCONTROL
            }
        }

        throw IllegalArgumentException()
    }

    private fun parsePe(buf: ByteBuf): Artifact {
        val pe = PEParser.parse(ByteBufInputStream(buf.slice()))

        val arch = when (pe.coffHeader.machine) {
            MachineType.IMAGE_FILE_MACHINE_I386 -> Architecture.X86
            MachineType.IMAGE_FILE_MACHINE_AMD64 -> Architecture.AMD64
            else -> throw IllegalArgumentException()
        }

        val symbols = parsePeExportNames(buf, pe).toSet()

        val type = getArtifactType(symbols.asSequence())
        val jvm = if (symbols.contains("RNIGetCompatibleVersion")) {
            Jvm.MICROSOFT
        } else {
            Jvm.SUN
        }

        return Artifact(
            buf.retain(),
            "shared",
            "live",
            null,
            Instant.ofEpochSecond(pe.coffHeader.timeDateStamp.toLong()),
            type,
            ArtifactFormat.NATIVE,
            OperatingSystem.WINDOWS,
            arch,
            jvm,
            emptyList()
        )
    }

    private fun parsePeExportNames(buf: ByteBuf, pe: PE): Sequence<String> {
        return sequence {
            val exportTable = pe.imageData.exportTable
            val namePointerTable =
                pe.sectionTable.rvaConverter.convertVirtualAddressToRawDataPointer(exportTable.namePointerRVA.toInt())

            for (i in 0 until exportTable.numberOfNamePointers.toInt()) {
                val namePointerRva = buf.readerIndex() + buf.getIntLE(buf.readerIndex() + namePointerTable + 4 * i)
                val namePointer = pe.sectionTable.rvaConverter.convertVirtualAddressToRawDataPointer(namePointerRva)

                val end = buf.forEachByte(namePointer, buf.writerIndex() - namePointer, ByteProcessor.FIND_NUL)
                require(end != -1) {
                    "Unterminated string"
                }

                yield(buf.toString(namePointer, end - namePointer, Charsets.US_ASCII))
            }
        }
    }

    private fun parseJar(buf: ByteBuf): Artifact {
        val timestamp = getJarTimestamp(ByteBufInputStream(buf.slice()))
        return parseLibrary(buf, JarLibraryReader, ArtifactFormat.JAR, timestamp)
    }

    private fun parsePack200(buf: ByteBuf): Artifact {
        val timestamp = ByteArrayOutputStream().use { tempOutput ->
            Gzip.createHeaderlessInputStream(ByteBufInputStream(buf.slice())).use { gzipInput ->
                JarOutputStream(tempOutput).use { jarOutput ->
                    Pack200.newUnpacker().unpack(gzipInput, jarOutput)
                }
            }

            getJarTimestamp(ByteArrayInputStream(tempOutput.toByteArray()))
        }

        return parseLibrary(buf, Pack200LibraryReader, ArtifactFormat.PACK200, timestamp)
    }

    private fun parseCab(buf: ByteBuf): Artifact {
        val timestamp = getCabTimestamp(ByteBufInputStream(buf.slice()))
        return parseLibrary(buf, CabLibraryReader, ArtifactFormat.CAB, timestamp)
    }

    private fun getJarTimestamp(input: InputStream): Instant? {
        var timestamp: Instant? = null

        JarInputStream(input).use { jar ->
            for (entry in jar.entries) {
                val t = entry.lastModifiedTime?.toInstant()
                if (timestamp == null || (t != null && t < timestamp)) {
                    timestamp = t
                }
            }
        }

        return timestamp
    }

    private fun getCabTimestamp(input: InputStream): Instant? {
        var timestamp: Instant? = null

        CabParser(input, object : CabStreamSaver {
            override fun closeOutputStream(outputStream: OutputStream, entry: CabFileEntry) {
                // entry
            }

            override fun openOutputStream(entry: CabFileEntry): OutputStream {
                val t = entry.date.toInstant()
                if (timestamp == null || t < timestamp) {
                    timestamp = t
                }

                return OutputStream.nullOutputStream()
            }

            override fun saveReservedAreaData(data: ByteArray?, dataLength: Int): Boolean {
                return false
            }
        }).extractStream()

        return timestamp
    }

    private fun parseLibrary(
        buf: ByteBuf,
        reader: LibraryReader,
        format: ArtifactFormat,
        timestamp: Instant? = null
    ): Artifact {
        val library = Library.read("client", ByteBufInputStream(buf.slice()), reader)

        val game: String
        val build: CacheExporter.Build?
        val type: ArtifactType
        val links: List<ArtifactLink>

        val mudclient = library["mudclient"]
        val client = library["client"]
        val loader = library["loader"]

        if (mudclient != null) {
            game = "classic"
            build = null // TODO(gpe): classic support
            type = ArtifactType.CLIENT
            links = emptyList()
        } else if (client != null) {
            game = "runescape"
            build = parseClientBuild(client)
            type = if (build != null && build.major < COMBINED_BUILD && isClientGl(library)) {
                ArtifactType.CLIENT_GL
            } else {
                ArtifactType.CLIENT
            }
            links = emptyList()
        } else if (loader != null) {
            if (isLoaderClassic(loader)) {
                game = "classic"
                build = null // TODO(gpe): classic support
                type = ArtifactType.LOADER
                links = emptyList() // TODO(gpe): classic support
            } else {
                game = "runescape"
                build = parseLoaderBuild(library)
                type = if (timestamp != null && timestamp < COMBINED_TIMESTAMP && isLoaderGl(library)) {
                    ArtifactType.LOADER_GL
                } else {
                    ArtifactType.LOADER
                }
                links = parseLinks(library)
            }
        } else if (library.contains("mapview")) {
            game = "mapview"
            build = null
            type = ArtifactType.CLIENT
            links = emptyList()
        } else if (library.contains("jaggl/opengl")) {
            game = "shared"
            type = ArtifactType.JAGGL
            build = null
            links = emptyList()
        } else if (library.contains("com/sun/opengl/impl/GLImpl")) {
            game = "shared"
            type = ArtifactType.JOGL
            build = null
            links = emptyList()
        } else if (library.contains("unpackclass")) {
            game = "shared"
            type = ArtifactType.UNPACKCLASS
            build = null
            links = emptyList()
        } else {
            throw IllegalArgumentException()
        }

        return Artifact(
            buf.retain(),
            game,
            "live",
            build,
            timestamp,
            type,
            format,
            OperatingSystem.INDEPENDENT,
            Architecture.INDEPENDENT,
            Jvm.INDEPENDENT,
            links
        )
    }

    private fun isClientGl(library: Library): Boolean {
        for (clazz in library) {
            for (method in clazz.methods) {
                if (!method.hasCode) {
                    continue
                }

                for (insn in method.instructions) {
                    if (insn is MethodInsnNode && insn.name == "glBegin") {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun isLoaderClassic(clazz: ClassNode): Boolean {
        for (method in clazz.methods) {
            if (!method.hasCode) {
                continue
            }

            for (insn in method.instructions) {
                if (insn is LdcInsnNode && insn.cst == "mudclient") {
                    return true
                }
            }
        }

        return false
    }

    private fun isLoaderGl(library: Library): Boolean {
        for (clazz in library) {
            for (method in clazz.methods) {
                if (!method.hasCode || method.name != "<clinit>") {
                    continue
                }

                for (insn in method.instructions) {
                    if (insn !is LdcInsnNode) {
                        continue
                    }

                    if (insn.cst == "jaggl.dll" || insn.cst == "jogl.dll") {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun parseClientBuild(clazz: ClassNode): CacheExporter.Build? {
        for (method in clazz.methods) {
            if (!method.hasCode || method.name != "main") {
                continue
            }

            for (match in OLD_ENGINE_VERSION_MATCHER.match(method)) {
                val ldc = match[0] as LdcInsnNode
                if (ldc.cst != OLD_ENGINE_VERSION_STRING) {
                    continue
                }

                val version = match[2].intConstant
                if (version != null) {
                    return CacheExporter.Build(version, null)
                }
            }

            var betweenNewAndReturn = false
            val candidates = mutableListOf<Int>()

            for (insn in method.instructions) {
                if (insn is TypeInsnNode && insn.desc == "client") {
                    betweenNewAndReturn = true
                } else if (insn.opcode == Opcodes.RETURN) {
                    break
                } else if (betweenNewAndReturn) {
                    val candidate = insn.intConstant
                    if (candidate != null && candidate in NEW_ENGINE_BUILDS) {
                        candidates += candidate
                    }
                }
            }

            candidates -= NEW_ENGINE_RESOLUTIONS

            val version = candidates.singleOrNull()
            if (version != null) {
                return CacheExporter.Build(version, null)
            }
        }

        return null
    }

    private fun parseLoaderBuild(library: Library): CacheExporter.Build? {
        val clazz = library["sign/signlink"] ?: return null

        for (field in clazz.fields) {
            val value = field.value
            if (field.name == "clientversion" && field.desc == "I" && value is Int) {
                return CacheExporter.Build(value, null)
            }
        }

        return null
    }

    private fun parseLinks(library: Library): List<ArtifactLink> {
        val sig = library["sig"]
        if (sig != null) {
            var size: Int? = null
            var sha1: ByteArray? = null

            for (field in sig.fields) {
                val value = field.value
                if (field.name == "len" && field.desc == "I" && value is Int) {
                    size = value
                }
            }

            for (method in sig.methods) {
                if (!method.hasCode || method.name != "<clinit>") {
                    continue
                }

                for (match in SHA1_MATCHER.match(method)) {
                    val len = match[0].intConstant
                    if (len != SHA1_BYTES) {
                        continue
                    }

                    sha1 = ByteArray(SHA1_BYTES)
                    for (i in 2 until match.size step 4) {
                        val k = match[i + 1].intConstant!!
                        val v = match[i + 2].intConstant!!
                        sha1[k] = v.toByte()
                    }
                }
            }

            require(size != null && sha1 != null)

            return listOf(
                ArtifactLink(
                    ArtifactType.CLIENT,
                    ArtifactFormat.JAR,
                    OperatingSystem.INDEPENDENT,
                    Architecture.INDEPENDENT,
                    Jvm.INDEPENDENT,
                    crc32 = null,
                    sha1,
                    size
                )
            )
        }

        // TODO(gpe): new engine support
        return emptyList()
    }

    private fun ByteBuf.hasPrefix(bytes: ByteArray): Boolean {
        Unpooled.wrappedBuffer(bytes).use { prefix ->
            val len = prefix.readableBytes()
            if (readableBytes() < len) {
                return false
            }

            return slice(readerIndex(), len) == prefix
        }
    }

    private companion object {
        private val logger = InlineLogger()

        private val CAB = byteArrayOf('M'.code.toByte(), 'S'.code.toByte(), 'C'.code.toByte(), 'F'.code.toByte())
        private val ELF = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
        private val JAR = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 0x03, 0x04)
        private val MACHO32BE = byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte())
        private val MACHO32LE = byteArrayOf(0xCE.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte())
        private val MACHO64BE = byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCF.toByte())
        private val MACHO64LE = byteArrayOf(0xCF.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte())
        private val MACHO_UNIVERSAL = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
        private val PACK200 = byteArrayOf(0x08)
        private val PACKCLASS_UNCOMPRESSED = byteArrayOf(0x00)
        private val PACKCLASS_BZIP2 = byteArrayOf(0x01)
        private val PACKCLASS_GZIP = byteArrayOf(0x02)
        private val PE = byteArrayOf('M'.code.toByte(), 'Z'.code.toByte())

        private const val OLD_ENGINE_VERSION_STRING = "RS2 user client - release #"
        private val OLD_ENGINE_VERSION_MATCHER =
            InsnMatcher.compile("LDC INVOKESPECIAL (ICONST | BIPUSH | SIPUSH | LDC)")

        private val NEW_ENGINE_RESOLUTIONS = listOf(765, 503, 1024, 768)
        private val NEW_ENGINE_BUILDS = 402..916

        private const val COMBINED_BUILD = 555
        private val COMBINED_TIMESTAMP = LocalDate.of(2009, Month.SEPTEMBER, 2)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()

        private const val ARCH_SPARCV9 = 43
        private const val SOLARIS_COMMENT = "Solaris Link Editors:"

        private const val SHA1_BYTES = 20
        private val SHA1_MATCHER =
            InsnMatcher.compile("BIPUSH NEWARRAY (DUP (ICONST | BIPUSH) (ICONST | BIPUSH | SIPUSH) IASTORE)+")
    }
}
