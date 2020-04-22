package compiler.generator.vmwriter

import compiler.symboltable.Kind

enum class Segment(val value: String) {

    CONST("constant"),
    ARG("argument"),
    LOCAL("local"),
    STATIC("static"),
    THIS("this"),
    THAT("that"),
    POINTER("pointer"),
    TEMP("temp");

    companion object {
        fun find(kind: Kind): Segment {
            return when (kind) {
                Kind.VAR -> LOCAL
                Kind.ARG -> ARG
                Kind.STATIC -> STATIC
                Kind.FIELD -> THIS
                else -> throw IllegalStateException()
            }
        }
    }
}
