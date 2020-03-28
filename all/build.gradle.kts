import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jk1.license.render.TextReportRenderer

plugins {
    `maven-publish`
    application
    id("com.github.jk1.dependency-license-report")
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
}

application {
    applicationName = "openrs2"
    mainClassName = "dev.openrs2.LauncherKt"
}

dependencies {
    implementation(project(":bundler"))
    implementation(project(":decompiler"))
    implementation(project(":deob"))
    implementation(project(":deob-ast"))
    implementation(project(":game"))
}

tasks.withType<ShadowJar> {
    minimize()
}

licenseReport {
    renderers = arrayOf(TextReportRenderer())
}

val distTasks = listOf(
    "distTar",
    "distZip",
    "installDist",
    "installShadowDist",
    "shadowDistTar",
    "shadowDistZip"
)

configure(tasks.filter { it.name in distTasks }) {
    dependsOn("generateLicenseReport")
}

distributions {
    all {
        contents {
            from("${rootProject.projectDir}/DCO")
            from("${rootProject.projectDir}/LICENSE")
            from("${rootProject.projectDir}/README.md")
            from("${rootProject.projectDir}/docs") {
                include("*.md")
                into("docs")
            }
            from("$buildDir/reports/dependency-license/THIRD-PARTY-NOTICES.txt") {
                rename { "third-party-licenses.txt" }
                into("docs")
            }
        }
    }

    named("shadow") {
        distributionBaseName.set("openrs2-shadow")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(tasks.named("shadowDistZip").get())

        pom {
            packaging = "zip"
            name.set("OpenRS2")
            description.set(
                """
                OpenRS2 is an open-source multiplayer game server and suite of
                associated tools. It is compatible with build 550 of the
                RuneScape client, which was released in mid-2009.
            """.trimIndent()
            )
        }
    }
}
