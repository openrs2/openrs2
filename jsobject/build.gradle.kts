plugins {
    `java-library`
    `maven-publish`
}

// XXX(gpe): this module MUST NOT depend on any of the GPLed modules.

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 JavaScript Object")
            description.set("""
                Stubs for the netscape.javascript package.
            """.trimIndent())
        }
    }
}
