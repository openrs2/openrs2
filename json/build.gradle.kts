plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}")
    api("com.google.inject:guice:${Versions.guice}")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 JSON")
            description.set(
                """
                Guice module for creating a JSON ObjectMapper.
            """.trimIndent()
            )
        }
    }
}
