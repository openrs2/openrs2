plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(project(":crypto"))
    api(libs.fastutil)
    api(libs.guice)
    api(libs.netty.buffer)

    implementation(project(":buffer"))
    implementation(project(":compress"))
    implementation(project(":util"))

    testImplementation(libs.jimfs)
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
