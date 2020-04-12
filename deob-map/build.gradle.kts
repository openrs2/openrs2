plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(project(":asm"))
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator Map")
            description.set(
                """
                Data structures for representing a map of obfuscated to
                refactored names.
            """.trimIndent()
            )
        }
    }
}
