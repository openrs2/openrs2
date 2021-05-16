plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.game.GameCommandKt")
}

dependencies {
    api(libs.clikt)

    implementation(projects.buffer)
    implementation(projects.cache)
    implementation(projects.conf)
    implementation(projects.inject)
    implementation(projects.net)
    implementation(projects.protocol)
    implementation(projects.util)
    implementation(libs.guava)
    implementation(libs.netty.codec.http)
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
