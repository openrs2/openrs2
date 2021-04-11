plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.decompiler.DecompileCommandKt")
    applicationDefaultJvmArgs = listOf("-Xmx3G")
}

dependencies {
    api(project(":deob-util"))
    api(libs.clikt)
    api(libs.guice)

    implementation(project(":util"))
    implementation(libs.fernflower)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Decompiler")
            description.set(
                """
                A thin wrapper around OpenRS2's fork of Fernflower that sets
                the standard options required to decompile the RuneScape client
                and its dependencies.
            """.trimIndent()
            )
        }
    }
}
