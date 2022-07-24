plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(projects.crypto)
    api(libs.bundles.guice)

    implementation(projects.yaml)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Configuration")
            description.set(
                """
                Provides a parser for OpenRS2's main configuration file.
            """.trimIndent()
            )
        }
    }
}
