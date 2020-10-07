package org.openrs2.deob.filter

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.filter.AnyMemberFilter
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.asm.hasCode

public class BrowserControlFilter private constructor(private val clazz: String) : MemberFilter {
    override fun matches(owner: String, name: String, desc: String): Boolean {
        return clazz == owner
    }

    public companion object {
        private val logger = InlineLogger()

        public fun create(classPath: ClassPath): MemberFilter {
            val browserControlClass = findBrowserControlClass(classPath)

            return if (browserControlClass != null) {
                logger.info { "Identified browser control class $browserControlClass" }
                BrowserControlFilter(browserControlClass)
            } else {
                logger.warn { "Failed to identify browser control class" }
                AnyMemberFilter
            }
        }

        private fun findBrowserControlClass(classPath: ClassPath): String? {
            for (library in classPath.libraries) {
                for (clazz in library) {
                    if (isBrowserControlClass(clazz)) {
                        return clazz.name
                    }
                }
            }

            return null
        }

        private fun isBrowserControlClass(clazz: ClassNode): Boolean {
            for (method in clazz.methods) {
                if (!method.hasCode) {
                    continue
                }

                for (insn in method.instructions) {
                    if (insn !is MethodInsnNode) {
                        continue
                    } else if (insn.owner == "netscape/javascript/JSObject") {
                        return true
                    }
                }
            }

            return false
        }
    }
}
