plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("org.tukaani:xz:${Versions.xz}")

    implementation(project(":util"))
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("org.apache.commons:commons-compress:${Versions.commonsCompress}")
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
