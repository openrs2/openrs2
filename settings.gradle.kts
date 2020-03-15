import java.nio.file.Files

rootProject.name = "openrs2"

include(
    "all",
    "asm",
    "bundler",
    "common",
    "decompiler",
    "deob",
    "deob-annotations",
    "deob-ast",
    "game"
)

if (Files.exists(rootProject.projectDir.toPath().resolve("nonfree/build.gradle.kts"))) {
    include(
        "nonfree",
        "nonfree:client",
        "nonfree:gl",
        "nonfree:loader",
        "nonfree:signlink",
        "nonfree:unpack",
        "nonfree:unpacker"
    )
}
