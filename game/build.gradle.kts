plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.game.GameCommandKt")
}

dependencies {
    api("com.github.ajalt.clikt:clikt:${Versions.clikt}")

    implementation(project(":inject"))
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Game Server")
            description.set(
                """
                Reimplementation of the RuneScape game server software.
            """.trimIndent()
            )
        }
    }
}
