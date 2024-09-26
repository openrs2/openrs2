import com.github.jk1.license.render.TextReportRenderer
import java.nio.file.Files

plugins {
    `maven-publish`
    application
    alias(libs.plugins.dependencyLicenseReport)
    alias(libs.plugins.shadow)
    kotlin("jvm")
}

application {
    applicationName = "openrs2"
    mainClass.set("org.openrs2.CommandKt")
}

dependencies {
    implementation(projects.archive)
    implementation(projects.bufferGenerator)
    implementation(projects.cacheCli)
    implementation(projects.compressCli)
    implementation(projects.crc32)
    implementation(projects.deob)
    implementation(projects.game)
    implementation(projects.log)
    implementation(projects.patcher)
}

tasks.shadowJar {
    archiveFileName.set("openrs2.jar")

    minimize {
        exclude(dependency("ch.qos.logback:logback-classic"))
        exclude(dependency("com.github.ajalt.mordant:mordant-jvm-ffm-jvm"))
        exclude(dependency("com.github.ajalt.mordant:mordant-jvm-jna-jvm"))
        exclude(dependency("com.github.jnr:jnr-ffi"))
        exclude(dependency("org.flywaydb:flyway-core"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
    }
}

tasks.register("generateAuthors") {
    inputs.dir("$rootDir/.git")
    outputs.file(layout.buildDirectory.file("AUTHORS"))

    doLast {
        Files.newOutputStream(layout.buildDirectory.file("AUTHORS").get().asFile.toPath()).use { out ->
            exec {
                commandLine("git", "shortlog", "-esn", "HEAD")
                standardOutput = out
            }.assertNormalExitValue()
        }
    }
}

licenseReport {
    renderers = arrayOf(TextReportRenderer())
}

val distTasks = listOf(
    "distTar",
    "distZip",
    "installDist"
)

configure(tasks.filter { it.name in distTasks }) {
    enabled = false
}

val shadowDistTasks = listOf(
    "installShadowDist",
    "shadowDistTar",
    "shadowDistZip"
)

configure(tasks.filter { it.name in shadowDistTasks }) {
    dependsOn("generateAuthors", "generateLicenseReport")
}

distributions {
    named("shadow") {
        distributionBaseName.set("openrs2")

        contents {
            from(layout.buildDirectory.file("AUTHORS"))
            from("$rootDir/CONTRIBUTING.md")
            from("$rootDir/DCO")
            from("$rootDir/LICENSE")
            from("$rootDir/README.md")
            from("$rootDir/etc/archive.example.yaml") {
                rename { "archive.yaml" }
                into("etc")
            }
            from("$rootDir/etc/config.example.yaml") {
                rename { "config.yaml" }
                into("etc")
            }
            from("$rootDir/share") {
                exclude(".*", "*~")
                into("share")
            }
            from(layout.buildDirectory.file("reports/dependency-license/THIRD-PARTY-NOTICES.txt")) {
                rename { "third-party-licenses.txt" }
                into("share/doc")
            }
        }
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "openrs2"
        setArtifacts(listOf(tasks.named("shadowDistZip").get()))

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
