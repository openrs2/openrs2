plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.deob.bytecode.DeobfuscateBytecodeCommandKt")
}

dependencies {
    api(libs.bundles.guice)
    api(libs.clikt)

    implementation(projects.deobAnnotations)
    implementation(projects.deobUtil)
    implementation(projects.inject)
    implementation(projects.log)
    implementation(projects.patcher)
    implementation(projects.yaml)
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
