rootProject.name = "openrs2"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.openrs2.org/repository/openrs2")
        maven(url = "https://repo.runelite.net")
        mavenLocal()
        maven(url = "https://repo.openrs2.org/repository/openrs2-snapshots")
    }
}

pluginManagement {
    plugins {
        id("com.github.ben-manes.versions") version "0.40.0"
        id("com.github.jk1.dependency-license-report") version "2.0"
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("org.jetbrains.dokka") version "1.6.10"
        id("org.jmailen.kotlinter") version "3.8.0"
        kotlin("jvm") version "1.6.10"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

include(
    "all",
    "archive",
    "asm",
    "buffer",
    "buffer-generator",
    "cache",
    "cache-550",
    "cli",
    "compress",
    "compress-cli",
    "conf",
    "crc32",
    "crypto",
    "db",
    "decompiler",
    "deob",
    "deob-annotations",
    "deob-ast",
    "deob-bytecode",
    "deob-processor",
    "deob-util",
    "game",
    "http",
    "inject",
    "json",
    "log",
    "net",
    "nonfree",
    "nonfree:client",
    "nonfree:gl",
    "nonfree:loader",
    "nonfree:signlink",
    "nonfree:unpack",
    "nonfree:unpackclass",
    "patcher",
    "protocol",
    "util",
    "xtea-plugin",
    "yaml"
)
