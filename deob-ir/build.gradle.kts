plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

dependencies {
    implementation(project(":asm"))
    implementation("com.google.guava:guava:${Versions.guava}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator IR")
            description.set(
                """
                An intermediate reprsentation of bytecode.
            """.trimIndent()
            )
        }
    }
}
