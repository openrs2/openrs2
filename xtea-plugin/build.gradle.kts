plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    annotationProcessor(libs.pf4j)

    compileOnly(libs.pf4j)
    compileOnly(libs.runelite.client)

    testImplementation(libs.runelite.client)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 XTEA Plugin")
            description.set(
                """
                A RuneLite/OpenOSRS plugin that collects XTEA keys and submits
                them to the OpenRS2 Archive.
            """.trimIndent()
            )
        }
    }
}

tasks.jar {
    manifest {
        attributes["Plugin-Description"] = "Collects XTEA keys and submits them to the OpenRS2 Archive"
        attributes["Plugin-Id"] = "OpenRS2 XTEA"
        attributes["Plugin-License"] = "ISC"
        attributes["Plugin-Provider"] = "OpenRS2"
        attributes["Plugin-Version"] = project.version
    }
}

tasks.register<JavaExec>("run") {
    classpath = project.sourceSets.test.get().runtimeClasspath
    mainClass.set("org.openrs2.xtea.XteaPluginTest")
    jvmArgs = listOf("-ea")
}
