plugins {
    `maven-publish`
    kotlin("jvm")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 bzip2")
            description.set(
                """
                Provides a bzip2 encoder that produces output identical to the
                reference implementation.
            """.trimIndent()
            )
        }
    }
}
