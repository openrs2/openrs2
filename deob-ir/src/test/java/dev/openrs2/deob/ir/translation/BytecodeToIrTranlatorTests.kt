package dev.openrs2.deob.ir.translation

import dev.openrs2.deob.ir.translation.fixture.Fixture
import dev.openrs2.deob.ir.translation.fixture.FixtureMethod
import org.junit.jupiter.api.Test

class BytecodeToIrTranlatorTests {

    @Test
    fun `Creates entry basic block`() {
        class CfgSample() : Fixture {
            val a = true
            val b = false

            override fun test() {
                if (a) {
                    println("a")
                } else if (b) {
                    println("b")
                }
            }
        }

        val fixture = FixtureMethod.from(CfgSample::class)
        val decompiler = BytecodeToIrTranlator()
        val irMethod = decompiler.decompile(fixture.owner, fixture.method)

        // @TODO - some way of asserting on the output cfg?
    }
}

