plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.archive.ArchiveCommandKt")
}

dependencies {
    api("com.github.ajalt.clikt:clikt:${Versions.clikt}")

    implementation(project(":buffer"))
    implementation(project(":cache"))
    implementation(project(":cli"))
    implementation(project(":db"))
    implementation(project(":http"))
    implementation(project(":inject"))
    implementation(project(":json"))
    implementation(project(":net"))
    implementation(project(":protocol"))
    implementation(project(":util"))
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("io.ktor:ktor-jackson:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-thymeleaf:${Versions.ktor}")
    implementation("io.ktor:ktor-webjars:${Versions.ktor}")
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("org.jdom:jdom2:${Versions.jdom}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("org.thymeleaf:thymeleaf:${Versions.thymeleaf}")
    implementation("org.thymeleaf.extras:thymeleaf-extras-java8time:${Versions.thymeleafJava8Time}")
    implementation("org.webjars:bootstrap:${Versions.bootstrap}")
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
