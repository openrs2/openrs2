plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "org.openrs2.archive.ArchiveCommandKt"
}

dependencies {
    api("com.github.ajalt.clikt:clikt:${Versions.clikt}")

    implementation(project(":buffer"))
    implementation(project(":cache"))
    implementation(project(":db"))
    implementation(project(":json"))
    implementation(project(":util"))
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Archive")
            description.set(
                """
                Service for archiving clients, caches and XTEA keys in an
                efficient deduplicated format.
            """.trimIndent()
            )
        }
    }
}
