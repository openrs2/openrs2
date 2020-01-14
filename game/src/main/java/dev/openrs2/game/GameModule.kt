package dev.openrs2.game

import com.google.inject.AbstractModule
import dev.openrs2.common.CommonModule

class GameModule : AbstractModule() {
    override fun configure() {
        install(CommonModule())
    }
}
