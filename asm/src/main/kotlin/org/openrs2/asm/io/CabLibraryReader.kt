package org.openrs2.asm.io

import dorkbox.cabParser.CabParser
import dorkbox.cabParser.CabStreamSaver
import dorkbox.cabParser.structure.CabFileEntry
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.JsrInliner
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

public object CabLibraryReader : LibraryReader {
    private const val CLASS_SUFFIX = ".class"

    override fun read(input: InputStream): Iterable<ClassNode> {
        val classes = mutableListOf<ClassNode>()

        ByteArrayOutputStream().use { tempOutput ->
            CabParser(input, object : CabStreamSaver {
                override fun closeOutputStream(outputStream: OutputStream, entry: CabFileEntry) {
                    if (entry.name.endsWith(CLASS_SUFFIX)) {
                        val clazz = ClassNode()
                        val reader = ClassReader(tempOutput.toByteArray())
                        reader.accept(JsrInliner(clazz), ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

                        classes += clazz
                    }

                    tempOutput.reset()
                }

                override fun openOutputStream(entry: CabFileEntry): OutputStream {
                    return tempOutput
                }

                override fun saveReservedAreaData(data: ByteArray?, dataLength: Int): Boolean {
                    return false
                }
            }).extractStream()
        }

        return classes
    }
}
