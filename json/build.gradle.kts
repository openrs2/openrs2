plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.bundles.guice)
    api(libs.jackson.databind)

    implementation(libs.jackson.kotlin)
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
