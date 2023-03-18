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
    fun writeArithmetic(operator: String) {
        val commands = when (operator) {
            "add" -> "M=M+D"
            "sub" -> "M=M-D"
            "neg" -> "M=-M"
            "and" -> "M=M&D"
            "or" -> "M=M|D"
            "not" -> "M=!M"
            "eq" -> eq()
            "gt" -> gt()
            "lt" -> lt()
            else -> throw IllegalArgumentException()
        }

        val assemblyCode =
            if (commands == "neg" || commands == "not") genLine(decrementSP(), loadSP(), operator, incrementSP())
            else genLine(pop(), decrementSP(), loadSP(), operator, incrementSP())

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
                "static" -> genLine("@$fileName.$index", "D=M", push())
                else -> throw IllegalArgumentException()
            }
        } else {
            when (segment) {
                "argument", "local", "this", "that", "pointer", "temp" -> popToSegment(segment, index)
                "static" -> genLine(pop(), "@$fileName.$index", "M=D")
                else -> throw IllegalArgumentException()
            }
        }

        file.appendText(assemblyCode)
    }

    /**
     * VMの初期化を行うアセンブリコードをファイルに書き込む
     */
    fun writeInit() {
        val assemblyCode = genLine("@256", "D=A", "@SP", "M=D")

        file.appendText(assemblyCode)
        writeCall("Sys.init", 0)
    }

    /**
     * labelコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeLabel(label: String) {
        val assemblyCode = genLine("($fileName$$label)")
        file.appendText(assemblyCode)
    }

    /**
     * gotoコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeGoto(label: String) {
        val assemblyCode = genLine("@$fileName$$label", "0;JMP")
        file.appendText(assemblyCode)
    }

    /**
     * if-gotoコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeIf(label: String) {
        val assemblyCode = genLine(pop(), "@$fileName$$label", "D;JNE")
        file.appendText(assemblyCode)
    }

    /**
     * functionコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeFunction(functionName: String, numLocals: Int) {
        var assemblyCode = genLine("($functionName)")
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
        assemblyCode += genLine("@return-address$returnAddressCounter", "D=A", push())
        // push LCL
        assemblyCode += genLine("@LCL", "D=M", push())
        // push ARG
        assemblyCode += genLine("@ARG", "D=M", push())
        // push THIS
        assemblyCode += genLine("@THIS", "D=M", push())
        // push THAT
        assemblyCode += genLine("@THAT", "D=M", push())
        // ARG = SP-n-5
        assemblyCode += genLine("@SP", "D=M", "@$numArgs", "D=D-A", "@5", "D=D-A", "@ARG", "M=D")
        // LCL = SP
        assemblyCode += genLine("@SP", "D=M", "@LCL", "M=D")
        // goto f
        assemblyCode += genLine("@$functionName", "0;JMP")
        // (return-address)
        assemblyCode += genLine("(return-address$returnAddressCounter)")

        file.appendText(assemblyCode)

        returnAddressCounter++
    }

    /**
     * returnコマンドをアセンブリコードに変換し、ファイルに書き込む
     */
    fun writeReturn() {
        var assemblyCode = ""

        // FRAMEのベースアドレスをLCLから取得
        assemblyCode += genLine("@LCL", "D=M", "@frame", "M=D")

        // リターンアドレスをLCLから取得
        assemblyCode += genLine("@5", "D=A", "@frame", "A=M-D", "D=M", "@ret", "M=D")

        // 関数の戻り値を呼び出し元のスタックに格納(argument[0]は呼び出し元ではスタックの最上位になる)
        assemblyCode += genLine(pop() + "@ARG", "A=M", "M=D")

        // SPを呼び出し元のSPに戻す
        assemblyCode += genLine("@ARG", "D=M", "@SP", "M=D+1")

        // THATを呼び出し元のTHATに戻す
        assemblyCode += genLine("@frame", "A=M-1", "D=M", "@THAT", "M=D")

        // THISを呼び出し元のTHISに戻す
        assemblyCode += genLine("@2", "D=A", "@frame", "A=M-D", "D=M", "@THIS", "M=D")

        // ARGを呼び出し元のARGに戻す
        assemblyCode += genLine("@3", "D=A", "@frame", "A=M-D", "D=M", "@ARG", "M=D")

        // LCLを呼び出し元のLCLに戻す
        assemblyCode += genLine("@4", "D=A", "@frame", "A=M-D", "D=M", "@LCL", "M=D")

        // リターンアドレスにjump
        assemblyCode += genLine("@ret", "A=M", "0;JMP")

        file.appendText(assemblyCode)
    }

    /**
     * 出力ファイルを閉じる
     * KotlinのFile::appendText()メソッドの中で書き込みと同時にclose処理をおこなっているので今回は実装しない
     */
    fun close() {}

    private fun loadSP() = genLine("@SP", "A=M")
    private fun incrementSP() = genLine("@SP", "M=M+1")
    private fun decrementSP() = genLine("@SP", "M=M-1")
    private fun pop() = genLine(decrementSP(), loadSP(), "D=M", "M=0")
    private fun push() = genLine(loadSP(), "M=D", incrementSP())

    private fun popToSegment(segment: String, index: Int): String {
        val command = when (segment) {
            "argument" -> genLine("@ARG", "D=M+D")
            "local" -> genLine("@LCL", "D=M+D")
            "this" -> genLine("@THIS", "D=M+D")
            "that" -> genLine("@THAT", "D=M+D")
            "pointer" -> genLine("@3", "D=A+D")
            "temp" -> genLine("@5", "D=A+D")
            else -> throw IllegalArgumentException()
        }
        return genLine("@$index", "D=A", command, "@R13", "M=D", "", pop(), "@R13", "A=M", "", "M=D")

    }

    private fun pushConst(const: Int) = genLine("@$const", "D=A") + push()
    private fun pushSegment(segment: String, index: Int): String {
        val command = when (segment) {
            "argument" -> genLine("@ARG", "A=M+D", "D=M")
            "local" -> genLine("@LCL", "A=M+D", "D=M")
            "this" -> genLine("@THIS", "A=M+D", "D=M")
            "that" -> genLine("@THAT", "A=M+D", "D=M")
            else -> throw IllegalArgumentException()
        }

        return genLine("@$index", "D=A", command, push())
    }

    private fun pushTempOrPointer(segment: String, index: Int): String {
        val command = when (segment) {
            "pointer" -> genLine("@${3 + index}", "D=M")
            "temp" -> genLine("@${5 + index}", "D=M")
            else -> throw IllegalArgumentException()
        }

        return command + push()
    }

    private fun eq(): String {
        val command = genLine(
            "D=M-D",
            "@EQ$eqNum",
            "D;JEQ",
            "@NEQ$eqNum",
            "0;JMP",
            "(EQ$eqNum)",
            loadSP(),
            "M=-1",
            "@EQNEXT$eqNum",
            "0;JMP",
            "(NEQ$eqNum)",
            loadSP(),
            "M=0",
            "@EQNEXT$eqNum",
            "0;JMP",
            "(EQNEXT$eqNum)"
        )
        eqNum++
        return command
    }

    private fun gt(): String {
        val command = genLine(
            "D=M-D",
            "@GT$gtNum",
            "D;JGT",
            "@NGT$gtNum",
            "0;JMP",
            "(GT$gtNum)",
            loadSP(),
            "M=-1",
            "@GTNEXT$gtNum",
            "0;JMP",
            "(NGT$gtNum)",
            loadSP(),
            "M=0",
            "@GTNEXT$gtNum",
            "0;JMP",
            "(GTNEXT$gtNum)"
        )
        gtNum++
        return command
    }

    private fun lt(): String {
        val command = genLine(
            "D=M-D",
            "@LT$ltNum",
            "D;JLT",
            "@NLT$ltNum",
            "0;JMP",
            "(LT$ltNum)",
            loadSP(),
            "M=-1",
            "@LTNEXT$ltNum",
            "0;JMP",
            "(NLT$ltNum)",
            loadSP(),
            "M=0",
            "@LTNEXT$ltNum",
            "0;JMP",
            "(LTNEXT$ltNum)"
        )
        ltNum++
        return command
    }

    private fun genLine(vararg commands: String): String =
        commands.joinToString(separator = "\n", postfix = "\n") { it.replace("(\\n)+$".toRegex(), "") }
}
