plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    implementation(project(":util"))
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
                Provides headerless implementations of bzip2 and gzip.
            """.trimIndent()
            )
        }
    }
}
