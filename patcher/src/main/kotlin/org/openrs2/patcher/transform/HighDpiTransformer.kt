package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.getExpression
import org.openrs2.asm.previousReal
import org.openrs2.asm.transform.Transformer
import kotlin.math.max

@Singleton
public class HighDpiTransformer : Transformer() {
    private var isGl: Boolean = false
    private var gameShell: String? = null
    private var newMembers: Int = 0
    private var initBlocks: Int = 0
    private var scaledCalls: Int = 0

    override fun preTransform(classPath: ClassPath) {
        newMembers = 0
        initBlocks = 0
        scaledCalls = 0

        isGl = classPath.libraryClasses.any { it.name in GL_IMPLS }

        gameShell = if (isGl) {
            val client = classPath["client!client"] ?: classPath["client"]
            client?.superClass?.name
        } else {
            null
        }

        if (gameShell != null) {
            logger.info { "Identified game shell: $gameShell" }
        }
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        if (!isGl || gameShell == null) {
            return false
        }

        when (clazz.name) {
            gameShell -> {
                addCanvasScaleField(clazz)
                newMembers++
            }

            in GL_INTERFACES -> {
                addPixelZoomMethod(clazz, Opcodes.ACC_ABSTRACT)
                newMembers++
            }

            in GL_IMPLS -> {
                addPixelZoomMethod(clazz, Opcodes.ACC_FINAL or Opcodes.ACC_NATIVE)
                newMembers++
            }
        }

        return false
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if (!isGl || gameShell == null) {
            return false
        }

        var extraStack = 0

        for (insn in method.instructions) {
            if (insn !is MethodInsnNode || insn.opcode != Opcodes.INVOKEINTERFACE) {
                continue
            } else if (insn.owner !in GL_INTERFACES) {
                continue
            }

            if (insn.name == "glViewport" && insn.desc == "(IIII)V") {
                if (method.access and Opcodes.ACC_STATIC == 0) {
                    /*
                     * Non-static glViewport() calls are used for off-screen
                     * framebuffers, so don't need scaling.
                     */
                    continue
                }

                if (transformBounds(method, insn)) {
                    scaledCalls++
                    extraStack = max(extraStack, 4)
                }
            } else if (insn.name == "glScissor" && insn.desc == "(IIII)V") {
                if (transformBounds(method, insn)) {
                    scaledCalls++
                    extraStack = max(extraStack, 4)
                }
            } else if ((insn.name == "glPointSize" || insn.name == "glLineWidth") && insn.desc == "(F)V") {
                transform(method, insn)
                scaledCalls++
                extraStack = max(extraStack, 2)
            }
        }

        for (match in DRAW_PIXELS_MATCHER.match(method)) {
            val aload = match[0] as VarInsnNode
            val invoke = match[11] as MethodInsnNode

            if (invoke.owner !in GL_INTERFACES) {
                continue
            } else if (invoke.name != "glDrawPixels") {
                continue
            } else if (invoke.desc != "(IIIILjava/nio/Buffer;)V") {
                continue
            }

            transformDrawPixelsCall(method, aload, invoke)
            scaledCalls++
            extraStack = max(extraStack, 3)
        }

        for (match in GET_GL_MATCHER.match(method)) {
            val invoke = match[1] as MethodInsnNode
            val putstatic = match[2] as FieldInsnNode

            if (invoke.owner !in GL_CONTEXTS) {
                continue
            } else if (invoke.name != "getGL") {
                continue
            } else if (invoke.desc !in GET_GL_DESCRIPTORS) {
                continue
            }

            addDefaultLineWidthCall(method, invoke, putstatic)
            initBlocks++
            extraStack = max(extraStack, 3)
        }

        if (clazz.name == gameShell && (method.access and Opcodes.ACC_STATIC) == 0) {
            for (match in SET_CANVAS_VISIBLE_MATCHER.match(method)) {
                val getstatic = match[0] as FieldInsnNode
                if (getstatic.desc != "Ljava/awt/Canvas;") {
                    continue
                }

                val invoke = match[2] as MethodInsnNode
                if (invoke.owner != "java/awt/Canvas" || invoke.name != "setVisible" || invoke.desc != "(Z)V") {
                    continue
                }

                addScaleDetector(method, getstatic, invoke)
                initBlocks++
                extraStack = max(extraStack, 2)
            }
        }

        method.maxStack += extraStack
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        if (!isGl) {
            logger.info { "Skipped as the SD client does not require patching" }
        } else if (gameShell == null) {
            logger.info { "Skipped as the game shell could not be identified" }
        } else {
            logger.info { "Added $newMembers members and $initBlocks blocks of initialisation code" }
            logger.info { "Scaled $scaledCalls OpenGL calls" }
        }
    }

