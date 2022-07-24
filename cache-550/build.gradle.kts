plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(projects.cache)
    api(libs.bundles.guice)

    implementation(projects.buffer)
    implementation(projects.util)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Cache (Build 550)")
            description.set(
                """
                A library for encoding and decoding files contained within the
                RuneScape cache. It is only compatible with build 550.
            """.trimIndent()
            )
        }
    }
}
