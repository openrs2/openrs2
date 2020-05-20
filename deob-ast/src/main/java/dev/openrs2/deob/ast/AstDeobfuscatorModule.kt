package dev.openrs2.deob.ast

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import dev.openrs2.deob.ast.gl.GlRegistry
import dev.openrs2.deob.ast.gl.GlRegistryProvider
import dev.openrs2.deob.ast.transform.AddSubTransformer
import dev.openrs2.deob.ast.transform.BinaryExprOrderTransformer
import dev.openrs2.deob.ast.transform.BitMaskTransformer
import dev.openrs2.deob.ast.transform.ComplementTransformer
import dev.openrs2.deob.ast.transform.EncloseTransformer
import dev.openrs2.deob.ast.transform.ForLoopConditionTransformer
import dev.openrs2.deob.ast.transform.GlTransformer
import dev.openrs2.deob.ast.transform.IdentityTransformer
import dev.openrs2.deob.ast.transform.IfElseTransformer
import dev.openrs2.deob.ast.transform.IncrementTransformer
import dev.openrs2.deob.ast.transform.NegativeLiteralTransformer
import dev.openrs2.deob.ast.transform.NewInstanceTransformer
import dev.openrs2.deob.ast.transform.TernaryTransformer
import dev.openrs2.deob.ast.transform.Transformer
import dev.openrs2.deob.ast.transform.UnencloseTransformer
import dev.openrs2.deob.ast.transform.ValueOfTransformer

object AstDeobfuscatorModule : AbstractModule() {
    override fun configure() {
        bind(GlRegistry::class.java)
            .toProvider(GlRegistryProvider::class.java)
            .`in`(Scopes.SINGLETON)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java)
        binder.addBinding().to(UnencloseTransformer::class.java)
        binder.addBinding().to(NegativeLiteralTransformer::class.java)
        binder.addBinding().to(ComplementTransformer::class.java)
        binder.addBinding().to(IfElseTransformer::class.java)
        binder.addBinding().to(TernaryTransformer::class.java)
        binder.addBinding().to(BinaryExprOrderTransformer::class.java)
        binder.addBinding().to(AddSubTransformer::class.java)
        binder.addBinding().to(IdentityTransformer::class.java)
        binder.addBinding().to(BitMaskTransformer::class.java)
        binder.addBinding().to(ValueOfTransformer::class.java)
        binder.addBinding().to(NewInstanceTransformer::class.java)
        binder.addBinding().to(IncrementTransformer::class.java)
        binder.addBinding().to(ForLoopConditionTransformer::class.java)
        binder.addBinding().to(GlTransformer::class.java)
        binder.addBinding().to(EncloseTransformer::class.java)
    }
}
