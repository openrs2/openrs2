package org.openrs2.archive.web

import org.thymeleaf.context.IExpressionContext
import org.thymeleaf.expression.IExpressionObjectFactory

public object ByteUnitsExpressionFactory : IExpressionObjectFactory {
    private const val NAME = "byteunits"
    private val ALL_NAMES = setOf(NAME)

    override fun getAllExpressionObjectNames(): Set<String> {
        return ALL_NAMES
    }

    override fun buildObject(context: IExpressionContext, expressionObjectName: String): Any? {
        return if (expressionObjectName == NAME) {
            ByteUnits
        } else {
            null
        }
    }

    override fun isCacheable(expressionObjectName: String): Boolean {
        return expressionObjectName == NAME
    }
}
