rootProject.name = "openrs2"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.openrs2.org/repository/openrs2")
        mavenLocal()
        maven(url = "https://repo.openrs2.org/repository/openrs2-snapshots")
    }
}

enableFeaturePreview("VERSION_CATALOGS")

include(
    "all",
    "archive",
    "asm",
    "buffer",
    "cache",
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
    "yaml"
)
