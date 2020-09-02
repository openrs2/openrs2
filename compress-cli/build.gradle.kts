plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.compress.cli.CompressCommandKt"
}

dependencies {
    api("com.github.ajalt.clikt:clikt:${Versions.clikt}")

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
