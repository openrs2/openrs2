package dev.openrs2.deob.ast.gl

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSetMultimap
import org.jdom2.input.SAXBuilder
import java.io.InputStream

public data class GlEnum(val name: String, val value: Long)

public data class GlGroup(val name: String, val enums: List<GlEnum>)

public data class GlParameter(val name: String, val bitfield: Boolean, val group: GlGroup?)

public data class GlCommand(val name: String, val parameters: List<GlParameter>)

public data class GlRegistry(val enums: ImmutableSetMultimap<Long, GlEnum>, val commands: Map<String, GlCommand>) {
    public companion object {
        private fun parseValue(s: String): Long {
            return if (s.startsWith("0x")) {
                java.lang.Long.parseUnsignedLong(s.substring(2), 16)
            } else {
                s.toLong()
            }
        }

        public fun parse(input: InputStream): GlRegistry {
            val root = SAXBuilder().build(input).rootElement

            // create enums and groups
            val enumsBuilder = ImmutableSetMultimap.builder<Long, GlEnum>()
            val groupsBuilder = HashMultimap.create<String, GlEnum>()

            for (parent in root.getChildren("enums")) {
                val parentGroups = (parent.getAttributeValue("group") ?: "").split(",")

                for (element in parent.getChildren("enum")) {
                    val name = element.getAttributeValue("name")
                    val value = parseValue(element.getAttributeValue("value"))

                    val enum = GlEnum(name, value)
                    enumsBuilder.put(value, enum)

                    val groups = (element.getAttributeValue("group") ?: "").split(",")
                    for (group in parentGroups union groups) {
                        groupsBuilder.put(group, enum)
                    }
                }
            }

            val groups = groupsBuilder.asMap().mapValues { entry ->
                // sort by name length ascending so names with vendor suffixes come last
                GlGroup(entry.key, entry.value.sortedBy { enum -> enum.name.length })
            }

            // create parameters and commands
            val commands = mutableMapOf<String, GlCommand>()

            for (element in root.getChild("commands").getChildren("command")) {
                val commandName = element.getChild("proto").getChildText("name")

                val parameters = mutableListOf<GlParameter>()
                for (paramElement in element.getChildren("param")) {
                    val paramName = paramElement.getChildText("name")

                    val type = paramElement.getChildText("ptype")
                    val bitfield = type == "GLbitfield"

                    val groupName = paramElement.getAttributeValue("group")
                    val group = if (groupName != null) {
                        groups[groupName]
                    } else {
                        null
                    }

                    parameters += GlParameter(paramName, bitfield, group)
                }

                commands[commandName] = GlCommand(commandName, parameters)
            }

            return GlRegistry(enumsBuilder.build(), commands)
        }
    }
}
