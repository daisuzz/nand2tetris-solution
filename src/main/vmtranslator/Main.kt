package vmtranslator

import java.io.File

fun main(args: Array<String>) {
    val sourcePath = args[0].trimEnd('/')
    val source = File(sourcePath)

    val outputFilePath =
        if (source.isFile) sourcePath.substringBeforeLast(".") + ".asm" else sourcePath.substringAfterLast("/") + ".asm"
    val outputFile = File(outputFilePath)
    if (outputFile.exists()) outputFile.delete()

    val codeWriter = CodeWriter(outputFile)

    if (source.isFile) {
        val parser = Parser(source)
        val inputFileName = sourcePath.substringAfterLast("/").substringBeforeLast(".")
        codeWriter.setFileName(inputFileName)
        translate(parser, codeWriter)
    } else {
        val vmFiles = source.listFiles { file -> file.extension == "vm" }
        if (!vmFiles.isNullOrEmpty()) {
            vmFiles.forEach { vmfile ->
                val parser = Parser(vmfile)
                translate(parser, codeWriter)
            }
        }
    }
}

fun translate(parser: Parser, codeWriter: CodeWriter) {
    loop@ while (parser.hasMoreCommands()) {
        parser.advance()

        when (val commandType = parser.commandType()) {
            CommandType.C_ARITHMETIC -> {
                val command = parser.arg1()
                codeWriter.writeArithmetic(command)
            }
            CommandType.C_PUSH -> {
                val segment = parser.arg1()
                val index = parser.arg2()
                codeWriter.writePushPop("push", segment, index)
            }
            CommandType.C_POP -> {
                val segment = parser.arg1()
                val index = parser.arg2()
                codeWriter.writePushPop("pop", segment, index)
            }
            CommandType.C_LABEL -> {
                TODO()
            }
            CommandType.C_GOTO -> {
                TODO()
            }
            CommandType.C_IF -> {
                TODO()
            }
            CommandType.C_FUNCTION -> {
                TODO()
            }
            CommandType.C_CALL -> {
                TODO()
            }
            CommandType.C_RETURN -> {
                TODO()
            }
        }
    }
}
