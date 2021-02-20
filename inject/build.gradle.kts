plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("com.google.inject:guice:${Versions.guice}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Inject")
            description.set(
                """
                Guice extension for closing AutoCloseable singletons.
            """.trimIndent()
            )
        }
    }
}
