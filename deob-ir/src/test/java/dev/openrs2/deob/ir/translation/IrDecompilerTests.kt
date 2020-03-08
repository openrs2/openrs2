package dev.openrs2.deob.ir.translation

import dev.openrs2.deob.ir.translation.fixture.Fixture
import dev.openrs2.deob.ir.translation.fixture.FixtureMethod
import org.junit.jupiter.api.Test

class IrDecompilerTests {

    @Test
    fun `Creates entry basic block`() {
        class CfgSample(val a: Boolean, val b: Boolean) :
            Fixture {
            override fun test() {
                if (a) {
                    println("a")
                } else if (b) {
                    println("b")
                }
            }
        }

        val fixture = FixtureMethod.from(CfgSample::class)
        val decompiler = IrDecompiler(fixture.owner, fixture.method)
        val irMethod = decompiler.decompile()
    }
}

