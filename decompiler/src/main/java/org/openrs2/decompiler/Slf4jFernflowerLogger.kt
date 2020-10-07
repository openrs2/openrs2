package org.openrs2.decompiler

import com.github.michaelbull.logging.InlineLogger
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger

public object Slf4jFernflowerLogger : IFernflowerLogger() {
    private val logger = InlineLogger()

    override fun startClass(className: String) {
        logger.info { "Decompiling $className" }
    }

    override fun writeMessage(message: String, severity: Severity) {
        when (severity) {
            Severity.TRACE -> logger.trace { message }
            Severity.INFO -> logger.info { message }
            Severity.WARN -> logger.warn { message }
            Severity.ERROR -> logger.error { message }
            else -> throw IllegalArgumentException()
        }
    }

    override fun writeMessage(message: String, severity: Severity, t: Throwable) {
        when (severity) {
            Severity.TRACE -> logger.trace(t) { message }
            Severity.INFO -> logger.info(t) { message }
            Severity.WARN -> logger.warn(t) { message }
            Severity.ERROR -> logger.error(t) { message }
            else -> throw IllegalArgumentException()
        }
    }
}
