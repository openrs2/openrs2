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
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedType
import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.deob.ast.gl.GlCommand
import dev.openrs2.deob.ast.gl.GlEnum
import dev.openrs2.deob.ast.gl.GlGroup
import dev.openrs2.deob.ast.gl.GlParameter
import dev.openrs2.deob.ast.gl.GlRegistry
import dev.openrs2.deob.ast.util.checkedAsInt
import dev.openrs2.deob.ast.util.walk

class GlConstantTransformer : Transformer() {
    private val enums = mutableSetOf<GlEnum>()

    override fun preTransform(units: Map<String, CompilationUnit>) {
        enums.clear()
    }

    override fun transformUnit(
        units: Map<String, CompilationUnit>,
        unit: CompilationUnit
    ) {
        if (!units.containsKey(GL_CLASS)) {
            return
        }

        val primaryType = unit.primaryType.orElse(null)
        if (primaryType.fullyQualifiedName.orElse(null) in GL_CLASSES) {
            transformParameterNames(primaryType)
        } else {
            transformLiteralArguments(unit)
        }
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
            isArray && asArrayType().componentType.isPrimitive -> true
            isReferenceType && asReferenceType().qualifiedName == "java.lang.Object" -> true
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

        val command = REGISTRY.commands[name] ?: error("Failed to find $name in the OpenGL registry")

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

    private fun transformLiteralArguments(unit: CompilationUnit) {
        unit.walk { expr: MethodCallExpr ->
            if (!expr.nameAsString.startsWith(GL_METHOD_PREFIX)) {
                return@walk
            }

            expr.scope.ifPresent { scope ->
                val type = scope.calculateResolvedType()
                if (!type.isReferenceType) {
                    return@ifPresent
                }

                val name = type.asReferenceType().qualifiedName
                if (name in GL_CLASSES) {
                    transformLiteralArguments(unit, expr)
                }
            }
        }
    }

    override fun postTransform(units: Map<String, CompilationUnit>) {
        val glUnit = units[GL_CLASS] ?: return
        val glInterface = glUnit.primaryType.get()

        // add missing fields
        val fields = enums.filter { !glInterface.getFieldByName(it.name).isPresent }
            .map { it.toDeclaration() }
        glInterface.members.addAll(fields)

        // sort fields by value for consistency
        glInterface.members.sortWith(FIELD_METHOD_COMPARATOR.thenComparing(GL_FIELD_VALUE_COMPARATOR))
    }

    private fun transformLiteralArguments(unit: CompilationUnit, expr: MethodCallExpr) {
        val name = expr.nameAsString
        val command = REGISTRY.commands[name] ?: error("Failed to find $name in the OpenGL registry")

        var registryIndex = 0
        var followedByOffset = false
        for (argument in expr.arguments) {
            val type = argument.calculateResolvedType()

            if (followedByOffset && type.isOffset()) {
                continue
            }

            if (type.isPrimitive) {
                transformLiteralArgument(unit, command, command.parameters[registryIndex], argument)
            }

            registryIndex++
            followedByOffset = type.isFollowedByOffset()
        }

        if (registryIndex != command.parameters.size) {
            error("Command parameters inconsistent with registry")
        }
    }

    private val GlCommand.vendor: String?
        get() = VENDORS.firstOrNull { name.endsWith(it) }

    private fun GlGroup.firstEnumOrNull(value: Int, vendor: String?): GlEnum? {
        if (vendor != null) {
            val enum = enums.filter { it.name.endsWith("_$vendor") }.firstOrNull { it.value == value.toLong() }
            if (enum != null) {
                return enum
            }
        }

        return enums.firstOrNull { it.value == value.toLong() }
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

    private fun Int.toHexLiteralExpr(): IntegerLiteralExpr {
        return IntegerLiteralExpr("0x${Integer.toUnsignedString(this, 16)}")
    }

    private fun transformLiteralArgument(
        unit: CompilationUnit,
        command: GlCommand,
        parameter: GlParameter,
        argument: Expression
    ) {
        if (!argument.isIntegerLiteralExpr) {
            return
        }

        var value = argument.asIntegerLiteralExpr().checkedAsInt()
        val vendor = command.vendor
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

                val enum = group.firstEnumOrNull(bit, vendor)
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

            val expr = bitfieldEnums.sortedBy(GlEnum::value)
                .map { it.toExpr() }
                .reduce { a, b -> BinaryExpr(a, b, BinaryExpr.Operator.BINARY_OR) }

            if (value != 0) {
                logger.warn { "Missing some enums in ${command.name}'s ${parameter.name} bitfield: $value" }

                argument.replace(BinaryExpr(expr, value.toHexLiteralExpr(), BinaryExpr.Operator.BINARY_OR))
            } else {
                argument.replace(expr)
            }
        } else {
            val enum = group.firstEnumOrNull(value, vendor)
            if (enum != null) {
                unit.addImport(GL_CLASS)
                enums += enum

                argument.replace(enum.toExpr())
            } else {
                logger.warn { "Missing enum for ${command.name}'s ${parameter.name} parameter: $value" }
            }
        }
    }

    companion object {
        private val logger = InlineLogger()
        private const val GL_METHOD_PREFIX = "gl"
        private const val GL_CLASS_UNQUALIFIED = "GL"
        private const val GL_CLASS = "javax.media.opengl.$GL_CLASS_UNQUALIFIED"
        private const val JAGGL_CLASS = "jaggl.opengl"
        private val GL_CLASSES = setOf(GL_CLASS, JAGGL_CLASS)
        private val REGISTRY = GlRegistry.parse()
        private val VENDORS = setOf("ARB", "EXT")

        private val FIELD_METHOD_COMPARATOR = Comparator<BodyDeclaration<*>> { a, b ->
            when {
                a.isFieldDeclaration && !b.isFieldDeclaration -> -1
                !a.isFieldDeclaration && b.isFieldDeclaration -> 1
                else -> 0
            }
        }

        private fun BodyDeclaration<*>.getIntValue(): Int? {
            if (!isFieldDeclaration) {
                return null
            }

            val variable = asFieldDeclaration().variables.firstOrNull() ?: return null
            return variable.initializer.map {
                if (it.isIntegerLiteralExpr) {
                    it.asIntegerLiteralExpr().checkedAsInt()
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
