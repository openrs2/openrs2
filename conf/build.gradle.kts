plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.guice)

    implementation(project(":yaml"))
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
