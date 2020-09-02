package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.toAbstractInsnNode
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.bundler.Resource
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

public class ResourceTransformer(
    private val resources: List<Resource>? = null,
    private val glResources: List<List<Resource>> = Resource.compressGlNatives(),
    private val miscResources: List<Resource> = Resource.compressMiscNatives()
) : Transformer() {
    private var glBlocks = 0
    private var miscBlocks = 0

    override fun preTransform(classPath: ClassPath) {
        glBlocks = 0
        miscBlocks = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if (resources != null) {
            for ((i, match) in RESOURCE_MATCHER.match(method).withIndex()) {
                val resource = resources[i]

                // sanity check that the destination matches up
                val destination = match[2] as LdcInsnNode
                require(destination.cst == resource.destination)

                // update the source (the CRC may have changed)
                val source = match[3] as LdcInsnNode
                source.cst = resource.sourceWithChecksum

                // update file sizes
                method.instructions.set(match[22], resource.uncompressedSize.toAbstractInsnNode())
                method.instructions.set(match[23], resource.compressedSize.toAbstractInsnNode())

                // update digest
                for ((j, byte) in resource.digest.withIndex()) {
                    method.instructions.set(match[28 + 4 * j], byte.toInt().toAbstractInsnNode())
                }
            }
        }

        val match = GL_RESOURCES_MATCHER.match(method).singleOrNull()
        if (match != null) {
            // remove everything but the PUTSTATIC
            for (i in 0 until match.size - 1) {
                method.instructions.remove(match[i])
            }

            // find the type of the resource class
            val new = match[8] as TypeInsnNode
            val type = new.desc

            // create our own resource array
            val list = InsnList()
            create2dResourceArray(list, type, glResources, GL_LOADING_MESSAGES)

            // insert our own resource array before the PUTSTATIC
            method.instructions.insertBefore(match[match.size - 1], list)

            glBlocks++
        }

        val miscMatch = MISC_RESOURCES_MATCHER.match(method).singleOrNull()
        if (miscMatch != null) {
            // remove everything but the PUTSTATIC
            for (i in 0 until miscMatch.size - 1) {
                method.instructions.remove(miscMatch[i])
            }

            // find the type of the resource class
            val new = miscMatch[4] as TypeInsnNode
            val type = new.desc

            // create our own resource array
            val list = InsnList()
            createResourceArray(list, type, miscResources, MISC_LOADING_MESSAGES)

            // insert our own resource array before the PUTSTATIC
            method.instructions.insertBefore(miscMatch[miscMatch.size - 1], list)

            miscBlocks++
        }

        return false
    }

    private fun create2dResourceArray(
        list: InsnList,
        type: String,
        resources: List<List<Resource>>,
        messages: List<String>
    ) {
        list.add(resources.size.toAbstractInsnNode())
        list.add(TypeInsnNode(Opcodes.ANEWARRAY, "[L$type;"))

        for ((i, innerResources) in resources.withIndex()) {
            list.add(InsnNode(Opcodes.DUP))
            list.add(i.toAbstractInsnNode())

            createResourceArray(list, type, innerResources, messages, progressSufix = true)

            list.add(InsnNode(Opcodes.AASTORE))
        }
    }

    private fun createResourceArray(
        list: InsnList,
        type: String,
        resources: List<Resource>,
        messages: List<String>,
        progressSufix: Boolean = false
    ) {
        list.add(resources.size.toAbstractInsnNode())
        list.add(TypeInsnNode(Opcodes.ANEWARRAY, type))

        for ((i, resource) in resources.withIndex()) {
            list.add(InsnNode(Opcodes.DUP))
            list.add(i.toAbstractInsnNode())

            list.add(TypeInsnNode(Opcodes.NEW, type))
            list.add(InsnNode(Opcodes.DUP))
            list.add(LdcInsnNode(resource.destination))
            list.add(LdcInsnNode(resource.sourceWithChecksum))

            list.add(messages.size.toAbstractInsnNode())
            list.add(TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"))

            for ((j, message) in messages.withIndex()) {
                val messageWithSuffix = if (progressSufix && resources.size > 1) {
                    "$message (${i + 1}/${resources.size})"
                } else {
                    message
                }

                list.add(InsnNode(Opcodes.DUP))
                list.add(j.toAbstractInsnNode())
                list.add(LdcInsnNode(messageWithSuffix))
                list.add(InsnNode(Opcodes.AASTORE))
            }

            list.add(resource.uncompressedSize.toAbstractInsnNode())
            list.add(resource.compressedSize.toAbstractInsnNode())

            list.add(resource.digest.size.toAbstractInsnNode())
            list.add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT))

            for ((j, byte) in resource.digest.withIndex()) {
                list.add(InsnNode(Opcodes.DUP))
                list.add(j.toAbstractInsnNode())
                list.add(byte.toInt().toAbstractInsnNode())
                list.add(InsnNode(Opcodes.IASTORE))
            }

            list.add(
                MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    type,
                    "<init>",
                    "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;II[I)V"
                )
            )
            list.add(InsnNode(Opcodes.AASTORE))
        }
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $glBlocks jaggl and $miscBlocks jagmisc resource constructor blocks" }
    }

    private companion object {
        private val logger = InlineLogger()

        private const val RESOURCE_CONSTRUCTOR = """
            NEW DUP
            LDC
            LDC
            ICONST_4 ANEWARRAY (DUP ICONST LDC AASTORE)+
            (ICONST | BIPUSH | SIPUSH | LDC)
            (ICONST | BIPUSH | SIPUSH | LDC)
            BIPUSH
            NEWARRAY
            (DUP (ICONST | BIPUSH) (ICONST | BIPUSH) IASTORE)+
            INVOKESPECIAL
        """
        private const val RESOURCE_ARRAY_CONSTRUCTOR = "ICONST ANEWARRAY (DUP ICONST $RESOURCE_CONSTRUCTOR AASTORE)+"

        private val RESOURCE_MATCHER = InsnMatcher.compile("$RESOURCE_CONSTRUCTOR PUTSTATIC")
        private val GL_RESOURCES_MATCHER = InsnMatcher.compile(
            """
            ICONST ANEWARRAY
            (
                DUP ICONST
                $RESOURCE_ARRAY_CONSTRUCTOR
                AASTORE
            )+
            PUTSTATIC
            """
        )
        private val MISC_RESOURCES_MATCHER = InsnMatcher.compile("$RESOURCE_ARRAY_CONSTRUCTOR PUTSTATIC")

        private val GL_LOADING_MESSAGES = listOf(
            "Loading 3D library",
            "Lade 3D-Softwarebibliothek",
            "Chargement de la librairie 3D",
            "Carregando biblioteca 3D"
        )
        private val MISC_LOADING_MESSAGES = listOf(
            "Loading utility library",
            "Lade Utility-Softwarebibliothek",
            "Chargement de la librairie utilitaire",
            "Carregando biblioteca de ferramentas"
        )
    }
}
