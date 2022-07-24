plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.bundles.bouncyCastle)
    api(libs.bundles.guice)
    api(libs.jackson.databind)
    api(libs.netty.buffer)

    implementation(projects.util)

    testImplementation(projects.buffer)
    testImplementation(libs.jimfs)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Cryptography")
            description.set(
                """
                Provides implementations of cryptographic algorithms used by
                the client, including RSA, ISAAC, XTEA, Whirlpool and JAR
                signing.
            """.trimIndent()
            )
        }
    }
}
