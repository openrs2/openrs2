plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.patcher.PatchCommandKt")
}

dependencies {
    api(project(":asm"))
    api("com.github.ajalt.clikt:clikt:${Versions.clikt}")
    api("com.google.inject:guice:${Versions.guice}")

    implementation(project(":conf"))
    implementation(project(":crypto"))
    implementation(project(":inject"))
    implementation("org.openrs2:openrs2-natives-all:${Versions.openrs2Natives}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Patcher")
            description.set(
                """
                A tool for patching the RuneScape client to allow it to connect
                to an OpenRS2 server and improve compatibility with modern JVMs.
            """.trimIndent()
            )
        }
    }
}
