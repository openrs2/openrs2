plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.compress.cli.CompressCommandKt")
}

dependencies {
    api(libs.clikt)

    implementation(project(":compress"))
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Compression CLI")
            description.set(
                """
                Tools for compressing and uncompressing headerless bzip2,
                DEFLATE, gzip and LZMA files.
            """.trimIndent()
            )
        }
    }
}
