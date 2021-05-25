package org.openrs2.asm.io

import io.netty.buffer.ByteBufAllocator
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.packclass.ConstantPool
import org.openrs2.asm.packclass.PackClass
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Pack
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class PackClassLibraryReader @Inject constructor(
    private val alloc: ByteBufAllocator
) : LibraryReader {
    override fun read(input: InputStream): Iterable<ClassNode> {
        Js5Pack.read(input, alloc).use { pack ->
            // read constant pool
            val constantPool = pack.read(PackClass.CONSTANT_POOL_GROUP, PackClass.CONSTANT_POOL_FILE).use { buf ->
                ConstantPool.read(buf)
            }

            // read classes
            val classes = mutableListOf<ClassNode>()

            for (entry in pack.list(PackClass.CLASS_GROUP)) {
                pack.read(PackClass.CLASS_GROUP, entry.id).use { buf ->
                    classes += PackClass.read(buf, constantPool)
                }
            }

            return classes
        }
    }
}
