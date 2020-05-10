package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicKeyTransformer @Inject constructor(private val key: RSAPrivateCrtKeyParameters) : Transformer() {
    private var exponents = 0
    private var moduli = 0

    override fun preTransform(classPath: ClassPath) {
        exponents = 0
        moduli = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn !is LdcInsnNode) {
                continue
            }

            when (insn.cst) {
                JAGEX_EXPONENT -> {
                    insn.cst = key.publicExponent.toString()
                    exponents++
                }
                JAGEX_MODULUS -> {
                    insn.cst = key.modulus.toString()
                    moduli++
                }
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $exponents exponents and $moduli moduli" }
    }

    private companion object {
        private val logger = InlineLogger()
        private const val JAGEX_EXPONENT =
            "58778699976184461502525193738213253649000149147835990136706041084440742975821"
        private const val JAGEX_MODULUS = "71629005252297980327618167912305272963293132912323242902378492635012082079" +
            "72894053929065636522363163621000728841182238772712427862772219676577293600221789"
    }
}
