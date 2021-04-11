plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(projects.asm)
    api(projects.yaml)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator Utilities")
            description.set(
                """
                Common utility code used by all deobfuscator modules.
            """.trimIndent()
            )
        }
    }
}
