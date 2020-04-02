plugins {
    `maven-publish`
    kotlin("jvm")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Utilities")
            description.set(
                """
                Common utility code used by all modules.
            """.trimIndent()
            )
        }
    }
}
