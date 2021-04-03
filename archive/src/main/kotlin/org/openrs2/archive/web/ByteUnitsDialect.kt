package org.openrs2.archive.web

import org.thymeleaf.dialect.AbstractDialect
import org.thymeleaf.dialect.IExpressionObjectDialect
import org.thymeleaf.expression.IExpressionObjectFactory

public object ByteUnitsDialect : AbstractDialect("byteunits"), IExpressionObjectDialect {
    override fun getExpressionObjectFactory(): IExpressionObjectFactory {
        return ByteUnitsExpressionFactory
    }
}
