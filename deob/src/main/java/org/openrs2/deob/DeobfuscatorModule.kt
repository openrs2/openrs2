package org.openrs2.deob

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.openrs2.asm.transform.Transformer
import org.openrs2.bundler.BundlerModule
import org.openrs2.bundler.transform.ResourceTransformer
import org.openrs2.deob.transform.BitShiftTransformer
import org.openrs2.deob.transform.BitwiseOpTransformer
import org.openrs2.deob.transform.BundlerTransformer
import org.openrs2.deob.transform.CanvasTransformer
import org.openrs2.deob.transform.ClassLiteralTransformer
import org.openrs2.deob.transform.ConstantArgTransformer
import org.openrs2.deob.transform.CopyPropagationTransformer
import org.openrs2.deob.transform.CounterTransformer
import org.openrs2.deob.transform.EmptyClassTransformer
import org.openrs2.deob.transform.ExceptionTracingTransformer
import org.openrs2.deob.transform.FernflowerExceptionTransformer
import org.openrs2.deob.transform.FieldOrderTransformer
import org.openrs2.deob.transform.FinalFieldTransformer
import org.openrs2.deob.transform.FinalTransformer
import org.openrs2.deob.transform.InvokeSpecialTransformer
import org.openrs2.deob.transform.MethodOrderTransformer
import org.openrs2.deob.transform.MonitorTransformer
import org.openrs2.deob.transform.OpaquePredicateTransformer
import org.openrs2.deob.transform.OriginalNameTransformer
import org.openrs2.deob.transform.OriginalPcRestoreTransformer
import org.openrs2.deob.transform.OriginalPcSaveTransformer
import org.openrs2.deob.transform.OverrideTransformer
import org.openrs2.deob.transform.RedundantGotoTransformer
import org.openrs2.deob.transform.RemapTransformer
import org.openrs2.deob.transform.ResetTransformer
import org.openrs2.deob.transform.UnusedArgTransformer
import org.openrs2.deob.transform.UnusedLocalTransformer
import org.openrs2.deob.transform.UnusedMethodTransformer
import org.openrs2.deob.transform.VisibilityTransformer
import org.openrs2.deob.util.DeobfuscatorUtilModule

public object DeobfuscatorModule : AbstractModule() {
    override fun configure() {
        install(BundlerModule)
        install(DeobfuscatorUtilModule)

        bind(Profile::class.java)
            .toProvider(ProfileProvider::class.java)
            .`in`(Scopes.SINGLETON)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java, DeobfuscatorQualifier::class.java)
        binder.addBinding().to(OriginalPcSaveTransformer::class.java)
        binder.addBinding().to(OriginalNameTransformer::class.java)
        binder.addBinding().to(ClassLiteralTransformer::class.java)
        binder.addBinding().to(InvokeSpecialTransformer::class.java)
        binder.addBinding().to(RemapTransformer::class.java)
        binder.addBinding().to(BundlerTransformer::class.java)
        binder.addBinding().to(ResourceTransformer::class.java)
        binder.addBinding().to(OpaquePredicateTransformer::class.java)
        binder.addBinding().to(ExceptionTracingTransformer::class.java)
        binder.addBinding().to(MonitorTransformer::class.java)
        binder.addBinding().to(BitShiftTransformer::class.java)
        binder.addBinding().to(CanvasTransformer::class.java)
        binder.addBinding().to(FieldOrderTransformer::class.java)
        binder.addBinding().to(BitwiseOpTransformer::class.java)
        binder.addBinding().to(ConstantArgTransformer::class.java)
        binder.addBinding().to(CopyPropagationTransformer::class.java)
        binder.addBinding().to(UnusedLocalTransformer::class.java)
        binder.addBinding().to(UnusedMethodTransformer::class.java)
        binder.addBinding().to(UnusedArgTransformer::class.java)
        binder.addBinding().to(CounterTransformer::class.java)
        binder.addBinding().to(ResetTransformer::class.java)
        binder.addBinding().to(EmptyClassTransformer::class.java)
        binder.addBinding().to(MethodOrderTransformer::class.java)
        binder.addBinding().to(VisibilityTransformer::class.java)
        binder.addBinding().to(FinalTransformer::class.java)
        binder.addBinding().to(FinalFieldTransformer::class.java)
        binder.addBinding().to(OverrideTransformer::class.java)
        binder.addBinding().to(RedundantGotoTransformer::class.java)
        binder.addBinding().to(OriginalPcRestoreTransformer::class.java)
        binder.addBinding().to(FernflowerExceptionTransformer::class.java)
    }
}
