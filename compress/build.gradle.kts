plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.xz)

    implementation(project(":util"))
    api(libs.commons.compress)
    api(libs.guava)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Compression")
            description.set(
                """
                Provides headerless implementations of bzip2, gzip and LZMA.
            """.trimIndent()
            )
        }
    }
}
