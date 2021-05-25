package org.openrs2.asm.io

import io.netty.buffer.ByteBufAllocator
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.packclass.ConstantPool
import org.openrs2.asm.packclass.PackClass
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Pack
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class PackClassLibraryWriter @Inject constructor(
    private val alloc: ByteBufAllocator
) : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, classes: Iterable<ClassNode>) {
        Js5Pack.create(alloc).use { pack ->
            // create constant pool
            val builder = ConstantPool.Builder()
            for (clazz in classes) {
                builder.add(clazz)
            }
            val constantPool = builder.build()

            // write classes
            for ((i, clazz) in classes.withIndex()) {
                alloc.buffer().use { buf ->
                    PackClass.write(buf, constantPool, clazz)
                    pack.write(PackClass.CLASS_GROUP, i, buf)
                }
            }

            // write constant pool
            alloc.buffer().use { buf ->
                constantPool.write(buf)
                pack.write(PackClass.CONSTANT_POOL_GROUP, PackClass.CONSTANT_POOL_FILE, buf)
            }

            // write pack
            pack.write(output)
        }
    }
}
