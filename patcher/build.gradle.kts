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
    api(libs.clikt)
    api(libs.guice)

    implementation(project(":conf"))
    implementation(project(":crypto"))
    implementation(project(":inject"))
    implementation(libs.openrs2.natives)
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
