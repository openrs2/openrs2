plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(project(":crypto"))
    api(libs.netty.codec)

    implementation(project(":buffer"))
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
