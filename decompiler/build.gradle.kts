plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.decompiler.DecompilerKt"
    applicationDefaultJvmArgs = listOf("-Xmx3G")
}

dependencies {
    implementation(project(":common"))
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