    private fun addCanvasScaleField(clazz: ClassNode) {
        clazz.fields.add(FieldNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "canvasScale", "D", null, null))

        var clinit = clazz.methods.find { it.name == "<clinit>" }
        if (clinit == null) {
            clinit = MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
            clinit.instructions = InsnList()
            clinit.instructions.add(InsnNode(Opcodes.RETURN))
        }

        val list = InsnList()
        list.add(InsnNode(Opcodes.DCONST_1))
        list.add(FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, "canvasScale", "D"))
        clinit.instructions.insert(list)
    }

    private fun addPixelZoomMethod(clazz: ClassNode, access: Int) {
        val method = MethodNode(
            Opcodes.ACC_PUBLIC or access,
            "glPixelZoom",
            "(FF)V",
            null,
            null
        )
        method.parameters = mutableListOf(ParameterNode("xfactor", 0), ParameterNode("yfactor", 0))
        clazz.methods.add(method)
    }

    private fun addScaleDetector(method: MethodNode, getstatic: FieldInsnNode, invoke: MethodInsnNode) {
        val graphicsVar = method.maxLocals++
        val endLabel = LabelNode()
        val noGraphics2dLabel = LabelNode()

        val list = InsnList()
        list.add(FieldInsnNode(Opcodes.GETSTATIC, getstatic.owner, getstatic.name, getstatic.desc))
        list.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/awt/Canvas", "getGraphics", "()Ljava/awt/Graphics;"))
        list.add(VarInsnNode(Opcodes.ASTORE, graphicsVar))

        list.add(VarInsnNode(Opcodes.ALOAD, graphicsVar))
        list.add(JumpInsnNode(Opcodes.IFNULL, endLabel))

        list.add(VarInsnNode(Opcodes.ALOAD, graphicsVar))
        list.add(TypeInsnNode(Opcodes.INSTANCEOF, "java/awt/Graphics2D"))
        list.add(JumpInsnNode(Opcodes.IFEQ, noGraphics2dLabel))

        list.add(VarInsnNode(Opcodes.ALOAD, graphicsVar))
        list.add(TypeInsnNode(Opcodes.CHECKCAST, "java/awt/Graphics2D"))
        list.add(
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/awt/Graphics2D",
                "getTransform",
                "()Ljava/awt/geom/AffineTransform;"
            )
        )
        list.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/awt/geom/AffineTransform", "getScaleX", "()D"))
        list.add(FieldInsnNode(Opcodes.PUTSTATIC, gameShell, "canvasScale", "D"))
        list.add(JumpInsnNode(Opcodes.GOTO, endLabel))

        list.add(noGraphics2dLabel)
        list.add(InsnNode(Opcodes.DCONST_1))
        list.add(FieldInsnNode(Opcodes.PUTSTATIC, gameShell, "canvasScale", "D"))

        list.add(endLabel)

        method.instructions.insert(invoke, list)
    }

    private fun addDefaultLineWidthCall(method: MethodNode, invoke: MethodInsnNode, putstatic: FieldInsnNode) {
        val owner = if (invoke.desc.contains('!')) {
            "gl!javax/media/opengl/GL"
        } else {
            "javax/media/opengl/GL"
        }

        val list = InsnList()
        list.add(FieldInsnNode(Opcodes.GETSTATIC, putstatic.owner, putstatic.name, putstatic.desc))
        list.add(FieldInsnNode(Opcodes.GETSTATIC, gameShell, "canvasScale", "D"))
        list.add(InsnNode(Opcodes.D2F))
        list.add(MethodInsnNode(Opcodes.INVOKEINTERFACE, owner, "glLineWidth", "(F)V"))
        method.instructions.insert(putstatic, list)
    }

    private fun transformBounds(method: MethodNode, invoke: MethodInsnNode): Boolean {
        val exprs = mutableListOf<List<AbstractInsnNode>>()
        var head = invoke.previousReal ?: return false

        while (exprs.size < 4) {
            val expr = getExpression(head) ?: return false

            if (invoke.name == "glViewport" && expr.any { it.opcode == Opcodes.IALOAD }) {
                /*
                 * The glViewport() call that uses IALOAD restores viewport
                 * bounds previously saved with glGetIntegerv(), so there's no
                 * need for us to scale it again.
                 */
                return false
            }

            exprs += expr
            head = expr.first().previousReal ?: return false
        }

        exprs.reverse()

        for (expr in exprs) {
            val single = expr.singleOrNull()
            if (single != null && single.opcode == Opcodes.ICONST_0) {
                continue
            }

            val list = InsnList()
            list.add(InsnNode(Opcodes.I2D))
            list.add(FieldInsnNode(Opcodes.GETSTATIC, gameShell, "canvasScale", "D"))
            list.add(InsnNode(Opcodes.DMUL))
            list.add(LdcInsnNode(0.5))
            list.add(InsnNode(Opcodes.DADD))
            list.add(InsnNode(Opcodes.D2I))
            method.instructions.insert(expr.last(), list)
        }

        return true
    }

    private fun transform(method: MethodNode, invoke: MethodInsnNode) {
        val previous = invoke.previousReal!!
        if (previous.opcode == Opcodes.FCONST_1) {
            val list = InsnList()
            list.add(FieldInsnNode(Opcodes.GETSTATIC, gameShell, "canvasScale", "D"))
            list.add(InsnNode(Opcodes.D2F))
            method.instructions.insertBefore(invoke, list)
            method.instructions.remove(previous)
        } else {
            val list = InsnList()
            list.add(InsnNode(Opcodes.F2D))
            list.add(FieldInsnNode(Opcodes.GETSTATIC, gameShell, "canvasScale", "D"))
            list.add(InsnNode(Opcodes.DMUL))
            list.add(InsnNode(Opcodes.D2F))
            method.instructions.insertBefore(invoke, list)
        }
    }

    private fun transformDrawPixelsCall(method: MethodNode, aload: VarInsnNode, invoke: MethodInsnNode) {
        val enableZoom = InsnList()
        enableZoom.add(VarInsnNode(Opcodes.ALOAD, aload.`var`))
        enableZoom.add(FieldInsnNode(Opcodes.GETSTATIC, gameShell, "canvasScale", "D"))
        enableZoom.add(InsnNode(Opcodes.D2F))
        enableZoom.add(FieldInsnNode(Opcodes.GETSTATIC, gameShell, "canvasScale", "D"))
        enableZoom.add(InsnNode(Opcodes.D2F))
        enableZoom.add(MethodInsnNode(Opcodes.INVOKEINTERFACE, invoke.owner, "glPixelZoom", "(FF)V"))

        val disableZoom = InsnList()
        disableZoom.add(VarInsnNode(Opcodes.ALOAD, aload.`var`))
        disableZoom.add(InsnNode(Opcodes.FCONST_1))
        disableZoom.add(InsnNode(Opcodes.FCONST_1))
        disableZoom.add(MethodInsnNode(Opcodes.INVOKEINTERFACE, invoke.owner, "glPixelZoom", "(FF)V"))

        method.instructions.insertBefore(aload, enableZoom)
        method.instructions.insert(invoke, disableZoom)
    }

    private companion object {
        private val logger = InlineLogger()

        private val GL_INTERFACES = setOf("javax/media/opengl/GL", "gl!javax/media/opengl/GL")
        private val GL_IMPLS = setOf("jaggl/opengl", "gl!jaggl/opengl")
        private val GL_CONTEXTS = setOf("javax/media/opengl/GLContext", "gl!javax/media/opengl/GLContext")
        private val GET_GL_DESCRIPTORS = setOf("()Ljavax/media/opengl/GL;", "()Lgl!javax/media/opengl/GL;")

        private val DRAW_PIXELS_MATCHER = InsnMatcher.compile(
            """
            ALOAD ILOAD ILOAD LDC GETSTATIC IFEQ LDC GOTO SIPUSH ALOAD INVOKESTATIC INVOKEINTERFACE
            """.trimIndent()
        )
        private val GET_GL_MATCHER = InsnMatcher.compile("GETSTATIC INVOKEVIRTUAL PUTSTATIC")
        private val SET_CANVAS_VISIBLE_MATCHER = InsnMatcher.compile("GETSTATIC ICONST_1 INVOKEVIRTUAL")
    }
}
