package vmtranslator

import java.io.File

class Parser(file: File) {

    private val br = if (file.exists()) file.bufferedReader() else throw IllegalArgumentException()

    private var currentCommand = ""

    private var nextCommand = ""

    private var currentCommandElements = emptyList<String>()

    private lateinit var currentCommandType: CommandType

    /**
     * 入力においてさらにコマンドが存在するか
     */
    fun hasMoreCommands(): Boolean {
        var line = br.readLine() ?: return false
        nextCommand = trim(line.substringBefore("//"))
        // コメント行もしくは空行の場合、コマンドを読み込むまで、次の行を読み込み続ける
        while (nextCommand.isEmpty()) {
            line = br.readLine() ?: return false
            nextCommand = trim(line.substringBefore("//"))
        }
        return true
    }

    /**
     * 入力から次のコマンドを読み込んで現在の入力としてセットする
     * hasMoreCommand()がtrueのときだけ呼び出される
     */
    fun advance() {
        currentCommand = nextCommand
    }

    /**
     * 現在の入力のVMコマンドのタイプを返す
     */
    fun commandType(): CommandType {
        currentCommandElements = currentCommand.split("(\\s)+".toRegex())
        currentCommandType = when (currentCommandElements[0]) {
            "push" -> CommandType.C_PUSH
            "pop" -> CommandType.C_POP
            "label" -> CommandType.C_LABEL
            "goto" -> CommandType.C_GOTO
            "if-goto" -> CommandType.C_IF
            "function" -> CommandType.C_FUNCTION
            "call" -> CommandType.C_CALL
            "return" -> CommandType.C_RETURN
            else -> CommandType.C_ARITHMETIC
        }

        return currentCommandType
    }

    /**
     * 現在のコマンドの最初の引数を返す
     * C_ARITHMETICの場合コマンド自体を返す
     * コマンドのタイプがC_RETURNの場合呼び出さない
     */
    fun arg1(): String {
        return if (currentCommandType == CommandType.C_ARITHMETIC) {
            currentCommandElements[0]
        } else {
            currentCommandElements[1]
        }
    }

    /**
     * 現在のコマンドの2番目の引数を返す
     * C_PUSH, C_POP, C_FUNCTION, C_CALLの場合のみ呼び出される
     */
    fun arg2(): Int {
        if (!currentCommandType.hasArg2()) throw UnsupportedOperationException()
        return currentCommandElements[2].toInt()
    }

    private fun trim(str: String?): String {
        return str?.trim() ?: throw NullPointerException()
    }
}
