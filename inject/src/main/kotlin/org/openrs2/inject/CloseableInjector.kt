package org.openrs2.inject

import com.google.inject.Injector
import com.google.inject.Scopes

public class CloseableInjector(
    private val injector: Injector
) : Injector by injector, AutoCloseable {
    override fun close() {
        for (binding in allBindings.values) {
            if (!Scopes.isSingleton(binding)) {
                continue
            }

            val o = binding.provider.get()
            if (o !is AutoCloseable) {
                continue
            }

            o.close()
        }
    }
}
