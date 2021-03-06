plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.deob.ast.DeobfuscateAstCommandKt")
}

dependencies {
    api(project(":deob-util"))
    api("com.github.ajalt.clikt:clikt:${Versions.clikt}")
    api("com.google.inject:guice:${Versions.guice}")

    implementation(project(":inject"))
    implementation(project(":util"))
    implementation("com.github.javaparser:javaparser-symbol-solver-core:${Versions.javaParser}")
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("com.google.inject:guice:${Versions.guice}")
    implementation("org.jdom:jdom2:${Versions.jdom}")
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
