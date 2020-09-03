plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(project(":crypto"))
    api("io.netty:netty-buffer:${Versions.netty}")

    implementation(project(":buffer"))
    implementation(project(":compress"))
    implementation(project(":util"))
    implementation("it.unimi.dsi:fastutil:${Versions.fastutil}")

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
