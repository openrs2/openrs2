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
    implementation(projects.cache550)
    implementation(projects.conf)
    implementation(projects.inject)
    implementation(projects.log)
    implementation(projects.net)
    implementation(projects.protocol)
    implementation(projects.util)
    implementation(libs.guava)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.netty.codec.http)
    implementation(libs.result.core)
    implementation(libs.result.coroutines)
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
