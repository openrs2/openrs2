plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(project(":util"))
    api(project(":yaml"))
    api("com.google.inject:guice:${Versions.guice}")
    api("org.ow2.asm:asm:${Versions.asm}")
    api("org.ow2.asm:asm-commons:${Versions.asm}")
    api("org.ow2.asm:asm-tree:${Versions.asm}")
    api("org.ow2.asm:asm-util:${Versions.asm}")

    implementation(project(":compress"))
    implementation(project(":crypto"))
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 ASM Utilities")
            description.set(
                """
                Common utility code used for manipulating Java bytecode with
                the ASM library.
            """.trimIndent()
            )
        }
    }
}
