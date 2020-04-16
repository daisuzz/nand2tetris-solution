package compiler

enum class TagName(val value: String) {

    KEYWORD("keyword"),
    SYMBOL("symbol"),
    INTEGER_CONSTANT("integerConstant"),
    STRING_CONSTANT("stringConstant"),
    IDENTIFIER("identifier"),
    CLASS("class"),
    CLASS_VAR_DEC("classVarDec"),
    TYPE("type"),
    SUBROUTINE_DEC("subroutineDec"),
    PARAMETER_LIST("parameterList"),
    SUBROUTINE_BODY("subroutineBody"),
    VAR_DEC("varDec"),
    CLASS_NAME("className"),
    SUBROUTINE_NAME("subroutineName"),
    VAR_NAME("varName"),
    STATEMENTS("statements"),
    STATEMENT("statement"),
    LET_STATEMENT("letStatement"),
    IF_STATEMENT("ifStatement"),
    WHILE_STATEMENT("whileStatement"),
    DO_STATEMENT("doStatement"),
    RETURN_STATEMENT("returnStatement"),
    EXPRESSION("expression"),
    TERM("term"),
    SUBROUTINE_CALL("subroutineCall"),
    EXPRESSION_LIST("expressionList"),
    OP("op"),
    UNARY_OP("unaryOp"),
    KEYWORD_CONSTANT("keywordConstant");

}
