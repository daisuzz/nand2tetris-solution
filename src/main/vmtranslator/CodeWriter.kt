package vmtranslator

import java.io.File

class CodeWriter(private val file: File) {

    private var eqNum = 0
    private var gtNum = 0
    private var ltNum = 0

    private var fileName = ""

    private var returnAddressCounter = 0

    /**
     * 新しいVMファイルの変換が開始したことを知らせる
     */
    fun setFileName(fileName: String) {
        this.fileName = fileName
    }

    /**
     * 与えられた算術コマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeArithmetic(command: String) {
        val assemblyCode = when (command) {
            "add" -> {
                pop() + decrementSP() + loadSP() + add() + incrementSP()
            }
            "sub" -> {
                pop() + decrementSP() + loadSP() + sub() + incrementSP()
            }
            "neg" -> {
                decrementSP() + loadSP() + neg() + incrementSP()
            }
            "eq" -> {
                pop() + decrementSP() + loadSP() + eq() + incrementSP()
            }
            "gt" -> {
                pop() + decrementSP() + loadSP() + gt() + incrementSP()
            }
            "lt" -> {
                pop() + decrementSP() + loadSP() + lt() + incrementSP()
            }
            "and" -> {
                pop() + decrementSP() + loadSP() + and() + incrementSP()
            }
            "or" -> {
                pop() + decrementSP() + loadSP() + or() + incrementSP()
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
            when (segment) {
                "constant" -> pushConst(index)
                "argument", "local", "this", "that" -> pushSegment(segment, index)
                "pointer", "temp" -> pushTempOrPointer(segment, index)
                "static" -> pushStatic(index)
                else -> throw IllegalArgumentException()
            }
        } else {
            when (segment) {
                "argument", "local", "this", "that", "pointer", "temp" -> popToSegment(segment, index)
                "static" -> popToStatic(index)
                else -> throw IllegalArgumentException()
            }
        }

        file.appendText(assemblyCode)
    }

    /**
     * VMの初期化を行うアセンブリコードをファイルに書き込む
     */
    fun writeInit() {
        var assemblyCode = "@256\nD=A\n@SP\nM=D\n"

        file.appendText(assemblyCode)
        writeCall("Sys.init", 0)
    }

    /**
     * labelコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeLabel(label: String) {
        val assemblyCode = "($fileName$$label)\n"
        file.appendText(assemblyCode)
    }

    /**
     * gotoコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeGoto(label: String) {
        val assemblyCode = "@$fileName$$label\n0;JMP\n"
        file.appendText(assemblyCode)
    }

    /**
     * if-gotoコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeIf(label: String) {
        val assemblyCode = pop() + "@$fileName$$label\nD;JNE\n"
        file.appendText(assemblyCode)
    }

    /**
     * functionコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeFunction(functionName: String, numLocals: Int) {
        var assemblyCode = "($functionName)\n"
        (0 until numLocals).forEach { _ ->
            assemblyCode += pushConst(0)
        }
        file.appendText(assemblyCode)
    }


    /**
     * callコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeCall(functionName: String, numArgs: Int) {
        var assemblyCode = ""

        // push return-address
        assemblyCode += "@return-address$returnAddressCounter\nD=A\n" + push()
        // push LCL
        assemblyCode += "@LCL\nD=M\n" + push()
        // push ARG
        assemblyCode += "@ARG\nD=M\n" + push()
        // push THIS
        assemblyCode += "@THIS\nD=M\n" + push()
        // push THAT
        assemblyCode += "@THAT\nD=M\n" + push()
        // ARG = SP-n-5
        assemblyCode += "@SP\nD=M\n@$numArgs\nD=D-A\n@5\nD=D-A\n@ARG\nM=D\n"
        // LCL = SP
        assemblyCode += "@SP\nD=M\n@LCL\nM=D\n"
        // goto f
        assemblyCode += "@$functionName\n0;JMP\n"
        // (return-address)
        assemblyCode += "(return-address$returnAddressCounter)\n"

        file.appendText(assemblyCode)

        returnAddressCounter++
    }

    /**
     * returnコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeReturn() {
        var assemblyCode = ""

        // FRAMEのベースアドレスをLCLから取得
        assemblyCode += "@LCL\nD=M\n@frame\nM=D\n"

        // リターンアドレスをLCLから取得
        assemblyCode += "@5\nD=A\n@frame\nA=M-D\nD=M\n@ret\nM=D\n"

        // 関数の戻り値を呼び出し元のスタックに格納(argument[0]は呼び出し元ではスタックの最上位になる)
        assemblyCode += pop() + "@ARG\nA=M\nM=D\n"

        // SPを呼び出し元のSPに戻す
        assemblyCode += "@ARG\nD=M\n@SP\nM=D+1\n"

        // THATを呼び出し元のTHATに戻す
        assemblyCode += "@frame\nA=M-1\nD=M\n@THAT\nM=D\n"

        // THISを呼び出し元のTHISに戻す
        assemblyCode += "@2\nD=A\n@frame\nA=M-D\nD=M\n@THIS\nM=D\n"

        // ARGを呼び出し元のARGに戻す
        assemblyCode += "@3\nD=A\n@frame\nA=M-D\nD=M\n@ARG\nM=D\n"

        // LCLを呼び出し元のLCLに戻す
        assemblyCode += "@4\nD=A\n@frame\nA=M-D\nD=M\n@LCL\nM=D\n"

        // リターンアドレスにjump
        assemblyCode += "@ret\nA=M\n0;JMP\n"

        file.appendText(assemblyCode)
    }

    /**
     * 出力ファイルを閉じる
     * KotlinのFile::appendText()メソッドの中で書き込みと同時にclose処理をおこなっているので今回は実装しない
     */
    fun close() {}

