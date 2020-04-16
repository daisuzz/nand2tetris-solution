package compiler

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
    val tempFile = File(inputFile.parent + "/" + inputFile.nameWithoutExtension + "T.xml")
    if (tempFile.exists()) tempFile.delete()
    val outputFile = File(inputFile.parent + "/" + inputFile.nameWithoutExtension + ".xml")
    if (outputFile.exists()) outputFile.delete()
    val compiler = CompilationEngine(JackTokenizer(inputFile), outputFile)
    compiler.compileClass()
}

fun tokenize(inputFile: File, tempFile: File) {
    val tokenizer = JackTokenizer(inputFile)

    tempFile.writeWithLF("<tokens>")
    while (tokenizer.hasMoreTokens()) {
        tokenizer.advance()
        when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> {
                val input = tokenizer.keyword()
                tempFile.writeWithLF("<keyword> $input </keyword>")
            }
            TokenType.SYMBOL -> {
                val input = when (val symbol = tokenizer.symbol()) {
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '&' -> "&amp;"
                    else -> symbol.toString()
                }
                tempFile.writeWithLF("<symbol> $input </symbol>")
            }
            TokenType.INT_CONST -> {
                val input = tokenizer.intVal()
                tempFile.writeWithLF("<integerConstant> $input </integerConstant>")
            }
            TokenType.STRING_CONST -> {
                val input = tokenizer.stringVal()
                tempFile.writeWithLF("<stringConstant> $input </stringConstant>")
            }
            TokenType.IDENTIFIER -> {
                val input = tokenizer.identifier()
                tempFile.writeWithLF("<identifier> $input </identifier>")
            }
        }
    }
    tempFile.writeWithLF("</tokens>")
}
