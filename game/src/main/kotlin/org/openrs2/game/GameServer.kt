package org.openrs2.game

import com.github.michaelbull.logging.InlineLogger
import com.google.common.util.concurrent.Service
import com.google.common.util.concurrent.ServiceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class GameServer @Inject constructor(
    services: Set<Service>
) {
    private val serviceManager = ServiceManager(services)

    public fun run(start: Long) {
        serviceManager.startAsync().awaitHealthy()

        val elapsed = System.nanoTime() - start
        logger.info { "Started OpenRS2 in ${elapsed / NANOS_PER_MILLI} milliseconds" }

        serviceManager.awaitStopped()
    }

    private companion object {
        private val logger = InlineLogger()
        private const val NANOS_PER_MILLI = 1_000_000
    }
}
