package dev.openrs2.cli

import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

fun RawOption.inputStream(): NullableOption<InputStream, InputStream> {
    return convert("FILE") {
        return@convert if (it == "-") {
            System.`in`
        } else {
            Files.newInputStream(java.nio.file.Paths.get(it))
        }
    }
}

fun NullableOption<InputStream, InputStream>.defaultStdin(): OptionWithValues<InputStream, InputStream, InputStream> {
    return default(System.`in`, "-")
}

fun RawOption.outputStream(): NullableOption<OutputStream, OutputStream> {
    return convert("FILE") {
        return@convert if (it == "-") {
            System.out
        } else {
            Files.newOutputStream(java.nio.file.Paths.get(it))
        }
    }
}

fun NullableOption<OutputStream, OutputStream>.defaultStdout(): OptionWithValues<OutputStream, OutputStream,
    OutputStream> {

    return default(System.out, "-")
}
