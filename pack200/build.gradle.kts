plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Pack200")
            description.set(
                """
                Stubs for the java.util.jar.Pack200 class.
            """.trimIndent()
            )
        }
    }
}
