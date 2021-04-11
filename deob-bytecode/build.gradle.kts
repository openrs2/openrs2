plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.deob.bytecode.DeobfuscateBytecodeCommandKt")
}

dependencies {
    api(libs.clikt)
    api(libs.guice)

    implementation(project(":deob-annotations"))
    implementation(project(":deob-util"))
    implementation(project(":inject"))
    implementation(project(":patcher"))
    implementation(project(":yaml"))
    implementation(libs.fastutil)
    implementation(libs.guava)
    implementation(libs.jgrapht)
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
