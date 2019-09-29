package dev.openrs2.decompiler;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4jFernflowerLogger extends IFernflowerLogger {
	private static final Logger logger = LoggerFactory.getLogger(Slf4jFernflowerLogger.class);

	public static final IFernflowerLogger INSTANCE = new Slf4jFernflowerLogger();

	private Slf4jFernflowerLogger() {
		/* empty */
	}

	@Override
	public void startClass(String className) {
		logger.info("Decompiling {}", className);
	}

	@Override
	public void writeMessage(String message, Severity severity) {
		switch (severity) {
		case TRACE:
			logger.trace(message);
			break;
		case INFO:
			logger.info(message);
			break;
		case WARN:
			logger.warn(message);
			break;
		case ERROR:
			logger.error(message);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void writeMessage(String message, Severity severity, Throwable t) {
		switch (severity) {
		case TRACE:
			logger.trace(message, t);
			break;
		case INFO:
			logger.info(message, t);
			break;
		case WARN:
			logger.warn(message, t);
			break;
		case ERROR:
			logger.error(message, t);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
}
