package org.openrs2.deob.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import com.google.inject.Key
import com.sun.source.util.Trees
import org.openrs2.asm.MemberRef
import org.openrs2.asm.toInternalClassName
import org.openrs2.deob.annotation.OriginalArg
import org.openrs2.deob.annotation.OriginalClass
import org.openrs2.deob.annotation.OriginalMember
import org.openrs2.deob.util.map.Field
import org.openrs2.deob.util.map.Method
import org.openrs2.deob.util.map.NameMap
import org.openrs2.util.io.useAtomicBufferedWriter
import org.openrs2.yaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.TreeMap
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

@SupportedAnnotationTypes(
    "org.openrs2.deob.annotation.OriginalArg",
    "org.openrs2.deob.annotation.OriginalClass",
    "org.openrs2.deob.annotation.OriginalMember",
    "org.openrs2.deob.annotation.Pc"
)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedOptions(
    "map"
)
public class NameMapProcessor : AbstractProcessor() {
    private val map = NameMap()
    private val mapper: ObjectMapper
    private lateinit var trees: Trees
    private lateinit var localScanner: LocalVariableScanner

    init {
        val injector = Guice.createInjector(DeobfuscatorProcessorModule)
        mapper = injector.getInstance(Key.get(ObjectMapper::class.java, Yaml::class.java))
    }

    override fun init(env: ProcessingEnvironment) {
        super.init(env)
        trees = Trees.instance(env)
        localScanner = LocalVariableScanner(trees)
    }

    private fun getPath(): Path? {
        val map = processingEnv.options["map"] ?: return null
        return Paths.get(map)
    }

    override fun process(annotations: Set<TypeElement>, env: RoundEnvironment): Boolean {
        val mapPath = getPath() ?: return true

        for (element in env.getElementsAnnotatedWith(OriginalClass::class.java)) {
            check(element is TypeElement)

            val originalClass = element.getAnnotation(OriginalClass::class.java)!!
            map.classes[originalClass.value] = element.qualifiedName.toString().toInternalClassName()
        }

        for (element in env.getElementsAnnotatedWith(OriginalMember::class.java)) {
            val path = trees.getPath(element)
            val owner = trees.getScope(path).enclosingClass.qualifiedName.toString().toInternalClassName()
            val name = element.simpleName.toString()

            val originalMember = element.getAnnotation(OriginalMember::class.java)!!
            val ref = MemberRef(originalMember.owner, originalMember.name, originalMember.descriptor)

            when (element) {
                is VariableElement -> map.fields[ref] = Field(owner, name)
                is ExecutableElement -> {
                    val arguments = element.parameters.map { parameter ->
                        val originalArg = parameter.getAnnotation(OriginalArg::class.java)!!
                        Pair(originalArg.value, parameter.simpleName.toString())
                    }.toMap(LinkedHashMap())

                    val locals = TreeMap<Int, String>()
                    localScanner.scan(path, locals)

                    map.methods[ref] = Method(owner, name, arguments, locals)
                }
                else -> error("Unexpected element type")
            }
        }

        if (env.processingOver()) {
            Files.createDirectories(mapPath.parent)

            val combinedMap: NameMap
            if (Files.exists(mapPath)) {
                combinedMap = Files.newBufferedReader(mapPath).use { reader ->
                    mapper.readValue(reader, NameMap::class.java)
                }
                combinedMap.add(map)
            } else {
                combinedMap = map
            }

            mapPath.useAtomicBufferedWriter { writer ->
                mapper.writeValue(writer, combinedMap)
            }
        }

        return true
    }
}
