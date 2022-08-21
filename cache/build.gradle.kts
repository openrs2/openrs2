plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(projects.crypto)
    api(libs.bundles.guice)
    api(libs.commons.compress)
    api(libs.fastutil)
    api(libs.netty.buffer)

    implementation(projects.buffer)
    implementation(projects.compress)
    implementation(projects.util)
    implementation(libs.sqlite)

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
