plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.archive.ArchiveCommandKt")
}

dependencies {
    api(libs.clikt)

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
    implementation(project(":yaml"))
    implementation(libs.bootstrap)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.thymeleaf)
    implementation(libs.byteUnits)
    implementation(libs.flyway)
    implementation(libs.guava)
    implementation(libs.hikaricp)
    implementation(libs.jdom)
    implementation(libs.jsoup)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.postgres)
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
