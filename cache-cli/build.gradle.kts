plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.cache.cli.CacheCommandKt")
}

dependencies {
    api(libs.clikt)

    implementation(projects.cache)
    implementation(projects.inject)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Cache CLI")
            description.set(
                """
                Tools for working with RuneScape caches.
            """.trimIndent()
            )
        }
    }
}
