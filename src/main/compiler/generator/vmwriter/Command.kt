package compiler.generator.vmwriter

enum class Command(val ope: String, val unary: Boolean) {

    ADD("+", false),
    SUB("-", false),
    NEG("-", true),
    EQ("=", false),
    GT(">", false),
    LT("<", false),
    AND("&", false),
    OR("|", false),
    NOT("~", true);

    companion object {
        fun find(ope: String, unary: Boolean): Command {
            return values().find { it.ope == ope && it.unary == unary } ?: throw IllegalArgumentException()
        }
    }
}
