plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.kotlin.coroutines.core)

    implementation(libs.guava)

    testImplementation(libs.h2)
    testImplementation(libs.kotlin.coroutines.test)
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
