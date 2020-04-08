package vmtranslator

import java.io.File

fun main(args: Array<String>) {
    val sourcePath = args[0].trimEnd('/')
    val source = File(sourcePath)

    val outputFilePath = if (source.isFile) {
        sourcePath.substringBeforeLast(".") + ".asm"
    } else {
        val dirName = sourcePath.substringAfterLast("/")
        "$sourcePath/$dirName.asm"
    }
    val outputFile = File(outputFilePath)
    if (outputFile.exists()) outputFile.delete()

    val codeWriter = CodeWriter(outputFile)

    if (source.isFile) {
        val parser = Parser(source)
        val inputFileName = sourcePath.substringAfterLast("/").substringBeforeLast(".")
        codeWriter.setFileName(inputFileName)
        translate(parser, codeWriter)
    } else {

        val sysFile = source.listFiles { file -> file.name == "Sys.vm" }?.firstOrNull()
        if (sysFile != null) {
            val parser = Parser(sysFile)
            codeWriter.setFileName(sysFile.nameWithoutExtension)
            codeWriter.writeInit()
            translate(parser, codeWriter)
        }

        val vmFiles = source.listFiles { file -> file.name != "Sys.vm" && file.extension == "vm" }
        if (!vmFiles.isNullOrEmpty()) {
            vmFiles.forEach { vmFile ->
                val parser = Parser(vmFile)
                codeWriter.setFileName(vmFile.nameWithoutExtension)
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
                val label = parser.arg1()
                codeWriter.writeLabel(label)
            }
            CommandType.C_GOTO -> {
                val label = parser.arg1()
                codeWriter.writeGoto(label)
            }
            CommandType.C_IF -> {
                val label = parser.arg1()
                codeWriter.writeIf(label)
            }
            CommandType.C_FUNCTION -> {
                val functionName = parser.arg1()
                val numLocals = parser.arg2()
                codeWriter.writeFunction(functionName, numLocals)
            }
            CommandType.C_CALL -> {
                val functionName = parser.arg1()
                val numArgs = parser.arg2()
                codeWriter.writeCall(functionName, numArgs)
            }
            CommandType.C_RETURN -> {
                codeWriter.writeReturn()
            }
        }
    }
}


