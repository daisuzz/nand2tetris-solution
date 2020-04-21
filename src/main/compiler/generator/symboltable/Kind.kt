package compiler.symboltable

enum class Kind {

    STATIC,
    FIELD,
    ARG,
    VAR,
    NONE;

    companion object {
        fun find(keyword: String): Kind {
            return values().find { it.name.toLowerCase() == keyword } ?: throw IllegalArgumentException()
        }
    }
}
