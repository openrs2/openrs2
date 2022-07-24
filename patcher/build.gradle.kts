plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.patcher.PatchCommandKt")
}

dependencies {
    api(projects.asm)
    api(libs.bundles.guice)
    api(libs.clikt)

    implementation(projects.conf)
    implementation(projects.inject)
    implementation(projects.log)
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
