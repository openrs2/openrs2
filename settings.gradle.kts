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
    "compress-bzip2",
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
