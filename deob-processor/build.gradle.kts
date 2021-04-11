plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    implementation(projects.deobAnnotations)
    implementation(projects.deobUtil)
    implementation(projects.inject)
    implementation(projects.yaml)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Deobfuscator Annotation Processor")
            description.set(
                """
                Processes deobfuscator annotations to create a mapping file of
                obfuscated to refactored names.
            """.trimIndent()
            )
        }
    }
}
