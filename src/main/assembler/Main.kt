package assembler

import main.assembler.Parser
import java.io.File
import java.lang.IllegalArgumentException

fun main(args: Array<String>) {

    val inputFilePath = args[0]

    // アセンブリ言語で記述されたファイルを読み込む
    val inputFile = File(inputFilePath)
    val br = if(inputFile.exists())inputFile.bufferedReader() else throw IllegalArgumentException("invalid file path!")

    val parser = Parser(br)
    val code = Code()

    // 出力先のパスにすでにファイルが存在する場合はファイルを削除
    val outputFileName = inputFilePath.substringBeforeLast(".") + ".hack"
    val outputFile = File(outputFileName)
    if(outputFile.exists()) outputFile.delete()

    while (parser.hasMoreCommands()) {

        // 次のコマンドを読み込む
        parser.advance()

        // コマンドの種類に応じてバイナリコードを生成する
        val binaryCode = when (val commandType = parser.commandType()) {
            CommandType.A_COMMAND -> {
                val symbol = parser.symbol()

                commandType.toBinaryCode(symbol)
            }
            CommandType.C_COMMAND -> {
                val compMnemonic = parser.comp()
                val compBinary = code.comp(compMnemonic)

                val destMnemonic = parser.dest()
                val destBinary = code.dest(destMnemonic)

                val jumpMnemonic = parser.jump()
                val jumpBinary = code.jump(jumpMnemonic)

                commandType.toBinaryCode(compBinary + destBinary + jumpBinary)
            }
            CommandType.L_COMMAND -> {
                TODO()
            }
        }

        // バイナリコードをファイルに書き込む
        outputFile.appendText(binaryCode + System.getProperty("line.separator"))
    }
}
