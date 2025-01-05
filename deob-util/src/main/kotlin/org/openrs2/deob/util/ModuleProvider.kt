package org.openrs2.deob.util

import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.deob.util.profile.Profile
import java.nio.file.Path

public class ModuleProvider @Inject constructor(private val profile: Profile) : Provider<Set<Module>> {

    override fun get(): Set<Module> {
        val jars = profile.directory.resolve(JARS_DIR)
        val sources = profile.directory

        val gl = Module("gl", jars, sources)
        val signlink = Module("signlink", jars, sources)
        val unpack = Module("unpack", jars, sources)
        val client = Module("client", jars, sources, setOf(gl, signlink))
        val loader = Module("loader", jars, sources, setOf(signlink, unpack))
        val unpackclass = Module("unpackclass", jars, sources, setOf(unpack))

        return setOf(client, gl, loader, signlink, unpack, unpackclass)
    }

    private companion object {
        private val JARS_DIR = Path.of("var/cache/deob")
    }
}
