plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("com.google.inject:guice:${Versions.guice}")
    api("io.netty:netty-buffer:${Versions.netty}")
    api("org.bouncycastle:bcpkix-jdk15on:${Versions.bouncyCastlePkix}")
    api("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastleProvider}")

    testImplementation("com.google.jimfs:jimfs:${Versions.jimfs}")
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
                the client, including RSA, ISAAC, XTEA and JAR signing.
            """.trimIndent()
            )
        }
    }
}
