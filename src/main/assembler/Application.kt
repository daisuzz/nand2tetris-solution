package assembler

import java.io.File

class Application(private val code: Code, private val symbolTable: SymbolTable) {

    fun parseSymbol(parser: Parser) {
        var address = 0
        while (parser.hasMoreCommands()) {
            parser.advance()
            when(parser.commandType()){
                CommandType.A_COMMAND, CommandType.C_COMMAND -> address++
                CommandType.L_COMMAND -> {
                    val symbol = parser.symbol()
                    symbolTable.addEntry(symbol, address)
                }
            }
        }
    }

    fun assemble(parser: Parser, outputFile: File) {
        var addressForVariable = 16
        loop@ while (parser.hasMoreCommands()) {
            parser.advance()

            // コマンドの種類に応じたバイナリコードを生成する
            val binaryCode = when (parser.commandType()) {
                CommandType.L_COMMAND -> continue@loop
                CommandType.A_COMMAND -> {
                    val symbol = parser.symbol()
                    val isSymbol = symbol.toIntOrNull() == null
                    val address = when {
                        !isSymbol -> symbol
                        isSymbol && symbolTable.contains(symbol) -> symbolTable.getAddress(symbol).toString()
                        else -> {
                            symbolTable.addEntry(symbol, addressForVariable)
                            val targetAddress = addressForVariable
                            addressForVariable++
                            targetAddress.toString()
                        }
                    }
                    "0${address.toInt().toString(radix = 2).padStart(15, '0')}"
                }
                CommandType.C_COMMAND -> {
                    val compMnemonic = parser.comp()
                    val compBinary = code.comp(compMnemonic)

                    val destMnemonic = parser.dest()
                    val destBinary = code.dest(destMnemonic)

                    val jumpMnemonic = parser.jump()
                    val jumpBinary = code.jump(jumpMnemonic)

                    "111${compBinary + destBinary + jumpBinary}"
                }
            }

            // バイナリコードをファイルに書き込む
            outputFile.appendText(binaryCode + System.getProperty("line.separator"))
        }
    }
}
