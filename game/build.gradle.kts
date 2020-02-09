plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.game.GameServerKt"
}

dependencies {
    implementation(project(":common"))
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Game Server")
            description.set("""
                Reimplementation of the RuneScape game server software.
            """.trimIndent())
        }
    }
}
