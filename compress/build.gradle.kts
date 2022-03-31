plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.commons.compress)
    api(libs.guava)
    api(libs.xz)

    implementation(projects.util)
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
