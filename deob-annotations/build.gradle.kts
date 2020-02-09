plugins {
    `java-library`
    `maven-publish`
}

// XXX(gpe): this module MUST NOT depend on any of the GPLed modules.

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator Annotations")
            description.set("""
                Annotations inserted by the deobfuscator to track the original
                names and descriptors of classes, methods and fields.
            """.trimIndent())
        }
    }
}
