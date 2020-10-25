plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("io.netty:netty-codec:${Versions.netty}")

    implementation(project(":buffer"))

    testImplementation(project(":buffer"))
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
