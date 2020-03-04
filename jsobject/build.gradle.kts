plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 JavaScript Object")
            description.set(
                """
                Stubs for the netscape.javascript package.
            """.trimIndent()
            )
        }
    }
}
