package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
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

        unit.walk { expr: MethodCallExpr ->
            if (!expr.nameAsString.startsWith("gl")) {
                return@walk
            }

            expr.scope.ifPresent { scope ->
                val type = scope.calculateResolvedType()
                if (!type.isReferenceType) {
                    return@ifPresent
                }

                val name = type.asReferenceType().qualifiedName
                if (name == GL_CLASS) {
                    transformCall(unit, expr)
                }
            }
        }
    }

    override fun postTransform(units: Map<String, CompilationUnit>) {
        val glUnit = units[GL_CLASS] ?: return
        val glInterface = glUnit.primaryType.get()

        // remove existing declarations first to maintain sort order
        for (enum in enums) {
            val declaration = glInterface.getFieldByName(enum.name)
            declaration.ifPresent { it.remove() }
        }

        val fields = enums.sortedBy(GlEnum::value).map { it.toDeclaration() }
        glInterface.members.addAll(0, fields)
    }

    private fun transformCall(unit: CompilationUnit, expr: MethodCallExpr) {
        val name = expr.nameAsString
        val command = REGISTRY.commands[name] ?: error("Failed to find $name in the OpenGL registry")

        var offset = false
        var registryIndex = 0
        for (argument in expr.arguments) {
            val type = argument.calculateResolvedType()

            if (offset) {
                if (type != ResolvedPrimitiveType.INT) {
                    error("Expecting integer offset after primitive array")
                }

                offset = false
                continue
            }

            when {
                type.isArray -> offset = type.asArrayType().componentType.isPrimitive
                type.isPrimitive -> transformArgument(unit, command, command.parameters[registryIndex], argument)
                !type.isReferenceType -> error("Expecting array, reference or primitive type")
            }

            registryIndex++
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

    private fun transformArgument(
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
        private const val GL_CLASS_UNQUALIFIED = "GL"
        private const val GL_CLASS = "javax.media.opengl.$GL_CLASS_UNQUALIFIED"
        private val REGISTRY = GlRegistry.parse()
        private val VENDORS = setOf("ARB", "EXT")
    }
}
