plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.decompiler.DecompileCommandKt"
    applicationDefaultJvmArgs = listOf("-Xmx3G")
}

dependencies {
    api("com.github.ajalt:clikt:${Versions.clikt}")

    implementation(project(":deob-util"))
    implementation(project(":util"))
    implementation("dev.openrs2:fernflower:${Versions.fernflower}")
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
