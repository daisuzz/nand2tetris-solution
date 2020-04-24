package compiler.generator

import java.io.File

fun main(vararg args: String) {

    val source = File(args[0])
    if (source.isDirectory) {
        val jackFiles = source.listFiles { file -> file.extension == "jack" } ?: throw IllegalArgumentException()
        jackFiles.forEach { inputFile -> compile(inputFile) }
    } else {
        compile(source)
    }
}

fun compile(inputFile: File) {
    val outputFile = File(inputFile.parent + "/" + inputFile.nameWithoutExtension + ".vm")
    if (outputFile.exists()) outputFile.delete()
    val compiler = CompilationEngine(inputFile, outputFile)
    compiler.compileClass()
}
