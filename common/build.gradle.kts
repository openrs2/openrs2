plugins {
    `maven-publish`
    kotlin("jvm")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Common")
            description.set(
                """
                Common code used by all modules.
            """.trimIndent()
            )
        }
    }
}
