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
import java.nio.file.Files
import java.nio.file.Paths

public object AstDeobfuscatorModule : AbstractModule() {
    override fun configure() {
        bind(GlRegistry::class.java)
            .toProvider(GlRegistryProvider::class.java)
            .`in`(Scopes.SINGLETON)

        val binder = Multibinder.newSetBinder(binder(), Transformer::class.java)

        if (Files.notExists(Paths.get("share", "deob", "ast.transformers.list"))) {
            println("No AST transformers")
            return
        }

        // would be nice to read this from yaml, unfortunately profile.yaml is not available until the module runs
        val transformers = Files.readAllLines(Paths.get("share", "deob", "ast.transformers.list"))
        for (transformer in transformers) {
            if (transformer.isEmpty()) {
                continue
            }

            binder.addBinding().to(when (transformer) {
                "Unenclose" -> UnencloseTransformer::class.java
                "NegativeLiteral" -> NegativeLiteralTransformer::class.java
                "Complement" -> ComplementTransformer::class.java
                "Not" -> NotTransformer::class.java
                "CharLiteral" -> CharLiteralTransformer::class.java
                "IfElse" -> IfElseTransformer::class.java
                "Ternary" -> TernaryTransformer::class.java
                "BinaryExprOrder" -> BinaryExprOrderTransformer::class.java
                "AddSub" -> AddSubTransformer::class.java
                "Identity" -> IdentityTransformer::class.java
                "BitMask" -> BitMaskTransformer::class.java
                "HexLiteral" -> HexLiteralTransformer::class.java
                "ValueOf" -> ValueOfTransformer::class.java
                "NewInstance" -> NewInstanceTransformer::class.java
                "Increment" -> IncrementTransformer::class.java
                "ForLoopCondition" -> ForLoopConditionTransformer::class.java
                "RedundantCast" -> RedundantCastTransformer::class.java
                "Gl" -> GlTransformer::class.java
                "Enclose" -> EncloseTransformer::class.java
                else -> throw IllegalArgumentException("Unknown AST transformer: $transformer")
            })
        }
    }
}
