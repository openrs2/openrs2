import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

defaultTasks("build")

plugins {
    base
    id("com.github.ben-manes.versions") version Versions.versionsPlugin

    id("com.github.jk1.dependency-license-report") version Versions.dependencyLicenseReport apply false
    id("com.github.johnrengelman.shadow") version Versions.shadowPlugin apply false
    id("org.jmailen.kotlinter") version Versions.kotlinter apply false
    kotlin("jvm") version Versions.kotlin apply false
}

allprojects {
    group = "dev.openrs2"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        if (hasProperty("repoUsername")) {
            maven(url = "https://repo.openrs2.dev/repository/openrs2") {
                credentials {
                    username = findProperty("repoUsername")?.toString()
                    password = findProperty("repoPassword")?.toString()
                }
            }
            maven(url = "https://repo.openrs2.dev/repository/openrs2-snapshots") {
                credentials {
                    username = findProperty("repoUsername")?.toString()
                    password = findProperty("repoPassword")?.toString()
                }
            }
        }
        maven(url = "https://dl.bintray.com/michaelbull/maven")
    }

    plugins.withType<BasePlugin> {
        configure<BasePluginConvention> {
            archivesBaseName = "${rootProject.name}-$name"
        }
    }

    plugins.withType<ApplicationPlugin> {
        tasks.named<JavaExec>("run") {
            workingDir = rootProject.projectDir
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            withSourcesJar()

            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.withType<Test> {
        reports {
            html.isEnabled = false
            junitXml.isEnabled = true
        }
    }

    tasks.withType<JacocoReport> {
        dependsOn("test")

        reports {
            csv.isEnabled = false
            html.isEnabled = false
            xml.isEnabled = false
        }

        tasks.named("check") {
            dependsOn("jacocoTestReport")
        }
    }
}

val Project.free: Boolean
    get() = name != "nonfree" && parent?.name != "nonfree"

configure(subprojects.filter { it.free }) {
    apply(plugin = "jacoco")

    plugins.withType<KotlinPluginWrapper> {
        apply(plugin = "org.jmailen.kotlinter")

        dependencies {
            val api by configurations
            api(kotlin("stdlib-jdk8"))

            val implementation by configurations
            implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger-jvm:${Versions.inlineLogger}")

            val testImplementation by configurations
            testImplementation(kotlin("test-junit5"))
        }
    }

    plugins.withType<JavaPlugin> {
        dependencies {
            val testRuntimeOnly by configurations
            testRuntimeOnly("org.junit.jupiter:junit-jupiter:${Versions.junit}")
        }
    }

    plugins.withType<ApplicationPlugin> {
        dependencies {
            val runtimeOnly by configurations
            runtimeOnly("ch.qos.logback:logback-classic:${Versions.logback}")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        jvmArgs = listOf("-ea", "-Dio.netty.leakDetection.level=PARANOID")
    }

    plugins.withType<MavenPublishPlugin> {
        configure<PublishingExtension> {
            repositories {
                maven {
                    url = if (version.toString().endsWith("-SNAPSHOT")) {
                        uri("https://repo.openrs2.dev/repository/openrs2-snapshots")
                    } else {
                        uri("https://repo.openrs2.dev/repository/openrs2")
                    }

                    credentials {
                        username = findProperty("repoUsername")?.toString()
                        password = findProperty("repoPassword")?.toString()
                    }
                }
            }

            publications.withType<MavenPublication> {
                artifactId = "openrs2-${project.name}"

                pom {
                    url.set("https://www.openrs2.dev/")
                    inceptionYear.set("2019")

                    organization {
                        name.set("OpenRS2 Authors")
                        url.set("https://www.openrs2.dev/")
                    }

                    licenses {
                        license {
                            if (project.name in listOf("deob-annotations", "jsobject")) {
                                name.set("GNU Lesser General Public License v3.0 or later")
                                url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                            } else {
                                name.set("GNU General Public License v3.0 or later")
                                url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                            }
                        }
                    }

                    scm {
                        connection.set("scm:git:https://git.openrs2.dev/openrs2/openrs2.git")
                        developerConnection.set("scm:git:git@git.openrs2.dev:openrs2/openrs2.git")
                        url.set("https://git.openrs2.dev/openrs2/openrs2")
                    }

                    issueManagement {
                        system.set("Gitea")
                        url.set("https://git.openrs2.dev/openrs2/openrs2")
                    }

                    ciManagement {
                        system.set("Jenkins")
                        url.set("https://build.openrs2.dev/job/openrs2/")
                    }
                }
            }
        }
    }
}

val rejectVersionRegex = Regex("(?i)[._-](?:alpha|beta|rc|cr|m)")

tasks.withType<DependencyUpdatesTask> {
    gradleReleaseChannel = "current"
    revision = "release"

    rejectVersionIf {
        candidate.version.contains(rejectVersionRegex)
    }
}
