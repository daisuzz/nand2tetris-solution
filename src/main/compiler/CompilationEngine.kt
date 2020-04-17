package compiler

import java.io.File

class CompilationEngine(val tokenizer: JackTokenizer, val outputFile: File) {

    private val opeSet: Set<String> = setOf("+", "-", "*", "/", "&", "|", "<", ">", "=")

    private val statementSet: Set<String> = setOf("let", "if", "while", "do", "return")

    fun compileClass() {
        outputFile.writeWithLF(genStartTag(TagName.CLASS.value))

        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "class" -> outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                        "static", "field" -> compileClassVarDec()
                        "constructor", "function", "method" -> compileSubroutine()
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '{' -> outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        '}' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            break@loop
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.CLASS.value))
    }

    private fun compileClassVarDec() {
        outputFile.writeWithLF(genStartTag(TagName.CLASS_VAR_DEC.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "int", "char", "boolean" -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        ',' -> outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        ';' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            break@loop
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.CLASS_VAR_DEC.value))
    }

    private fun compileSubroutine() {
        outputFile.writeWithLF(genStartTag(TagName.SUBROUTINE_DEC.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "void", "int", "char", "boolean" -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                        }
                        in statementSet -> {
                            compileStatements()
                        }
                        "var" -> compileVarDec()
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '(' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileParameterList()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        }
                        '{' -> {
                            outputFile.writeWithLF(genStartTag(TagName.SUBROUTINE_BODY.value))
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        }
                        '}' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            outputFile.writeWithLF(genEndTag(TagName.SUBROUTINE_BODY.value))
                            break@loop
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.SUBROUTINE_DEC.value))
    }

    /**
     * init -> type or )
     * end -> )
     */
    private fun compileParameterList() {
        outputFile.writeWithLF(genStartTag(TagName.PARAMETER_LIST.value))
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            outputFile.writeWithLF(genEndTag(TagName.PARAMETER_LIST.value))
            return
        }
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "int", "char", "boolean" -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        ',' -> outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        ')' -> break@loop
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                }
                else -> throw IllegalArgumentException()
            }
        }
        outputFile.writeWithLF(genEndTag(TagName.PARAMETER_LIST.value))
    }

    private fun compileVarDec() {
        outputFile.writeWithLF(genStartTag(TagName.VAR_DEC.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "int", "char", "boolean" -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                        }
                    }
                }
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        ';' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            break@loop
                        }
                        ',' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        }
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.VAR_DEC.value))
    }

    /**
     * init -> let, if, while, do, return
     * end -> ;
     */
    private fun compileStatements() {
        outputFile.writeWithLF(genStartTag(TagName.STATEMENTS.value))
        loop@ while (tokenizer.hasMoreTokens()) {
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (tokenizer.keyword()) {
                        "let" -> compileLet()
                        "if" -> compileIf()
                        "while" -> compileWhile()
                        "do" -> compileDo()
                        "return" -> {
                            compileReturn()
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                else -> throw IllegalArgumentException()
            }
            if (!statementSet.contains(tokenizer.nextToken())) break@loop
            tokenizer.advance()
        }
        outputFile.writeWithLF(genEndTag(TagName.STATEMENTS.value))
    }

    /**
     * init -> let
     * end -> ;
     */
    private fun compileLet() {
        outputFile.writeWithLF(genStartTag(TagName.LET_STATEMENT.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '[' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        }
                        '=' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                            break@loop
                        }
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.LET_STATEMENT.value))
    }

    /**
     * init -> if
     * end -> }
     */
    private fun compileIf() {
        outputFile.writeWithLF(genStartTag(TagName.IF_STATEMENT.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '(' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        }
                        '{' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileStatements()
                            tokenizer.advance()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                            break@loop
                        }
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        if (tokenizer.nextToken() == "else") {
            tokenizer.advance()
            outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
            tokenizer.advance()
            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
            tokenizer.advance()
            compileStatements()
            tokenizer.advance()
            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
        }
        outputFile.writeWithLF(genEndTag(TagName.IF_STATEMENT.value))
    }

    /**
     * init -> do
     * end -> ;
     */
    private fun compileDo() {
        outputFile.writeWithLF(genStartTag(TagName.DO_STATEMENT.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        ';' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            break@loop
                        }
                    }
                }
                TokenType.IDENTIFIER -> {
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                    tokenizer.advance()
                    when (tokenizer.tokenType()) {
                        TokenType.SYMBOL -> {
                            when (val symbol = tokenizer.symbol()) {
                                '(' -> {
                                    outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                                    compileExpressionList()
                                    tokenizer.advance()
                                    outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                                }
                                '.' -> {
                                    outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                                    tokenizer.advance()
                                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                                    tokenizer.advance()
                                    outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                                    compileExpressionList()
                                    tokenizer.advance()
                                    outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                                }
                                else -> {
                                    outputFile.writeWithLF(genEndTag(TagName.DO_STATEMENT.value))
                                    outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                                    return
                                }
                            }
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.DO_STATEMENT.value))
    }

    /**
     * init -> while
     * end -> }
     */
    private fun compileWhile() {
        outputFile.writeWithLF(genStartTag(TagName.WHILE_STATEMENT.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '(' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        }
                        '{' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                            tokenizer.advance()
                            compileStatements()
                            tokenizer.advance()
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                            break@loop
                        }
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.WHILE_STATEMENT.value))
    }

    /**
     * init -> return
     * end -> ;
     */
    private fun compileReturn() {
        outputFile.writeWithLF(genStartTag(TagName.RETURN_STATEMENT.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        tokenizer.advance()
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            compileExpression()
            tokenizer.advance()
        }
        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
        outputFile.writeWithLF(genEndTag(TagName.RETURN_STATEMENT.value))
    }

    /**
     * init -> termの先頭
     * end -> termの最後
     */
    private fun compileExpression() {
        outputFile.writeWithLF(genStartTag(TagName.EXPRESSION.value))
        compileTerm()
        loop@ while (tokenizer.hasMoreTokens()) {
            if (!opeSet.contains(tokenizer.nextToken())) break@loop
            tokenizer.advance()
            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, sanitize(tokenizer.symbol().toString())))
            tokenizer.advance()
            compileTerm()
        }
        outputFile.writeWithLF(genEndTag(TagName.EXPRESSION.value))
    }

    /**
     * init -> (
     * end -> expressionの最後
     */
    private fun compileExpressionList() {
        outputFile.writeWithLF(genStartTag(TagName.EXPRESSION_LIST.value))
        if (tokenizer.nextToken() == ")" || tokenizer.nextToken() == "]") {
            outputFile.writeWithLF(genEndTag(TagName.EXPRESSION_LIST.value))
            return
        }
        tokenizer.advance()
        compileExpression()
        loop@ while (tokenizer.hasMoreTokens()) {
            if (tokenizer.nextToken() != ",") break@loop
            tokenizer.advance()
            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
            tokenizer.advance()
            compileExpression()
        }
        outputFile.writeWithLF(genEndTag(TagName.EXPRESSION_LIST.value))
    }

    /**
     * init -> termの先頭
     * end -> termの最後
     */
    private fun compileTerm() {
        outputFile.writeWithLF(genStartTag(TagName.TERM.value))
        when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> {
                when (val keyword = tokenizer.keyword()) {
                    "true", "false", "null", "this" -> {
                        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                    }
                    else -> throw IllegalArgumentException()
                }
            }
            TokenType.SYMBOL -> {
                when (val symbol = tokenizer.symbol()) {
                    '-', '~' -> {
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        tokenizer.advance()
                        compileTerm()
                    }
                    '(' -> {
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        tokenizer.advance()
                        compileExpression()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                }
            }
            TokenType.IDENTIFIER -> {
                outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                when (tokenizer.nextToken()) {
                    "[" -> {
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        tokenizer.advance()
                        compileExpression()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                    "(" -> {
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        compileExpressionList()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                    "." -> {
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        compileExpressionList()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                }
            }
            TokenType.INT_CONST -> {
                outputFile.writeWithLF(wrapWithTag(TagName.INTEGER_CONSTANT, tokenizer.intVal().toString()))
            }
            TokenType.STRING_CONST -> {
                outputFile.writeWithLF(wrapWithTag(TagName.STRING_CONSTANT, tokenizer.stringVal()))
            }
        }
        outputFile.writeWithLF(genEndTag(TagName.TERM.value))
    }

    private fun genStartTag(str: String) = "<$str>"
    private fun genEndTag(str: String) = "</$str>"
    private fun wrapWithTag(tagName: TagName, value: String): String {
        return genStartTag(tagName.value) + value + genEndTag(tagName.value)
    }

    private fun sanitize(symbol: String): String {
        return when (symbol) {
            "<" -> "&lt;"
            ">" -> "&gt;"
            "&" -> "&amp;"
            else -> symbol
        }
    }
}
