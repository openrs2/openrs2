plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.deob.cli.DeobfuscatorCliKt"
}

dependencies {
    implementation(project(":asm"))
    implementation(project(":deob"))
    implementation(project(":deob-ir"))
    implementation("com.github.ajalt:clikt:${Versions.clikt}")
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("org.jgrapht:jgrapht-io:${Versions.jgrapht}")
    implementation("org.jgrapht:jgrapht-guava:${Versions.jgrapht}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator CLI")
            description.set(
                """
                A command-line interface that provides access to the deobfuscators
                analyis, deobfuscation, and decompilation tools.
            """.trimIndent()
            )
        }
    }
}
