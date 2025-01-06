package org.openrs2.deob.util.module

import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.deob.util.profile.Profile
import java.util.EnumMap

public class ModuleProvider @Inject constructor(private val profile: Profile) : Provider<Set<Module>> {

    private val specs = profile.modules.associateByTo(EnumMap(ModuleType::class.java), ModuleSpec::type)

    override fun get(): Set<Module> {
        val modules = EnumMap<ModuleType, Module>(ModuleType::class.java)

        fun createModule(type: ModuleType): Module {
            return modules.getOrPut(type) {
                if (type.synthetic) {
                    Module(type, type.library, ModuleFormat.JAR, profile.directory)
                } else {
                    val (_, source, format, deps) = specs[type] ?: error("Module $type missing from profile")
                    Module(type, source, format, profile.directory, deps.mapTo(hashSetOf(), ::createModule))
                }
            }
        }

        specs.keys.forEach { type -> modules.getOrPut(type) { createModule(type) } }
        return modules.values.toHashSet()
    }
}
