plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.buffer.generator.GenerateBufferCommandKt")
}

dependencies {
    api(libs.clikt)

    implementation(projects.log)
    implementation(projects.util)
    implementation(libs.kotlinPoet)
    implementation(libs.netty.buffer)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Buffer Generator")
            description.set(
                """
                Tool for generating Jagex-specific ByteBuf extension methods
                automatically.
            """.trimIndent()
            )
        }
    }
}
