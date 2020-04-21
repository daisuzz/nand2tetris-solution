package compiler.generator.vmwriter

enum class Segment(val value: String) {

    CONST("constant"),
    ARG("argument"),
    LOCAL("local"),
    STATIC("static"),
    THIS("this"),
    THAT("that"),
    POINTER("pointer"),
    TEMP("temp")
}
