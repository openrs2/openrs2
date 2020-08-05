package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.resolution.types.ResolvedArrayType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.gl.GlCommand
import dev.openrs2.deob.ast.gl.GlEnum
import dev.openrs2.deob.ast.gl.GlParameter
import dev.openrs2.deob.ast.gl.GlRegistry
import dev.openrs2.deob.ast.util.checkedAsInt
import dev.openrs2.deob.ast.util.toHexLiteralExpr
import dev.openrs2.deob.ast.util.walk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlTransformer @Inject constructor(private val registry: GlRegistry) : Transformer() {
    private val enums = mutableSetOf<GlEnum>()
    private var glUnit: CompilationUnit? = null

    override fun preTransform(group: LibraryGroup) {
        enums.clear()
        glUnit = group["gl"]?.get(GL_CLASS)
    }

    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        if (glUnit == null) {
            return
        }

        val primaryType = unit.primaryType.orElse(null)
        if (primaryType.fullyQualifiedName.orElse(null) in GL_CLASSES) {
            transformParameterNames(primaryType)
        } else {
            transformArguments(unit)
        }

        transformFramebufferStatus(unit)
    }

    private fun transformFramebufferStatus(unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            if (expr.operator == BinaryExpr.Operator.EQUALS || expr.operator == BinaryExpr.Operator.NOT_EQUALS) {
                transformFramebufferStatus(unit, expr.left)
                transformFramebufferStatus(unit, expr.right)
            }
        }
    }

    private fun transformFramebufferStatus(unit: CompilationUnit, expr: Expression) {
        if (expr !is IntegerLiteralExpr) {
            return
        }

        val value = expr.checkedAsInt()
        if (value.toLong() != GL_FRAMEBUFFER_COMPLETE.value) {
            return
        }

        unit.addImport(GL_CLASS)
        enums += GL_FRAMEBUFFER_COMPLETE

        expr.replace(GL_FRAMEBUFFER_COMPLETE.toExpr())
    }

    private fun transformParameterNames(type: TypeDeclaration<*>) {
        for (method in type.methods) {
            if (!method.nameAsString.startsWith(GL_METHOD_PREFIX)) {
                continue
            }

            transformParameterNames(method)
        }
    }

    private fun ResolvedType.isFollowedByOffset(): Boolean {
        return when {
            this is ResolvedArrayType && componentType.isPrimitive -> true
            this is ResolvedReferenceType && qualifiedName == "java.lang.Object" -> true
            else -> false
        }
    }

    private fun ResolvedType.isOffset(): Boolean {
        return this == ResolvedPrimitiveType.INT
    }

    private fun transformParameterNames(method: MethodDeclaration) {
        var name = method.nameAsString

        val last = name.lastOrNull()
        if (method.isNative && last != null && last.isDigit()) {
            name = name.dropLast(1)
        }

        val command = registry.commands[name] ?: error("Failed to find $name in the OpenGL registry")

        var registryIndex = 0
        var followedByOffset = false
        for (parameter in method.parameters) {
            val type = parameter.type.resolve()

            if (followedByOffset && type.isOffset()) {
                transformParameterName(method, command.parameters[registryIndex - 1], parameter, offset = true)
            } else {
                transformParameterName(method, command.parameters[registryIndex], parameter, offset = false)
                registryIndex++
            }

            followedByOffset = type.isFollowedByOffset()
        }

        if (registryIndex != command.parameters.size) {
            error("Command parameters inconsistent with registry")
        }
    }

    private fun transformParameterName(
        method: MethodDeclaration,
        glParameter: GlParameter,
        parameter: Parameter,
        offset: Boolean
    ) {
        val oldName = parameter.nameAsString

        var newName = glParameter.name
        if (offset) {
            newName += "Offset"
        }

        val newSimpleName = SimpleName(newName)

        parameter.name = newSimpleName

        method.walk { expr: NameExpr ->
            if (expr.nameAsString == oldName) {
                expr.name = newSimpleName
            }
        }
    }

    private fun transformArguments(unit: CompilationUnit) {
        unit.walk { expr: MethodCallExpr ->
            if (!expr.nameAsString.startsWith(GL_METHOD_PREFIX)) {
                return@walk
            }

            expr.scope.ifPresent { scope ->
                val type = scope.calculateResolvedType()
                if (type !is ResolvedReferenceType) {
                    return@ifPresent
                }

                val name = type.qualifiedName
                if (name in GL_CLASSES) {
                    transformArguments(unit, expr)
                }
            }
        }
    }

    override fun postTransform(group: LibraryGroup) {
        if (glUnit == null) {
            return
        }

        val glInterface = glUnit!!.primaryType.get()

        // add missing fields
        val fields = enums.filter { glInterface.getFieldByName(it.name).isEmpty }
            .map { it.toDeclaration() }
        glInterface.members.addAll(fields)

        // sort fields by value for consistency
        glInterface.members.sortWith(FIELD_METHOD_COMPARATOR.thenComparing(GL_FIELD_VALUE_COMPARATOR))
    }

    private fun transformArguments(unit: CompilationUnit, expr: MethodCallExpr) {
        val name = expr.nameAsString
        val command = registry.commands[name] ?: error("Failed to find $name in the OpenGL registry")

        var registryIndex = 0
        var followedByOffset = false
        for (argument in expr.arguments) {
            val type = argument.calculateResolvedType()

            if (followedByOffset && type.isOffset()) {
                continue
            }

            if (type.isPrimitive) {
                transformExpr(unit, command, command.parameters[registryIndex], argument)
            }

            registryIndex++
            followedByOffset = type.isFollowedByOffset()
        }

        if (registryIndex != command.parameters.size) {
            error("Command parameters inconsistent with registry")
        }
    }

    private fun GlEnum.toExpr(): Expression {
        return FieldAccessExpr(NameExpr(GL_CLASS_UNQUALIFIED), name)
    }

    private fun GlEnum.toDeclaration(): BodyDeclaration<*> {
        return FieldDeclaration(
            NodeList(),
            VariableDeclarator(PrimitiveType.intType(), name, value.toInt().toHexLiteralExpr())
        )
    }

    private fun transformExpr(
        unit: CompilationUnit,
        command: GlCommand,
        parameter: GlParameter,
        expr: Expression
    ) {
        when (expr) {
            is BinaryExpr -> {
                transformExpr(unit, command, parameter, expr.left)
                transformExpr(unit, command, parameter, expr.right)
            }
            is ConditionalExpr -> {
                transformExpr(unit, command, parameter, expr.thenExpr)
                transformExpr(unit, command, parameter, expr.elseExpr)
            }
            is IntegerLiteralExpr -> {
                transformIntegerLiteralExpr(unit, command, parameter, expr)
            }
        }
    }

    private fun transformIntegerLiteralExpr(
        unit: CompilationUnit,
        command: GlCommand,
        parameter: GlParameter,
        expr: IntegerLiteralExpr
    ) {
        var value = expr.checkedAsInt()
        val group = parameter.group ?: return

        if (parameter.bitfield) {
            if (value == 0) {
                return
            }

            val bitfieldEnums = mutableListOf<GlEnum>()

            for (i in 0..31) {
                val bit = 1 shl i
                if (value and bit == 0) {
                    continue
                }

                val enum = group.enums.firstOrNull { it.value == bit.toLong() }
                if (enum != null) {
                    bitfieldEnums += enum
                    value = value and bit.inv()
                }
            }

            if (bitfieldEnums.isEmpty()) {
                logger.warn { "Missing all enums in ${command.name}'s ${parameter.name} bitfield: $value" }
                return
            }

            unit.addImport(GL_CLASS)
            enums += bitfieldEnums

            val orExpr = bitfieldEnums.sortedBy(GlEnum::value)
                .map { it.toExpr() }
                .reduce { a, b -> BinaryExpr(a, b, BinaryExpr.Operator.BINARY_OR) }

            if (value != 0) {
                logger.warn { "Missing some enums in ${command.name}'s ${parameter.name} bitfield: $value" }

                expr.replace(BinaryExpr(orExpr, value.toHexLiteralExpr(), BinaryExpr.Operator.BINARY_OR))
            } else {
                expr.replace(orExpr)
            }
        } else {
            val enum = group.enums.firstOrNull { it.value == value.toLong() }
            if (enum != null) {
                unit.addImport(GL_CLASS)
                enums += enum

                expr.replace(enum.toExpr())
            } else {
                logger.warn { "Missing enum for ${command.name}'s ${parameter.name} parameter: $value" }
            }
        }
    }

    private companion object {
        private val logger = InlineLogger()
        private const val GL_METHOD_PREFIX = "gl"
        private const val GL_CLASS_UNQUALIFIED = "GL"
        private const val GL_CLASS = "javax.media.opengl.$GL_CLASS_UNQUALIFIED"
        private const val JAGGL_CLASS = "jaggl.opengl"
        private val GL_CLASSES = setOf(GL_CLASS, JAGGL_CLASS)
        private val GL_FRAMEBUFFER_COMPLETE = GlEnum("GL_FRAMEBUFFER_COMPLETE", 0x8CD5)

        private val FIELD_METHOD_COMPARATOR = Comparator<BodyDeclaration<*>> { a, b ->
            when {
                a is FieldDeclaration && b !is FieldDeclaration -> -1
                a !is FieldDeclaration && b is FieldDeclaration -> 1
                else -> 0
            }
        }

        private fun BodyDeclaration<*>.getIntValue(): Int? {
            if (this !is FieldDeclaration) {
                return null
            }

            val variable = variables.firstOrNull() ?: return null
            return variable.initializer.map {
                if (it is IntegerLiteralExpr) {
                    it.checkedAsInt()
                } else {
                    null
                }
            }.orElse(null)
        }

        private val GL_FIELD_VALUE_COMPARATOR = Comparator<BodyDeclaration<*>> { a, b ->
            val aValue = a.getIntValue()
            val bValue = b.getIntValue()
            when {
                aValue != null && bValue != null -> aValue - bValue
                aValue != null && bValue == null -> -1
                aValue == null && bValue != null -> 1
                else -> 0
            }
        }
    }
}
