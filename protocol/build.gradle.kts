plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(projects.crypto)
    api(libs.bundles.guice)
    api(libs.netty.codec.core)

    implementation(projects.buffer)
    implementation(projects.util)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Protocol")
            description.set(
                """
                An implementation of the RuneScape protocol.
            """.trimIndent()
            )
        }
    }
}
