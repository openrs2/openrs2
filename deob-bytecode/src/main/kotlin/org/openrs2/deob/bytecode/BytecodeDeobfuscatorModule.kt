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

        if (Files.notExists(Paths.get("share", "deob", "bytecode.transformers.list"))) {
            println("No bytecode transformers")
            return
        }

        // would be nice to read this from yaml, unfortunately profile.yaml is not available until the module runs
        val transformers = Files.readAllLines(Paths.get("share/deob/bytecode.transformers.list"))
        for (transformer in transformers) {
            if (transformer.isEmpty()) {
                continue
            }

            binder.addBinding().to(when (transformer) {
                "OriginalPcSave" -> OriginalPcSaveTransformer::class.java
                "OriginalName" -> OriginalNameTransformer::class.java
                "StringDecryption" -> StringDecryptionTransformer::class.java
                "ClassLiteral" -> ClassLiteralTransformer::class.java
                "InvokeSpecial" -> InvokeSpecialTransformer::class.java
                "MultipleAssignment" -> MultipleAssignmentTransformer::class.java
                "Remap" -> RemapTransformer::class.java
                "Patcher" -> PatcherTransformer::class.java
                "OpaquePredicate" -> OpaquePredicateTransformer::class.java
                "ExceptionObfuscation" -> ExceptionObfuscationTransformer::class.java
                "ExceptionTracing" -> ExceptionTracingTransformer::class.java
                "Monitor" -> MonitorTransformer::class.java
                "BitShift" -> BitShiftTransformer::class.java
                "Canvas" -> CanvasTransformer::class.java
                "FieldOrder" -> FieldOrderTransformer::class.java
                "BitwiseOp" -> BitwiseOpTransformer::class.java
                "ConstantArg" -> ConstantArgTransformer::class.java
                "CopyPropagation" -> CopyPropagationTransformer::class.java
                "UnusedLocal" -> UnusedLocalTransformer::class.java
                "UnusedMethod" -> UnusedMethodTransformer::class.java
                "UnusedArg" -> UnusedArgTransformer::class.java
                "Counter" -> CounterTransformer::class.java
                "Reset" -> ResetTransformer::class.java
                "EmptyClass" -> EmptyClassTransformer::class.java
                "MethodOrder" -> MethodOrderTransformer::class.java
                "Visibility" -> VisibilityTransformer::class.java
                "FinalClass" -> FinalClassTransformer::class.java
                "FinalMethod" -> FinalMethodTransformer::class.java
                "FinalField" -> FinalFieldTransformer::class.java
                "Override" -> OverrideTransformer::class.java
                "RedundantGoto" -> RedundantGotoTransformer::class.java
                "OriginalPcRestore" -> OriginalPcRestoreTransformer::class.java
                "FernflowerException" -> FernflowerExceptionTransformer::class.java
                else -> throw IllegalArgumentException("Unknown bytecode transformer: $transformer")
            })
        }
    }
}
