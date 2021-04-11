plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.deob.DeobfuscateCommandKt")
    applicationDefaultJvmArgs = listOf("-Xmx3G")
}

dependencies {
    api(libs.clikt)

    implementation(projects.decompiler)
    implementation(projects.deobAst)
    implementation(projects.deobBytecode)
    implementation(projects.inject)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator")
            description.set(
                """
                A tool for deobfuscating and decompiling the RuneScape client.
            """.trimIndent()
            )
        }
    }
}
