package compiler.generator

import java.io.File

fun File.writeWithLF(text: String) {
    this.appendText("$text\n")
}
