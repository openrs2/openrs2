package org.openrs2.deob.ast

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.openrs2.deob.ast.gl.GlRegistry
import org.openrs2.deob.ast.gl.GlRegistryProvider
import org.openrs2.deob.ast.transform.AddSubTransformer
import org.openrs2.deob.ast.transform.BinaryExprOrderTransformer
import org.openrs2.deob.ast.transform.BitMaskTransformer
import org.openrs2.deob.ast.transform.CharLiteralTransformer
import org.openrs2.deob.ast.transform.ComplementTransformer
import org.openrs2.deob.ast.transform.EncloseTransformer
import org.openrs2.deob.ast.transform.ForLoopConditionTransformer
import org.openrs2.deob.ast.transform.GlTransformer
import org.openrs2.deob.ast.transform.HexLiteralTransformer
import org.openrs2.deob.ast.transform.IdentityTransformer
import org.openrs2.deob.ast.transform.IfElseTransformer
import org.openrs2.deob.ast.transform.IncrementTransformer
import org.openrs2.deob.ast.transform.NegativeLiteralTransformer
import org.openrs2.deob.ast.transform.NewInstanceTransformer
import org.openrs2.deob.ast.transform.NotTransformer
import org.openrs2.deob.ast.transform.RedundantCastTransformer
import org.openrs2.deob.ast.transform.TernaryTransformer
import org.openrs2.deob.ast.transform.Transformer
import org.openrs2.deob.ast.transform.UnencloseTransformer
import org.openrs2.deob.ast.transform.ValueOfTransformer

public object AstDeobfuscatorModule : AbstractModule() {
    override fun configure() {
        bind(GlRegistry::class.java)
            .toProvider(GlRegistryProvider::class.java)
            .`in`(Scopes.SINGLETON)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java)
        binder.addBinding().to(UnencloseTransformer::class.java)
        binder.addBinding().to(NegativeLiteralTransformer::class.java)
        binder.addBinding().to(ComplementTransformer::class.java)
        binder.addBinding().to(NotTransformer::class.java)
        binder.addBinding().to(CharLiteralTransformer::class.java)
        binder.addBinding().to(IfElseTransformer::class.java)
        binder.addBinding().to(TernaryTransformer::class.java)
        binder.addBinding().to(BinaryExprOrderTransformer::class.java)
        binder.addBinding().to(AddSubTransformer::class.java)
        binder.addBinding().to(IdentityTransformer::class.java)
        binder.addBinding().to(BitMaskTransformer::class.java)
        binder.addBinding().to(HexLiteralTransformer::class.java)
        binder.addBinding().to(ValueOfTransformer::class.java)
        binder.addBinding().to(NewInstanceTransformer::class.java)
        binder.addBinding().to(IncrementTransformer::class.java)
        binder.addBinding().to(ForLoopConditionTransformer::class.java)
        binder.addBinding().to(RedundantCastTransformer::class.java)
        binder.addBinding().to(GlTransformer::class.java)
        binder.addBinding().to(EncloseTransformer::class.java)
    }
}
