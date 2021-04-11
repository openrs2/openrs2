plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.guice)
    api(libs.jackson.databind)

    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 YAML")
            description.set(
                """
                Guice module for creating a YAML ObjectMapper.
            """.trimIndent()
            )
        }
    }
}
