package org.openrs2.deob.bytecode.library

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Type
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.Library
import org.openrs2.deob.util.module.Module
import org.openrs2.deob.util.module.ModuleType
import org.openrs2.deob.util.module.ModuleType.CLIENT
import org.openrs2.deob.util.module.ModuleType.LOADER
import org.openrs2.deob.util.module.ModuleType.SIGNLINK

@Singleton
public class SignedClassPreprocessor : LibraryPreprocessor(LOADER, CLIENT, SIGNLINK) {

    override fun preprocess(modules: Map<ModuleType, Module>, libraries: Map<ModuleType, Library>) {
        // overwrite client's classes with signed classes from the loader
        val loader = libraries[LOADER]!!
        val client = libraries[CLIENT]!!
        val signLink = libraries[SIGNLINK]!!

        move(loader, client, signLink)
    }

    private fun move(loader: Library, client: Library, signLink: Library) {
        logger.info { "Moving signed classes from loader to signlink" }

        val signedClasses = findSignedClasses(loader)
        logger.debug { "Identified signed classes $signedClasses" }

        val dependencies = findDependencies(loader, signedClasses)
        logger.debug { "Identified signed class dependencies $dependencies" }

        // delete original signed classes (these have no dependencies)
        for (name in signedClasses) {
            client.remove(name)
        }

        // move loader signed classes to signlink
        for (name in signedClasses union dependencies) {
            signLink.add(loader.remove(name)!!)
        }
    }

    private fun findSignedClasses(loader: Library): Set<String> {
        val clazz = requireNotNull(loader["loader"]) { "Failed to find loader class" }

        val method = requireNotNull(clazz.methods.find { it.name == "run" && it.desc == "()V" }) {
            "Failed to find loader.run() method"
        }

        return findSignedClasses(method)
    }

    private fun findSignedClasses(method: MethodNode): Set<String> {
        val classes = mutableSetOf<String>()

        for (match in LOAD_SIGNED_CLASS_MATCHER.match(method)) {
            val ldc = match[0] as LdcInsnNode
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
                if (name in loader && name !in signedClasses) {
                    dependencies.add(name)
                }
            }
        }

        return dependencies
    }

    private companion object {
        private val logger = InlineLogger()
        private val LOAD_SIGNED_CLASS_MATCHER =
            InsnMatcher.compile("LDC INVOKESTATIC ASTORE ALOAD GETFIELD ALOAD INVOKEVIRTUAL ALOAD INVOKEVIRTUAL POP")
    }
}
