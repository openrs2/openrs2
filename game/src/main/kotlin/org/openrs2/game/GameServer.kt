package org.openrs2.game

import com.google.common.util.concurrent.Service
import com.google.common.util.concurrent.ServiceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class GameServer @Inject constructor(
    services: Set<Service>
) {
    private val serviceManager = ServiceManager(services)

    public fun run() {
        serviceManager.startAsync().awaitHealthy()
        serviceManager.awaitStopped()
    }
}