    private fun loadSP() = "@SP\nA=M\n"
    private fun loadArg() = "@ARG\nA=M+D\nD=M\n"
    private fun loadLcl() = "@LCL\nA=M+D\nD=M\n"
    private fun loadThis() = "@THIS\nA=M+D\nD=M\n"
    private fun loadThat() = "@THAT\nA=M+D\nD=M\n"
    private fun loadPointer(index: Int) = "@${3 + index}\nD=M\n"
    private fun loadTemp(index: Int) = "@${5 + index}\nD=M\n"
    private fun loadArgAddr() = "@ARG\nD=M+D\n"
    private fun loadLclAddr() = "@LCL\nD=M+D\n"
    private fun loadThisAddr() = "@THIS\nD=M+D\n"
    private fun loadThatAddr() = "@THAT\nD=M+D\n"
    private fun loadPointerAddr() = "@3\nD=A+D\n"
    private fun loadTempAddr() = "@5\nD=A+D\n"
    private fun loadRegister(register: String) = "@$register\nA=M\n"

    private fun setToRegister(register: String) = "@${register}\nM=D\n"

    private fun incrementSP() = "@SP\nM=M+1\n"
    private fun decrementSP() = "@SP\nM=M-1\n"

    private fun defIndex(index: Int) = "@$index\nD=A\n"

    private fun popToSegment(segment: String, index: Int): String {
        val command = when (segment) {
            "argument" -> loadArgAddr()
            "local" -> loadLclAddr()
            "this" -> loadThisAddr()
            "that" -> loadThatAddr()
            "pointer" -> loadPointerAddr()
            "temp" -> loadTempAddr()
            else -> IllegalArgumentException()
        }
        return defIndex(index) + command + setToRegister("R13") + pop() + loadRegister("R13") + "M=D\n"
    }

    private fun popToStatic(index: Int) = pop() + "@$fileName.$index\nM=D\n"

    private fun pop() = decrementSP() + loadSP() + "D=M\nM=0\n"

    private fun pushConst(const: Int) = defIndex(const) + push()

    private fun pushSegment(segment: String, index: Int): String {
        val command = when (segment) {
            "argument" -> loadArg()
            "local" -> loadLcl()
            "this" -> loadThis()
            "that" -> loadThat()
            else -> throw IllegalArgumentException()
        }

        return defIndex(index) + command + push()
    }

    private fun pushTempOrPointer(segment: String, index: Int): String {
        val command = when (segment) {
            "pointer" -> loadPointer(index)
            "temp" -> loadTemp(index)
            else -> throw IllegalArgumentException()
        }

        return command + push()
    }

    private fun pushStatic(index: Int) = "@$fileName.$index\nD=M\n" + push()

    private fun push() = loadSP() + "M=D\n" + incrementSP()

    private fun add() = "M=M+D\n"
    private fun sub() = "M=M-D\n"
    private fun neg() = "M=-M\n"
    private fun and() = "M=D&M\n"
    private fun or() = "M=D|M\n"
    private fun not() = "M=!M\n"
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
}
