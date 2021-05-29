package org.openrs2.game

import com.google.common.util.concurrent.AbstractScheduledService
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
public class GameService : AbstractScheduledService() {
    override fun runOneIteration() {
        // TODO(gpe): implement
    }

    override fun scheduler(): Scheduler {
        return Scheduler.newFixedRateSchedule(0, 600, TimeUnit.MILLISECONDS)
    }
}
