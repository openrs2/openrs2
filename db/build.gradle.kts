plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")

    implementation("com.google.guava:guava:${Versions.guava}")

    testImplementation("com.h2database:h2:${Versions.h2}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinCoroutines}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Database")
            description.set(
                """
                A thin layer on top of the JDBC API that enforces the use of
                transactions, automatically retrying them on deadlock, and
                provides coroutine integration.
            """.trimIndent()
            )
        }
    }
}
