plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.deob.DeobfuscateCommandKt"
}

dependencies {
    api("com.github.ajalt:clikt:${Versions.clikt}")

    implementation(project(":bundler"))
    implementation(project(":deob-annotations"))
    implementation(project(":deob-map"))
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("it.unimi.dsi:fastutil:${Versions.fastutil}")
    implementation("org.jgrapht:jgrapht-core:${Versions.jgrapht}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Bytecode Deobfuscator")
            description.set(
                """
                A tool for performing bytecode-level deobfuscation of the
                RuneScape client. It must be run before decompiling the
                client - some of the transformations it performs are required
                for the decompiler to produce valid output.
            """.trimIndent()
            )
        }
    }
}
