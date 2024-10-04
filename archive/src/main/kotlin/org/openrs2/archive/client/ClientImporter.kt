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
import org.glavo.pack200.Pack200
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.openrs2.archive.cache.CacheExporter
import org.openrs2.archive.cache.CacheImporter
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.getArgumentExpressions
import org.openrs2.asm.hasCode
import org.openrs2.asm.intConstant
import org.openrs2.asm.io.CabLibraryReader
import org.openrs2.asm.io.JarLibraryReader
import org.openrs2.asm.io.LibraryReader
import org.openrs2.asm.io.Pack200LibraryReader
import org.openrs2.asm.io.PackClassLibraryReader
import org.openrs2.asm.nextReal
import org.openrs2.asm.previousReal
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
import kotlin.io.path.getLastModifiedTime

@Singleton
public class ClientImporter @Inject constructor(
    private val database: Database,
    private val alloc: ByteBufAllocator,
    private val packClassLibraryReader: PackClassLibraryReader,
    private val importer: CacheImporter
) {
    public suspend fun import(
        paths: Iterable<Path>,
        name: String?,
        description: String?,
        url: String?,
        skipErrors: Boolean
    ) {
        alloc.buffer().use { buf ->
            for (path in paths) {
                buf.clear()

                Files.newInputStream(path).use { input ->
                    ByteBufOutputStream(buf).use { output ->
                        input.copyTo(output)
                    }
                }

                logger.info { "Importing $path" }
                try {
                    import(
                        parse(buf),
                        name,
                        description,
                        url,
                        path.fileName.toString(),
                        path.getLastModifiedTime().toInstant()
                    )
                } catch (t: Throwable) {
                    if (skipErrors) {
                        logger.warn(t) { "Failed to import $path" }
                        continue
                    }

                    throw t
                }
            }
        }
    }

    public suspend fun import(
        artifact: Artifact,
        name: String?,
        description: String?,
        url: String?,
        fileName: String,
        timestamp: Instant
    ) {
        database.execute { connection ->
            importer.prepare(connection)

            val id = import(connection, artifact)

            connection.prepareStatement(
                """
                INSERT INTO artifact_sources (blob_id, name, description, url, file_name, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ).use { stmt ->
                stmt.setLong(1, id)
                stmt.setString(2, name)
                stmt.setString(3, description)
                stmt.setString(4, url)
                stmt.setString(5, fileName)
                stmt.setObject(6, timestamp.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)

                stmt.execute()
            }

            resolveBuilds(connection)
        }
    }

    private fun import(connection: Connection, artifact: Artifact): Long {
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

        return id
    }

    public suspend fun refresh() {
        data class Blob(val id: Long, val bytes: ByteArray)

        database.execute { connection ->
            importer.prepare(connection)

            var lastId: Long? = null
            val blobs = mutableListOf<Blob>()

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
                            val id = rows.getLong(1)
                            lastId = id
                            blobs += Blob(id, rows.getBytes(2))
                        }
                    }
                }

                if (blobs.isEmpty()) {
                    break
                }

                for (blob in blobs) {
                    logger.info { "Refreshing artifact ${blob.id}" }

                    Unpooled.wrappedBuffer(blob.bytes).use { buf ->
                        import(connection, parse(buf))
                    }
                }
            }

            resolveBuilds(connection)
        }
    }

    private fun resolveBuilds(connection: Connection) {
        connection.prepareStatement(
            """
            UPDATE artifacts a
            SET
                resolved_build_major = t.build_major,
                resolved_build_minor = t.build_minor
            FROM (
                SELECT DISTINCT ON (a1.blob_id)
                    a1.blob_id,
                    coalesce(a1.build_major, a2.build_major) build_major,
                    coalesce(a1.build_minor, a2.build_minor) build_minor
                FROM artifacts a1
                LEFT JOIN artifact_links al ON al.blob_id = a1.blob_id
                LEFT JOIN blobs b2 ON b2.sha1 = al.sha1
                LEFT JOIN artifacts a2 ON a2.blob_id = b2.id
                ORDER BY a1.blob_id ASC, b2.id ASC
            ) t
            WHERE a.blob_id = t.blob_id
        """.trimIndent()).use { stmt ->
            stmt.execute()
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
        } else if (
            buf.hasPrefix(MACHO32BE) ||
            buf.hasPrefix(MACHO32LE) ||
            buf.hasPrefix(MACHO64BE) ||
            buf.hasPrefix(MACHO64LE) ||
            buf.hasPrefix(MACHO_UNIVERSAL)
        ) {
            parseMachO(buf)
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
            } else if (name.startsWith("jaggl_OpenGL_")) {
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

        val dllName = getDllName(buf, pe)
        val symbols = parsePeExportNames(buf, pe).toSet()

        /*
         * There are no non-obfuscated symbols exported by sw3d, so we can only
         * identify it by the DLL name.
         */
        val type = if (dllName == "sw3d.dll" && symbols.any { symbol -> symbol.startsWith("_Java_") }) {
            ArtifactType.SW3D
        } else {
            getArtifactType(symbols.asSequence())
        }
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

    private fun getDllName(buf: ByteBuf, pe: PE): String {
        val namePointer = pe.sectionTable.rvaConverter.convertVirtualAddressToRawDataPointer(pe.imageData.exportTable.nameRVA.toInt())

        val end = buf.forEachByte(namePointer, buf.writerIndex() - namePointer, ByteProcessor.FIND_NUL)
        require(end != -1) {
            "Unterminated string"
        }

        return buf.toString(namePointer, end - namePointer, Charsets.US_ASCII)
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

    private fun parseMachO(buf: ByteBuf): Artifact {
        val (arch, symbols) = MachO.parse(buf.slice())
        val type = getArtifactType(symbols.asSequence())

        return Artifact(
            buf.retain(),
            "shared",
            "live",
            null,
            null,
            type,
            ArtifactFormat.NATIVE,
            OperatingSystem.MACOS,
            arch,
            Jvm.SUN,
            emptyList()
        )
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
            build = parseClientBuild(library, client)
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
                build = parseSignLinkBuild(library)
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
        } else if (library.contains("loginapplet")) {
            game = "loginapplet"
            build = null
            type = ArtifactType.CLIENT
            links = emptyList()
        } else if (library.contains("passwordapp")) {
            game = "passapplet"
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

    private fun parseClientBuild(library: Library, clazz: ClassNode): CacheExporter.Build? {
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

            for (build in NEW_ENGINE_RESOLUTIONS) {
                candidates -= build
            }

            val version = candidates.singleOrNull()
            if (version != null) {
                return CacheExporter.Build(version, null)
            }
        }

        return parseSignLinkBuild(library)
    }

    private fun parseSignLinkBuild(library: Library): CacheExporter.Build? {
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

        val loader = library["loader"]
        if (loader != null) {
            val links = parseLoaderLinks(loader)
            if (links.isNotEmpty()) {
                return links
            }

            return parseResourceLinks(library)
        }

        // TODO(gpe)
        return emptyList()
    }

    private fun parseLoaderLinks(loader: ClassNode): List<ArtifactLink> {
        val links = mutableListOf<ArtifactLink>()
        val paths = mutableSetOf<String>()

        for (method in loader.methods) {
            if (method.name != "run" || method.desc != "()V") {
                continue
            }

            for (insn in method.instructions) {
                if (insn !is MethodInsnNode || insn.owner != loader.name || !insn.desc.endsWith(")[B")) {
                    continue
                }

                // TODO(gpe): extract file size too (tricky due to dummy arguments)

                val exprs = getArgumentExpressions(insn) ?: continue
                for (expr in exprs) {
                    val single = expr.singleOrNull() ?: continue
                    if (single !is LdcInsnNode) {
                        continue
                    }

                    val cst = single.cst
                    if (cst is String && FILE_NAME_REGEX.matches(cst)) {
                        paths += cst
                    }
                }
            }
        }

        val hashes = mutableMapOf<AbstractInsnNode, ByteArray>()

        for (method in loader.methods) {
            for (match in SHA1_CMP_MATCHER.match(method)) {
                val sha1 = ByteArray(SHA1_BYTES)
                var i = 0

                while (i < match.size) {
                    var n = match[i++].intConstant
                    if (n != null) {
                        i++ // ALOAD
                    }

                    val index = match[i++].intConstant!!
                    i++ // BALOAD

                    var xor = false
                    if (i + 1 < match.size && match[i + 1].opcode == Opcodes.IXOR) {
                        i += 2 // ICONST_M1, IXOR
                        xor = true
                    }

                    if (match[i].opcode == Opcodes.IFNE) {
                        n = 0
                        i++
                    } else {
                        if (n == null) {
                            n = match[i++].intConstant!!
                        }

                        i++ // ICMP_IFNE
                    }

                    if (xor) {
                        n = n.inv()
                    }

                    sha1[index] = n.toByte()
                }

                hashes[match[0]] = sha1
            }
        }

        for (method in loader.methods) {
            for (match in PATH_CMP_MATCHER.match(method)) {
                val first = match[0]
                val ldc = if (first is LdcInsnNode) {
                    first
                } else {
                    match[1] as LdcInsnNode
                }

                val path = ldc.cst
                if (path !is String) {
                    continue
                }

                val acmp = match[2] as JumpInsnNode
                val target = if (acmp.opcode == Opcodes.IF_ACMPNE) {
                    acmp.nextReal
                } else {
                    acmp.label.nextReal
                }

                val hash = hashes.remove(target) ?: continue
                if (!paths.remove(path)) {
                    logger.warn { "Adding link for unused file $path" }
                }

                links += parseLink(path, hash, null)
            }
        }

        if (paths.size != hashes.size || paths.size > 1) {
            throw IllegalArgumentException()
        } else if (paths.size == 1) {
            links += parseLink(paths.single(), hashes.values.single(), null)
        }

        return links
    }

    private fun parseResourceLinks(library: Library): List<ArtifactLink> {
        val links = mutableListOf<ArtifactLink>()

        for (clazz in library) {
            val clinit = clazz.methods.firstOrNull { it.name == "<clinit>" && it.desc == "()V" }
            if (clinit != null) {
                for (match in RESOURCE_CTOR_MATCHER.match(clinit)) {
                    val srcLdc = match[1] as LdcInsnNode
                    val src = srcLdc.cst
                    if (src !is String) {
                        continue
                    }

                    val newArray = match.single { it.opcode == Opcodes.NEWARRAY }

                    val off = match.indexOf(newArray) + 3
                    val sha1 = ByteArray(SHA1_BYTES) { i ->
                        val insn = match[off + i * 4]
                        insn.intConstant!!.toByte()
                    }

                    val size = newArray.previousReal!!.previousReal!!.previousReal!!.intConstant!!

                    links += parseLink(src, sha1, size)
                }
            }
        }

        return links
    }

    private fun parseLink(path: String, sha1: ByteArray, size: Int?): ArtifactLink {
        val m = FILE_NAME_REGEX.matchEntire(path) ?: throw IllegalArgumentException(path)
        val (name, namePrefix, crc1, ext, crc2) = m.destructured

        val format = when (ext) {
            "pack200" -> ArtifactFormat.PACK200
            "js5" -> ArtifactFormat.PACKCLASS
            "jar", "pack" -> ArtifactFormat.JAR
            "dll", "lib" -> ArtifactFormat.NATIVE
            else -> throw IllegalArgumentException(ext)
        }

        val type: ArtifactType
        val os: OperatingSystem
        val arch: Architecture
        val jvm: Jvm

        if (format == ArtifactFormat.NATIVE) {
            data class Tuple(
                val type: ArtifactType,
                val os: OperatingSystem,
                val arch: Architecture,
                val jvm: Jvm
            )

            val tuple = when (name) {
                "browsercontrol" -> Tuple(ArtifactType.BROWSERCONTROL, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jaggl_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jaggl_1" -> Tuple(ArtifactType.JAGGL, OperatingSystem.WINDOWS, Architecture.AMD64, Jvm.SUN)
                "jaggl_2" -> Tuple(ArtifactType.JAGGL, OperatingSystem.LINUX, Architecture.X86, Jvm.SUN)
                "jaggl_3" -> Tuple(ArtifactType.JAGGL, OperatingSystem.LINUX, Architecture.AMD64, Jvm.SUN)
                "jaggl_4" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.POWERPC, Jvm.SUN)
                // TODO: is jaggl_5 correct? the loader doesn't use it
                "jaggl_5" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.X86, Jvm.SUN)
                "jaggl_6" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.AMD64, Jvm.SUN)
                "jaggl_7" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.UNIVERSAL, Jvm.SUN)
                "jaggl_0_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jaggl_1_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.LINUX, Architecture.X86, Jvm.SUN)
                "jaggl_1_1" -> Tuple(ArtifactType.JAGGL_DRI, OperatingSystem.LINUX, Architecture.X86, Jvm.SUN)
                "jaggl_2_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.POWERPC, Jvm.SUN)
                "jaggl_3_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.X86, Jvm.SUN)
                "jaggl_4_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.WINDOWS, Architecture.AMD64, Jvm.SUN)
                "jaggl_5_0" -> Tuple(ArtifactType.JAGGL, OperatingSystem.MACOS, Architecture.AMD64, Jvm.SUN)
                "jagmisc_0" -> Tuple(ArtifactType.JAGMISC, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jagmisc_1" -> Tuple(ArtifactType.JAGMISC, OperatingSystem.WINDOWS, Architecture.X86, Jvm.MICROSOFT)
                "jagmisc_2" -> Tuple(ArtifactType.JAGMISC, OperatingSystem.WINDOWS, Architecture.AMD64, Jvm.SUN)
                "jogl" -> Tuple(ArtifactType.JOGL, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jogl_awt" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jogl_0_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jogl_0_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                "jogl_1_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.WINDOWS, Architecture.AMD64, Jvm.SUN)
                "jogl_1_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.WINDOWS, Architecture.AMD64, Jvm.SUN)
                "jogl_2_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.LINUX, Architecture.X86, Jvm.SUN)
                "jogl_2_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.LINUX, Architecture.X86, Jvm.SUN)
                "jogl_2_2" -> Tuple(ArtifactType.GLUEGEN_RT, OperatingSystem.LINUX, Architecture.X86, Jvm.SUN)
                "jogl_3_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.LINUX, Architecture.AMD64, Jvm.SUN)
                "jogl_3_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.LINUX, Architecture.AMD64, Jvm.SUN)
                "jogl_3_2" -> Tuple(ArtifactType.GLUEGEN_RT, OperatingSystem.LINUX, Architecture.AMD64, Jvm.SUN)
                "jogl_4_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.MACOS, Architecture.POWERPC, Jvm.SUN)
                "jogl_4_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.MACOS, Architecture.POWERPC, Jvm.SUN)
                "jogl_5_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.MACOS, Architecture.UNIVERSAL, Jvm.SUN)
                "jogl_5_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.MACOS, Architecture.UNIVERSAL, Jvm.SUN)
                "jogl_6_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.SOLARIS, Architecture.AMD64, Jvm.SUN)
                "jogl_6_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.SOLARIS, Architecture.AMD64, Jvm.SUN)
                "jogl_6_2" -> Tuple(ArtifactType.GLUEGEN_RT, OperatingSystem.SOLARIS, Architecture.AMD64, Jvm.SUN)
                "jogl_7_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.SOLARIS, Architecture.X86, Jvm.SUN)
                "jogl_7_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.SOLARIS, Architecture.X86, Jvm.SUN)
                "jogl_7_2" -> Tuple(ArtifactType.GLUEGEN_RT, OperatingSystem.SOLARIS, Architecture.X86, Jvm.SUN)
                "jogl_8_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.SOLARIS, Architecture.SPARC, Jvm.SUN)
                "jogl_8_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.SOLARIS, Architecture.SPARC, Jvm.SUN)
                "jogl_8_2" -> Tuple(ArtifactType.GLUEGEN_RT, OperatingSystem.SOLARIS, Architecture.SPARC, Jvm.SUN)
                "jogl_9_0" -> Tuple(ArtifactType.JOGL, OperatingSystem.SOLARIS, Architecture.SPARCV9, Jvm.SUN)
                "jogl_9_1" -> Tuple(ArtifactType.JOGL_AWT, OperatingSystem.SOLARIS, Architecture.SPARCV9, Jvm.SUN)
                "jogl_9_2" -> Tuple(ArtifactType.GLUEGEN_RT, OperatingSystem.SOLARIS, Architecture.SPARCV9, Jvm.SUN)
                "sw3d_0" -> Tuple(ArtifactType.SW3D, OperatingSystem.WINDOWS, Architecture.X86, Jvm.SUN)
                else -> throw IllegalArgumentException(name)
            }

            type = tuple.type
            os = tuple.os
            arch = tuple.arch
            jvm = tuple.jvm
        } else {
            // TODO(gpe): funorb loaders
            type = when (namePrefix) {
                "runescape", "client" -> ArtifactType.CLIENT
                "runescape_gl" -> ArtifactType.CLIENT_GL
                "unpackclass" -> ArtifactType.UNPACKCLASS
                "jogl", "jogltrimmed" -> ArtifactType.JOGL
                "jaggl" -> ArtifactType.JAGGL
                else -> throw IllegalArgumentException(namePrefix)
            }
            os = OperatingSystem.INDEPENDENT
            arch = Architecture.INDEPENDENT
            jvm = Jvm.INDEPENDENT
        }

        val crc = crc1.toIntOrNull() ?: crc2.toIntOrNull() ?: throw IllegalArgumentException()

        return ArtifactLink(
            type,
            format,
            os,
            arch,
            jvm,
            crc,
            sha1,
            size
        )
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

        private val FILE_NAME_REGEX = Regex("(([a-z0-9_]+?)(?:_[0-9]){0,2})(?:_(-?[0-9]+))?[.]([a-z0-9]+)(?:\\?crc=(-?[0-9]+))?")
        private val SHA1_CMP_MATCHER =
            InsnMatcher.compile("((ICONST | BIPUSH)? ALOAD (ICONST | BIPUSH) BALOAD (ICONST IXOR)? (ICONST | BIPUSH)? (IF_ICMPEQ | IF_ICMPNE | IFEQ | IFNE))+")
        private val PATH_CMP_MATCHER = InsnMatcher.compile("(LDC ALOAD | ALOAD LDC) (IF_ACMPEQ | IF_ACMPNE)")

        private val RESOURCE_CTOR_MATCHER = InsnMatcher.compile("LDC LDC (LDC+ | ICONST ANEWARRAY (DUP ICONST LDC AASTORE)+) (ICONST | BIPUSH | SIPUSH | LDC) (ICONST | BIPUSH | SIPUSH | LDC) BIPUSH NEWARRAY (DUP (ICONST | BIPUSH) (ICONST | BIPUSH) IASTORE)+ INVOKESPECIAL")
    }
}
