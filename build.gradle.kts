import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.net.URL

defaultTasks("build")

plugins {
    base
    id("com.github.ben-manes.versions") version Versions.versionsPlugin
    id("org.jetbrains.dokka") version Versions.dokka
    kotlin("jvm") version Versions.kotlin

    id("com.github.jk1.dependency-license-report") version Versions.dependencyLicenseReport apply false
    id("com.github.johnrengelman.shadow") version Versions.shadowPlugin apply false
    id("org.jmailen.kotlinter") version Versions.kotlinter apply false
}

repositories {
    jcenter()
}

allprojects {
    group = "dev.openrs2"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven(url = "https://repo.openrs2.dev/repository/openrs2")
        mavenLocal()
        maven(url = "https://repo.openrs2.dev/repository/openrs2-snapshots")
    }

    plugins.withType<BasePlugin> {
        configure<BasePluginConvention> {
            archivesBaseName = "${rootProject.name}-$name"
        }
    }

    plugins.withType<ApplicationPlugin> {
        tasks.named<JavaExec>("run") {
            standardInput = System.`in`
            workingDir = rootDir
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            withSourcesJar()

            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
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

val Project.isFree: Boolean
    get() = name != "nonfree" && parent?.name != "nonfree"

configure(subprojects.filter { it.isFree }) {
    apply(plugin = "jacoco")

    plugins.withType<KotlinPluginWrapper> {
        apply(plugin = "org.jmailen.kotlinter")

        dependencies {
            val api by configurations
            api(kotlin("stdlib-jdk8"))

            val implementation by configurations
            implementation(kotlin("reflect"))
            implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:${Versions.inlineLogger}")

            val testImplementation by configurations
            testImplementation(kotlin("test-junit5"))
            testImplementation("org.junit.jupiter:junit-jupiter-api") {
                version {
                    strictly(Versions.junit)
                }
            }
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
                        username = findProperty("openrs2RepoUsername")?.toString()
                        password = findProperty("openrs2RepoPassword")?.toString()
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
                            name.set("ISC License")
                            url.set("https://opensource.org/licenses/ISC")
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
                        system.set("Drone")
                        url.set("https://build.openrs2.dev/openrs2/openrs2/")
                    }
                }
            }
        }
    }
}

val rejectVersionRegex = Regex("(?i)[._-](?:alpha|beta|rc|cr|m)")

tasks.dependencyUpdates {
    gradleReleaseChannel = "current"
    revision = "release"

    rejectVersionIf {
        candidate.version.contains(rejectVersionRegex)
    }
}

fun commitHash(): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = out
    }.assertNormalExitValue()
    return String(out.toByteArray(), Charsets.UTF_8).trim()
}

tasks.dokka {
    outputDirectory = "$buildDir/kdoc"
    outputFormat = "html"

    subProjects = subprojects.filter { it.isFree && it.name != "deob-annotations" }.map { it.name }

    configuration {
        includeNonPublic = true

        /*
         * XXX(gpe): ideally we'd use 11 here, but we're blocked on
         * https://github.com/Kotlin/dokka/issues/294. 9 is the most recent
         * working version for now.
         */
        jdkVersion = 9

        sourceLink {
            path = "."
            url = "https://git.openrs2.dev/openrs2/openrs2/src/commit/${commitHash()}"
            lineSuffix = "#L"
        }

        externalDocumentationLink {
            url = URL("https://asm.ow2.io/javadoc/")
        }

        externalDocumentationLink {
            url = URL("https://www.bouncycastle.org/docs/docs1.5on/")
        }

        externalDocumentationLink {
            url = URL("https://www.bouncycastle.org/docs/pkixdocs1.5on/")
        }

        externalDocumentationLink {
            url = URL("https://ajalt.github.io/clikt/api/clikt/")
        }

        externalDocumentationLink {
            url = URL("https://commons.apache.org/proper/commons-compress/javadocs/api-${Versions.commonsCompress}/")
        }

        externalDocumentationLink {
            url = URL("https://guava.dev/releases/${Versions.guava}/api/docs/")
        }

        externalDocumentationLink {
            url = URL("https://google.github.io/guice/api-docs/${Versions.guice}/javadoc/")
        }

        val jacksonVersion = Versions.jackson.split(".")
            .take(2)
            .joinToString(".")

        externalDocumentationLink {
            url = URL("https://fasterxml.github.io/jackson-annotations/javadoc/$jacksonVersion/")
        }

        externalDocumentationLink {
            url = URL("https://fasterxml.github.io/jackson-core/javadoc/$jacksonVersion/")
        }

        externalDocumentationLink {
            url = URL("https://fasterxml.github.io/jackson-databind/javadoc/$jacksonVersion/")
        }

        externalDocumentationLink {
            url = URL("https://fasterxml.github.io/jackson-dataformats-text/javadoc/yaml/$jacksonVersion/")
        }

        externalDocumentationLink {
            url = URL("http://www.jdom.org/docs/apidocs/")
        }

        externalDocumentationLink {
            url = URL("https://google.github.io/jimfs/releases/${Versions.jimfs}/api/docs/")
        }

        externalDocumentationLink {
            url = URL("https://junit.org/junit5/docs/${Versions.junit}/api/")
        }

        externalDocumentationLink {
            val version = Versions.netty.split(".")
                .take(2)
                .joinToString(".")

            url = URL("https://netty.io/$version/api/")
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

configure(project(":nonfree").subprojects) {
    plugins.withType<JavaPlugin> {
        dependencies {
            val annotationProcessor by configurations
            annotationProcessor(project(":deob-processor"))

            val compileOnly by configurations
            compileOnly(project(":deob-annotations"))
        }

        tasks.named<JavaCompile>("compileJava") {
            options.compilerArgs = listOf("-Amap=${project.rootDir}/share/deob/map/${project.name}.yaml")
        }
    }
}

project(":nonfree") {
    apply(plugin = "base")
}

project(":nonfree:client") {
    apply(plugin = "application")

    configure<ApplicationPluginConvention> {
        mainClassName = "client"
    }

    tasks.named<JavaExec>("run") {
        args("1", "live", "en", "game0")
    }

    plugins.withType<JavaPlugin> {
        dependencies {
            val implementation by configurations
            implementation(project(":nonfree:gl"))
            implementation(project(":nonfree:signlink"))
        }
    }
}

project(":nonfree:gl") {
    apply(plugin = "java-library")
}

project(":nonfree:loader") {
    apply(plugin = "java")

    plugins.withType<JavaPlugin> {
        dependencies {
            val implementation by configurations
            implementation(project(":nonfree:signlink"))
            implementation(project(":nonfree:unpack"))
        }
    }
}

project(":nonfree:signlink") {
    apply(plugin = "java-library")
}

project(":nonfree:unpack") {
    apply(plugin = "java-library")
}

project(":nonfree:unpackclass") {
    apply(plugin = "java-library")

    plugins.withType<JavaPlugin> {
        dependencies {
            val implementation by configurations
            implementation(project(":nonfree:unpack"))
        }
    }
}
