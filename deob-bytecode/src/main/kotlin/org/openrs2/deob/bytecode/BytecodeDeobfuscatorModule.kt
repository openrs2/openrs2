package org.openrs2.deob.bytecode

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.bytecode.transform.BitShiftTransformer
import org.openrs2.deob.bytecode.transform.BitwiseOpTransformer
import org.openrs2.deob.bytecode.transform.CanvasTransformer
import org.openrs2.deob.bytecode.transform.ClassLiteralTransformer
import org.openrs2.deob.bytecode.transform.ConstantArgTransformer
import org.openrs2.deob.bytecode.transform.CopyPropagationTransformer
import org.openrs2.deob.bytecode.transform.CounterTransformer
import org.openrs2.deob.bytecode.transform.EmptyClassTransformer
import org.openrs2.deob.bytecode.transform.ExceptionObfuscationTransformer
import org.openrs2.deob.bytecode.transform.ExceptionTracingTransformer
import org.openrs2.deob.bytecode.transform.FernflowerExceptionTransformer
import org.openrs2.deob.bytecode.transform.FieldOrderTransformer
import org.openrs2.deob.bytecode.transform.FinalClassTransformer
import org.openrs2.deob.bytecode.transform.FinalFieldTransformer
import org.openrs2.deob.bytecode.transform.FinalMethodTransformer
import org.openrs2.deob.bytecode.transform.InvokeSpecialTransformer
import org.openrs2.deob.bytecode.transform.MethodOrderTransformer
import org.openrs2.deob.bytecode.transform.MonitorTransformer
import org.openrs2.deob.bytecode.transform.MultipleAssignmentTransformer
import org.openrs2.deob.bytecode.transform.OpaquePredicateTransformer
import org.openrs2.deob.bytecode.transform.OriginalNameTransformer
import org.openrs2.deob.bytecode.transform.OriginalPcRestoreTransformer
import org.openrs2.deob.bytecode.transform.OriginalPcSaveTransformer
import org.openrs2.deob.bytecode.transform.OverrideTransformer
import org.openrs2.deob.bytecode.transform.PatcherTransformer
import org.openrs2.deob.bytecode.transform.RedundantGotoTransformer
import org.openrs2.deob.bytecode.transform.RemapTransformer
import org.openrs2.deob.bytecode.transform.ResetTransformer
import org.openrs2.deob.bytecode.transform.StringDecryptionTransformer
import org.openrs2.deob.bytecode.transform.UnusedArgTransformer
import org.openrs2.deob.bytecode.transform.UnusedLocalTransformer
import org.openrs2.deob.bytecode.transform.UnusedMethodTransformer
import org.openrs2.deob.bytecode.transform.VisibilityTransformer
import org.openrs2.deob.util.DeobfuscatorUtilModule
import org.openrs2.patcher.PatcherModule
import java.nio.file.Files
import java.nio.file.Paths

public object BytecodeDeobfuscatorModule : AbstractModule() {
    override fun configure() {
        install(PatcherModule)
        install(DeobfuscatorUtilModule)

        bind(Profile::class.java)
            .toProvider(ProfileProvider::class.java)
            .`in`(Scopes.SINGLETON)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java, DeobfuscatorQualifier::class.java)
        binder.addBinding().to(BitShiftTransformer::class.java)
        binder.addBinding().to(BitwiseOpTransformer::class.java)
        binder.addBinding().to(CanvasTransformer::class.java)
        binder.addBinding().to(ClassLiteralTransformer::class.java)
        binder.addBinding().to(ConstantArgTransformer::class.java)
        binder.addBinding().to(CopyPropagationTransformer::class.java)
        binder.addBinding().to(CounterTransformer::class.java)
        binder.addBinding().to(EmptyClassTransformer::class.java)
        binder.addBinding().to(ExceptionObfuscationTransformer::class.java)
        binder.addBinding().to(ExceptionTracingTransformer::class.java)
        binder.addBinding().to(FernflowerExceptionTransformer::class.java)
        binder.addBinding().to(FieldOrderTransformer::class.java)
        binder.addBinding().to(FinalClassTransformer::class.java)
        binder.addBinding().to(FinalFieldTransformer::class.java)
        binder.addBinding().to(FinalMethodTransformer::class.java)
        binder.addBinding().to(InvokeSpecialTransformer::class.java)
        binder.addBinding().to(MethodOrderTransformer::class.java)
        binder.addBinding().to(MonitorTransformer::class.java)
        binder.addBinding().to(MultipleAssignmentTransformer::class.java)
        binder.addBinding().to(OpaquePredicateTransformer::class.java)
        binder.addBinding().to(OriginalNameTransformer::class.java)
        binder.addBinding().to(OriginalPcRestoreTransformer::class.java)
        binder.addBinding().to(OriginalPcSaveTransformer::class.java)
        binder.addBinding().to(OverrideTransformer::class.java)
        binder.addBinding().to(PatcherTransformer::class.java)
        binder.addBinding().to(RedundantGotoTransformer::class.java)
        binder.addBinding().to(RemapTransformer::class.java)
        binder.addBinding().to(ResetTransformer::class.java)
        binder.addBinding().to(ResourceTransformer::class.java)
        binder.addBinding().to(UnusedArgTransformer::class.java)
        binder.addBinding().to(UnusedLocalTransformer::class.java)
        binder.addBinding().to(UnusedMethodTransformer::class.java)
        binder.addBinding().to(VisibilityTransformer::class.java)
    }
}
