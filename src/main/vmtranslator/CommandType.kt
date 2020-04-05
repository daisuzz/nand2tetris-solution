package vmtranslator

enum class CommandType {
    C_ARITHMETIC,
    C_PUSH,
    C_POP,
    C_LABEL,
    C_GOTO,
    C_IF,
    C_FUNCTION,
    C_RETURN,
    C_CALL;

    fun hasArg2(): Boolean {
        return this == C_PUSH || this == C_POP || this == C_FUNCTION || this == C_CALL
    }
}
