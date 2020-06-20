package dev.openrs2.deob.remap

import dev.openrs2.asm.classpath.ClassMetadata
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.filter.ClassFilter
import dev.openrs2.deob.map.NameMap
import org.objectweb.asm.Opcodes

class ClassMappingGenerator(
    private val classPath: ClassPath,
    private val excludedClasses: ClassFilter,
    private val nameMap: NameMap
) {
    private val nameGenerator = NameGenerator()
    private val mapping = mutableMapOf<String, String>()

    fun generate(): Map<String, String> {
        for (clazz in classPath.libraryClasses) {
            populateMapping(clazz)
        }

        mapping.replaceAll { k, v ->
            val (library, default) = v.splitAtLibraryBoundary()
            val name = nameMap.mapClassName(k, default)
            return@replaceAll "$library!$name"
        }

        return mapping
    }

    private fun populateMapping(clazz: ClassMetadata): String {
        val name = clazz.name
        if (mapping.containsKey(name) || !isRenamable(clazz)) {
            return mapping.getOrDefault(name, name)
        }

        val mappedName = generateName(clazz)
        mapping[name] = mappedName
        return mappedName
    }

    private fun isRenamable(clazz: ClassMetadata): Boolean {
        if (excludedClasses.matches(clazz.name) || clazz.dependency) {
            return false
        }

        for (method in clazz.methods) {
            if (clazz.getMethodAccess(method)!! and Opcodes.ACC_NATIVE != 0) {
                return false
            }
        }

        return true
    }

    private fun generateName(clazz: ClassMetadata): String {
        val name = clazz.name
        var mappedName = name.getLibraryAndPackageName()

        val superClass = clazz.superClass
        if (superClass != null && superClass.name != "java/lang/Object") {
            var superName = populateMapping(superClass)
            superName = superName.getClassName()
            mappedName += nameGenerator.generate(superName + "_Sub")
        } else if (clazz.`interface`) {
            mappedName += nameGenerator.generate("Interface")
        } else {
            mappedName += nameGenerator.generate("Class")
        }

        return mappedName
    }
}
