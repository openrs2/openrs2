plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(libs.bundles.guice)
    api(libs.kotlin.coroutines.core)
    api(libs.netty.transport)

    implementation(projects.buffer)
    implementation("io.netty:netty-transport-native-epoll:${libs.versions.netty.get()}:linux-aarch_64")
    implementation("io.netty:netty-transport-native-epoll:${libs.versions.netty.get()}:linux-x86_64")
    implementation("io.netty:netty-transport-native-io_uring:${libs.versions.netty.get()}:linux-aarch_64")
    implementation("io.netty:netty-transport-native-io_uring:${libs.versions.netty.get()}:linux-x86_64")
    implementation("io.netty:netty-transport-native-kqueue:${libs.versions.netty.get()}:osx-x86_64")
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
