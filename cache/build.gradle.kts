plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    implementation(project(":buffer"))
    implementation(project(":compress"))
    implementation(project(":crypto"))
    implementation(project(":util"))

    testImplementation("com.google.jimfs:jimfs:${Versions.jimfs}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Cache")
            description.set(
                """
                A library for reading and writing the RuneScape cache.
            """.trimIndent()
            )
        }
    }
}
