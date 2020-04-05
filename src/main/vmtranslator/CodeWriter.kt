package vmtranslator

import java.io.File

class CodeWriter(private val file: File) {

    private var eqNum = 0
    private var gtNum = 0
    private var ltNum = 0

    /**
     * 新しいVMファイルの変換が開始したことを知らせる
     */
    fun setFileName(fileName: String) {
        TODO()
    }

    /**
     * 与えられた算術コマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeArithmetic(command: String) {
        val assemblyCode = when (command) {
            "add" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + add() + incrementSP()
            }
            "sub" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + sub() + incrementSP()
            }
            "neg" -> {
                decrementSP() + loadSP() + neg() + incrementSP()
            }
            "eq" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + eq() + incrementSP()
            }
            "gt" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + gt() + incrementSP()
            }
            "lt" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + lt() + incrementSP()
            }
            "and" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + and() + incrementSP()
            }
            "or" -> {
                decrementSP() + loadSP() + pop() + decrementSP() + loadSP() + or() + incrementSP()
            }
            "not" -> {
                decrementSP() + loadSP() + not() + incrementSP()
            }
            else -> throw IllegalArgumentException()
        }

        file.appendText(assemblyCode)
    }

    /**
     * C_PUSHまたはC_POPコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writePushPop(command: String, segment: String, index: Int) {
        val assemblyCode = if (command == "push") {
            if (segment == "constant") {
                pushConst(index) + incrementSP()
            } else {
                TODO()
            }
        } else {
            TODO()
        }

        file.appendText(assemblyCode)
    }

    /**
     * 出力ファイルを閉じる
     * KotlinのFile::appendText()メソッドの中で書き込みと同時にclose処理をおこなっているので今回は実装しない
     */
    fun close() {}

    private fun loadSP() = "@SP\nA=M\n"
    private fun incrementSP() = "@SP\nM=M+1\n"
    private fun decrementSP() = "@SP\nM=M-1\n"
    private fun pop() = "D=M\nM=0\n"
    private fun defConst(const: Int) = "@$const\nD=A\n"
    private fun pushConst(const: Int) = defConst(const) + loadSP() + "M=D\n"
    private fun add() = "M=M+D\n"
    private fun sub() = "M=M-D\n"
    private fun neg() = "M=-M\n"
    private fun eq(): String {
        val command =
            "D=M-D\n@EQ$eqNum\nD;JEQ\n@NEQ$eqNum\n0;JMP\n(EQ$eqNum)\n${loadSP()}M=-1\n@EQNEXT$eqNum\n0;JMP\n(NEQ$eqNum)\n${loadSP()}M=0\n@EQNEXT$eqNum\n0;JMP\n(EQNEXT$eqNum)\n"
        eqNum++
        return command
    }

    private fun gt(): String {
        val command =
            "D=M-D\n@GT$gtNum\nD;JGT\n@NGT$gtNum\n0;JMP\n(GT$gtNum)\n${loadSP()}M=-1\n@GTNEXT$gtNum\n0;JMP\n(NGT$gtNum)\n${loadSP()}M=0\n@GTNEXT$gtNum\n0;JMP\n(GTNEXT$gtNum)\n"
        gtNum++
        return command
    }

    private fun lt(): String {
        val command =
            "D=M-D\n@LT$ltNum\nD;JLT\n@NLT$ltNum\n0;JMP\n(LT$ltNum)\n${loadSP()}M=-1\n@LTNEXT$ltNum\n0;JMP\n(NLT$ltNum)\n${loadSP()}M=0\n@LTNEXT$ltNum\n0;JMP\n(LTNEXT$ltNum)\n"
        ltNum++
        return command
    }

    private fun and() = "M=D&M\n"
    private fun or() = "M=D|M\n"
    private fun not() = "M=!M\n"

}
