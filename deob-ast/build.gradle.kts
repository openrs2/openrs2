plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.deob.ast.DeobfuscateAstCommandKt")
}

dependencies {
    api(projects.deobUtil)
    api(libs.clikt)
    api(libs.guice)

    implementation(projects.inject)
    implementation(projects.log)
    implementation(projects.util)
    implementation(libs.guava)
    implementation(libs.javaParser)
    implementation(libs.jdom)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 AST Deobfuscator")
            description.set(
                """
                A tool for performing AST-level deobfuscation of the RuneScape client. It
                may be run after decompiling the client.
            """.trimIndent()
            )
        }
    }
}
