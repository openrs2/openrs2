plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("com.google.inject:guice:${Versions.guice}")

    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 HTTP")
            description.set(
                """
                Guava module for creating a HTTP client with .netrc and
                redirection support. The I/O dispatcher is used to run
                asynchronous requests, as code using coroutines will likely use
                the I/O dispatcher to read the response body.
            """.trimIndent()
            )
        }
    }
}
