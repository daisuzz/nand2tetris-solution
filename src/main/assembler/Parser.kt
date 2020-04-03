package assembler

import java.io.BufferedReader
import java.io.File

/**
 * アセンブリコマンドを基本要素(フィールド, シンボル)に分解するモジュール
 */
class Parser(file: File) {

    private val br: BufferedReader = if (file.exists()) file.bufferedReader() else throw IllegalArgumentException("invalid file path!")

    private var currentCommand = ""

    private var nextCommand: String = ""

    /**
     * 入力にまだコマンドが存在するかどうか。
     */
    fun hasMoreCommands(): Boolean {
        var line = br.readLine() ?: return false
        nextCommand = trimAllWhiteSpace(line.substringBefore("//"))
        // コメント行もしくは空行の場合、コマンドを読み込むまで、次の行を読み込み続ける
        while (nextCommand.isEmpty()) {
            line = br.readLine() ?: return false
            nextCommand = trimAllWhiteSpace(line.substringBefore("//"))
        }
        return true
    }

    /**
     * 入力から次のコマンドを読み、現在のコマンドとする。
     * hasMoreCommandsがtrueを返すときのみ呼ばれる。
     * 現在のコマンドの初期値は空。
     */
    fun advance() {
        currentCommand = trimAllWhiteSpace(nextCommand)
    }

    /**
     * 現在のコマンドの種類を返す。
     */
    fun commandType(): CommandType {
        return when (currentCommand[0]) {
            '@' -> CommandType.A_COMMAND
            '(' -> CommandType.L_COMMAND
            else -> CommandType.C_COMMAND
        }
    }

    /**
     * 現在のコマンド@Xxxまたは(Xxx)のXxxを返す
     * Xxxは10進数またはシンボル
     * commandType()がA_COMMAND, L_COMMANDを返すときだけ呼ばれる
     */
    fun symbol(): String {
        // @から始まるときは末尾まで読み込む, (から始まるときは末尾の)は読み込まない
        val end = if (currentCommand[0] == '@') currentCommand.length else currentCommand.length - 1
        return currentCommand.substring(1 until end)
    }

    /**
     * 現在のC命令のdestニーモニックを返す
     * commandType()がC_COMMANDを返すときだけ呼ばれる
     */
    fun dest(): String {
        return currentCommand.substringBefore('=', "")
    }

    /**
     * 現在のC命令のcompニーモニックを返す
     * commandType()がC_COMMANDを返すときだけ呼ばれる
     */
    fun comp(): String {
        val compAndJump = currentCommand.substringAfter('=')
        return compAndJump.substringBefore(';')
    }

    /**
     * 現在のC命令のjumpニーモニックを返す
     * commandType()がC_COMMANDを返すときだけ呼ばれる
     */
    fun jump(): String {
        return currentCommand.substringAfter(';', "")
    }

    private fun trimAllWhiteSpace(str: String?): String {
        return str?.replace("\\s".toRegex(), "") ?: throw NullPointerException()
    }
}
