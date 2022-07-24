plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClass.set("org.openrs2.archive.ArchiveCommandKt")
}

dependencies {
    api(libs.bundles.guice)
    api(libs.clikt)

    implementation(projects.buffer)
    implementation(projects.cache550)
    implementation(projects.cli)
    implementation(projects.compress)
    implementation(projects.db)
    implementation(projects.http)
    implementation(projects.inject)
    implementation(projects.json)
    implementation(projects.log)
    implementation(projects.net)
    implementation(projects.protocol)
    implementation(projects.util)
    implementation(projects.yaml)
    implementation(libs.bootstrap)
    implementation(libs.bootstrapTable)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.thymeleaf)
    implementation(libs.byteUnits)
    implementation(libs.flyway)
    implementation(libs.guava)
    implementation(libs.hikaricp)
    implementation(libs.jackson.jsr310)
    implementation(libs.jdom)
    implementation(libs.jquery)
    implementation(libs.jsoup)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.netty.handler)
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
