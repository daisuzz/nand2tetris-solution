package compiler.generator.vmwriter

import compiler.parser.writeWithLF
import java.io.File

class VMWriter(syntaxTree: File) {

    private var outputFile: File = File(syntaxTree.parent + "/" + syntaxTree.nameWithoutExtension + ".vm")

    init {
        if (outputFile.exists()) outputFile.delete()
    }

    fun writePush(segment: Segment, index: Int) {
        outputFile.writeWithLF("push ${segment.value} $index")
    }

    fun writePop(segment: Segment, index: Int) {
        outputFile.writeWithLF("pop ${segment.value} $index")
    }

    fun writeArithmetic(command: Command) {
        outputFile.writeWithLF("${command.name.toLowerCase()}")
    }

    fun writeLabel(label: String) {
        outputFile.writeWithLF("label $label")
    }

    fun writeGoto(label: String) {
        outputFile.writeWithLF("goto $label")
    }

    fun writeIf(label: String) {
        outputFile.writeWithLF("if-goto $label")
    }

    fun writeCall(name: String, nArgs: Int) {
        outputFile.writeWithLF("call $name $nArgs")
    }

    fun writeFunction(name: String, nLocals: Int) {
        outputFile.writeWithLF("function $name $nLocals")
    }

    fun writeReturn() {
        outputFile.writeWithLF("return")
    }

    fun close() {

    }
}
