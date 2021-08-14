plugins {
    `maven-publish`
    kotlin("jvm")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Logging")
            description.set(
                """
                Provides OpenRS2's logback configuration file.
            """.trimIndent()
            )
        }
    }
}
