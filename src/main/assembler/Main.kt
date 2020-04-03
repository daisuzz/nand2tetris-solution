package assembler

import java.io.File

fun main(args: Array<String>) {

    val inputFilePath = args[0]
    val inputFile = File(inputFilePath)

    val outputFilePath = inputFilePath.substringBeforeLast(".") + ".hack"
    val outputFile = File(outputFilePath)
    if (outputFile.exists()) outputFile.delete()

    val parserForSymbolTable = Parser(inputFile)
    val parserForAssemble = Parser(inputFile)
    val code = Code()
    val symbolTable = SymbolTable()
    val application = Application(code, symbolTable)

    application.parseSymbol(parserForSymbolTable)

    application.assemble(parserForAssemble, outputFile)
}
