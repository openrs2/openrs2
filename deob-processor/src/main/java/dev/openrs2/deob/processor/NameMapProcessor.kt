package dev.openrs2.deob.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sun.source.util.Trees
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.toInternalClassName
import dev.openrs2.deob.annotation.OriginalArg
import dev.openrs2.deob.annotation.OriginalClass
import dev.openrs2.deob.annotation.OriginalMember
import dev.openrs2.deob.map.Field
import dev.openrs2.deob.map.Method
import dev.openrs2.deob.map.NameMap
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
    "dev.openrs2.deob.annotation.OriginalArg",
    "dev.openrs2.deob.annotation.OriginalClass",
    "dev.openrs2.deob.annotation.OriginalMember",
    "dev.openrs2.deob.annotation.Pc"
)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedOptions(
    "map"
)
class NameMapProcessor : AbstractProcessor() {
    private val classes = TreeMap<String, String>()
    private val fields = TreeMap<MemberRef, Field>()
    private val methods = TreeMap<MemberRef, Method>()
    private lateinit var trees: Trees
    private lateinit var localScanner: LocalVariableScanner

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
            classes[originalClass.value] = element.qualifiedName.toString().toInternalClassName()
        }

        for (element in env.getElementsAnnotatedWith(OriginalMember::class.java)) {
            val path = trees.getPath(element)
            val owner = trees.getScope(path).enclosingClass.qualifiedName.toString().toInternalClassName()
            val name = element.simpleName.toString()

            val originalMember = element.getAnnotation(OriginalMember::class.java)!!
            val ref = MemberRef(originalMember.owner, originalMember.name, originalMember.descriptor)

            when (element) {
                is VariableElement -> fields[ref] = Field(owner, name)
                is ExecutableElement -> {
                    val arguments = element.parameters.map { parameter ->
                        val originalArg = parameter.getAnnotation(OriginalArg::class.java)!!
                        Pair(originalArg.value, parameter.simpleName.toString())
                    }.toMap(LinkedHashMap())

                    val locals = TreeMap<Int, String>()
                    localScanner.scan(path, locals)

                    methods[ref] = Method(owner, name, arguments, locals)
                }
                else -> error("Unexpected element type")
            }
        }

        if (env.processingOver()) {
            Files.newBufferedWriter(mapPath).use { writer ->
                val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
                mapper.writeValue(writer, NameMap(classes, fields, methods))
            }
        }

        return true
    }
}
