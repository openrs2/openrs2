plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}")
    api("com.google.inject:guice:${Versions.guice}")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 YAML")
            description.set(
                """
                Guava module for creating a YAML ObjectMapper.
            """.trimIndent()
            )
        }
    }
}
