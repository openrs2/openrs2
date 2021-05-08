import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.KotlinterPlugin
import java.io.ByteArrayOutputStream
import java.net.URL

defaultTasks("build")

plugins {
    base
    id("com.github.ben-manes.versions")
    id("org.jetbrains.dokka")
    kotlin("jvm")

    id("org.jmailen.kotlinter") apply false
}

allprojects {
    group = "org.openrs2"
    version = "0.1.0-SNAPSHOT"

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

    plugins.withType<KotlinPluginWrapper> {
        kotlin {
            explicitApi()
        }
    }

    plugins.withType<KotlinterPlugin> {
        configure<KotlinterExtension> {
            // see https://github.com/pinterest/ktlint/issues/764
            disabledRules = arrayOf("indent", "parameter-list-wrapping")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(11)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xinline-classes", "-Xjsr305=strict")
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

    configure<JacocoPluginExtension> {
        /*
         * Temporarily override bundled Jacoco for compatibility with Kotlin
         * 1.5.
         */
        toolVersion = "0.8.7"
    }

    plugins.withType<KotlinPluginWrapper> {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "org.jmailen.kotlinter")

        dependencies {
            val api by configurations
            for (module in listOf("stdlib", "stdlib-common", "stdlib-jdk7", "stdlib-jdk8")) {
                api("org.jetbrains.kotlin:kotlin-$module") {
                    version {
                        strictly(kotlinPluginVersion)
                    }
                }
            }

            val implementation by configurations
            implementation(kotlin("reflect"))
            implementation(libs.inlineLogger)

            val testImplementation by configurations
            testImplementation(kotlin("test-junit5"))
            testImplementation(libs.junit.api)

            val testRuntimeOnly by configurations
            testRuntimeOnly(libs.junit.engine)
        }
    }

    tasks.withType<DokkaTask> {
        inputs.dir("$rootDir/.git")

        dokkaSourceSets {
            configureEach {
                includeNonPublic.set(true)
                jdkVersion.set(11)
                moduleName.set("openrs2")

                sourceLink {
                    localDirectory.set(rootDir)
                    remoteUrl.set(URL("https://git.openrs2.org/openrs2/openrs2/src/commit/${commitHash()}"))
                    remoteLineSuffix.set("#L")
                }

                if (project.hasProperty("externalDocumentationLinks")) {
                    externalDocumentationLink {
                        url.set(URL("https://asm.ow2.io/javadoc/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://www.bouncycastle.org/docs/docs1.5on/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://www.bouncycastle.org/docs/pkixdocs1.5on/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://ajalt.github.io/clikt/api/clikt/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://commons.apache.org/proper/commons-compress/javadocs/api-${libs.versions.commons.compress.get()}/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://guava.dev/releases/${libs.versions.guava.get()}/api/docs/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://google.github.io/guice/api-docs/${libs.versions.guice.get()}/javadoc/"))
                    }

                    val jacksonVersion = libs.versions.jackson.get().split(".")
                        .take(2)
                        .joinToString(".")

                    externalDocumentationLink {
                        url.set(URL("https://fasterxml.github.io/jackson-annotations/javadoc/$jacksonVersion/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://fasterxml.github.io/jackson-core/javadoc/$jacksonVersion/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://fasterxml.github.io/jackson-databind/javadoc/$jacksonVersion/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://fasterxml.github.io/jackson-dataformats-text/javadoc/yaml/$jacksonVersion/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("http://www.jdom.org/docs/apidocs/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://google.github.io/jimfs/releases/${libs.versions.jimfs.get()}/api/docs/"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://junit.org/junit5/docs/${libs.versions.junit.get()}/api/"))
                    }

                    externalDocumentationLink {
                        val version = libs.versions.netty.get().split(".")
                            .take(2)
                            .joinToString(".")

                        url.set(URL("https://netty.io/$version/api/"))
                    }
                }
            }
        }
    }

    plugins.withType<ApplicationPlugin> {
        dependencies {
            val runtimeOnly by configurations
            runtimeOnly(libs.logback)
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
                    name = "openrs2"
                    url = if (version.toString().endsWith("-SNAPSHOT")) {
                        uri("https://repo.openrs2.org/repository/openrs2-snapshots")
                    } else {
                        uri("https://repo.openrs2.org/repository/openrs2")
                    }

                    credentials(PasswordCredentials::class)
                }
            }

            publications.withType<MavenPublication> {
                artifactId = "openrs2-${project.name}"

                pom {
                    url.set("https://www.openrs2.org/")
                    inceptionYear.set("2019")

                    organization {
                        name.set("OpenRS2 Authors")
                        url.set("https://www.openrs2.org/")
                    }

                    licenses {
                        license {
                            name.set("ISC License")
                            url.set("https://opensource.org/licenses/ISC")
                        }
                    }

                    scm {
                        connection.set("scm:git:https://git.openrs2.org/openrs2/openrs2.git")
                        developerConnection.set("scm:git:git@git.openrs2.org:openrs2/openrs2.git")
                        url.set("https://git.openrs2.org/openrs2/openrs2")
                    }

                    issueManagement {
                        system.set("Gitea")
                        url.set("https://git.openrs2.org/openrs2/openrs2")
                    }

                    ciManagement {
                        system.set("Drone")
                        url.set("https://build.openrs2.org/openrs2/openrs2/")
                    }
                }
            }
        }
    }
}

val rejectVersionRegex = Regex("(?i)[._-](?:alpha|beta|rc|cr|m|dev)")

tasks.dependencyUpdates {
    gradleReleaseChannel = "current"
    revision = "release"

    rejectVersionIf {
        candidate.version.contains(rejectVersionRegex)
    }
}

tasks.register("publish") {
    dependsOn("publishDokka")
}

tasks.register<Exec>("publishDokka") {
    dependsOn(":dokkaHtmlCollector")

    commandLine(
        "rsync",
        "-e",
        "ssh -oStrictHostKeyChecking=accept-new",
        "--delete",
        "-rtz",
        "$buildDir/dokka/htmlCollector/",
        "build@docs.openrs2.org:/srv/www/docs"
    )
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
            options.compilerArgs = listOf("-Amap=$rootDir/share/deob/map/${project.name}.yaml")
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

fun commitHash(): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = out
    }.assertNormalExitValue()
    return String(out.toByteArray(), Charsets.UTF_8).trim()
}
