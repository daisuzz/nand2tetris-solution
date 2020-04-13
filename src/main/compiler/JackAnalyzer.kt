package compiler

import java.io.File

fun main(vararg args: String) {

    val source = File(args[0])
    if (source.isDirectory) {
        val jackFiles = source.listFiles { file -> file.extension == "jack" } ?: throw IllegalArgumentException()
        jackFiles.forEach { inputFile ->
            val outputFile = File(inputFile.parent + "/" + inputFile.nameWithoutExtension + ".xml")
            if (outputFile.exists()) outputFile.delete()
            tokenize(inputFile, outputFile)
        }
    } else {
        val outputFile = File(source.parent + "/" + source.nameWithoutExtension + ".xml")
        if (outputFile.exists()) outputFile.delete()
        tokenize(source, outputFile)
    }
}

fun tokenize(inputFile: File, outputFile: File) {
    val tokenizer = JackTokenizer(inputFile)

    outputFile.writeWithLF("<tokens>")
    while (tokenizer.hasMoreTokens()) {
        tokenizer.advance()
        when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> {
                val input = tokenizer.keyword()
                outputFile.writeWithLF("<keyword> $input </keyword>")
            }
            TokenType.SYMBOL -> {
                val input = when (val symbol = tokenizer.symbol()) {
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '&' -> "&amp;"
                    else -> symbol.toString()
                }
                outputFile.writeWithLF("<symbol> $input </symbol>")
            }
            TokenType.INT_CONST -> {
                val input = tokenizer.intVal()
                outputFile.writeWithLF("<integerConstant> $input </integerConstant>")
            }
            TokenType.STRING_CONST -> {
                val input = tokenizer.stringVal()
                outputFile.writeWithLF("<stringConstant> $input </stringConstant>")
            }
            TokenType.IDENTIFIER -> {
                val input = tokenizer.identifier()
                outputFile.writeWithLF("<identifier> $input </identifier>")
            }
        }
    }
    outputFile.writeWithLF("</tokens>")
}

private fun File.writeWithLF(text: String) {
    this.appendText("$text\n")
}
