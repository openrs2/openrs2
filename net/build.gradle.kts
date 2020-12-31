plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api("com.google.inject:guice:${Versions.guice}")
    api("io.netty:netty-transport:${Versions.netty}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")

    implementation(project(":buffer"))
    implementation("io.netty:netty-transport-native-epoll:${Versions.netty}:linux-aarch_64")
    implementation("io.netty:netty-transport-native-epoll:${Versions.netty}:linux-x86_64")
    implementation("io.netty:netty-transport-native-kqueue:${Versions.netty}:osx-x86_64")
    implementation("io.netty.incubator:netty-incubator-transport-native-io_uring:${Versions.nettyIoUring}:linux-x86_64")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Network")
            description.set(
                """
                Common Netty utility code.
            """.trimIndent()
            )
        }
    }
}
