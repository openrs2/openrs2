package dev.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.Library
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode

object SignedClassUtils {
    private val logger = InlineLogger()
    private val LOAD_SIGNED_CLASS_MATCHER =
        InsnMatcher.compile("LDC INVOKESTATIC ASTORE ALOAD GETFIELD ALOAD INVOKEVIRTUAL ALOAD INVOKEVIRTUAL POP")

    @JvmStatic
    fun move(loader: Library, client: Library, signLink: Library) {
        // find signed classes
        val signedClasses = findSignedClasses(loader)
        logger.info { "Identified signed classes $signedClasses" }

        val dependencies = findDependencies(loader, signedClasses)
        logger.info { "Identified signed class dependencies $dependencies" }

        // rename dependencies of signed classes so they don't clash with client classes
        val mapping = mutableMapOf<String, String>()
        for (dependency in dependencies) {
            mapping[dependency] = "loader_$dependency"
        }
        val remapper = SimpleRemapper(mapping)

        // delete original signed classes (these have no dependencies)
        for (name in signedClasses) {
            client.remove(name)
        }

        // move loader signed classes to signlink
        for (name in signedClasses.union(dependencies)) {
            val `in` = loader.remove(name)!!

            val out = ClassNode()
            `in`.accept(ClassRemapper(out, remapper))

            signLink.add(out)
        }
    }

    private fun findSignedClasses(loader: Library): Set<String> {
        val clazz = loader["loader"] ?: throw IllegalArgumentException("Failed to find loader class")

        val method = clazz.methods.find { it.name == "run" && it.desc == "()V" }
            ?: throw IllegalArgumentException("Failed to find loader.run() method")

        return findSignedClasses(method)
    }

    private fun findSignedClasses(method: MethodNode): Set<String> {
        val classes = mutableSetOf<String>()

        LOAD_SIGNED_CLASS_MATCHER.match(method).forEach {
            val ldc = it[0] as LdcInsnNode
            val cst = ldc.cst
            if (cst is String && cst != "unpack") {
                classes.add(cst)
            }
        }

        return classes
    }

    private fun findDependencies(loader: Library, signedClasses: Set<String>): Set<String> {
        val dependencies = mutableSetOf<String>()

        for (signedClass in signedClasses) {
            val clazz = loader[signedClass]!!

            for (field in clazz.fields) {
                val type = Type.getType(field.desc)
                if (type.sort != Type.OBJECT) {
                    continue
                }

                val name = type.className
                if (loader.contains(name) && !signedClasses.contains(name)) {
                    dependencies.add(name)
                }
            }
        }

        return dependencies
    }
}
