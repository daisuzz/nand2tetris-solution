package compiler.generator

import compiler.generator.vmwriter.Command
import compiler.generator.vmwriter.Segment
import compiler.generator.vmwriter.VMWriter
import compiler.symboltable.Kind
import compiler.symboltable.SymbolTable
import java.io.File

class CompilationEngine(inputFile: File, private val outputFile: File) {

    private val tokenizer = JackTokenizer(inputFile)

    private val symbolTable = SymbolTable()

    private val vmWriter = VMWriter(outputFile)

    private val opeSet: Set<String> = setOf("+", "-", "*", "/", "&", "|", "<", ">", "=")

    private val statementSet: Set<String> = setOf("let", "if", "while", "do", "return")

    private lateinit var className: String

    private lateinit var subroutineName: String

    private var nArgs = 0

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
                    className = tokenizer.identifier()
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", class"))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.CLASS.value))
    }

    private fun compileClassVarDec() {
        val kind = tokenizer.keyword()
        outputFile.writeWithLF(genStartTag(TagName.CLASS_VAR_DEC.value))
        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, kind))

        val type: String
        tokenizer.advance()
        when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> {
                type = tokenizer.keyword()
                outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
            }
            TokenType.IDENTIFIER -> {
                type = tokenizer.identifier()
                outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", class"))
            }
            else -> throw IllegalArgumentException()
        }

        tokenizer.advance()
        symbolTable.define(tokenizer.identifier(), type, Kind.find(kind))
        outputFile.writeWithLF(
            wrapWithTag(
                TagName.IDENTIFIER,
                tokenizer.identifier() + ", $kind, defined, $kind, ${symbolTable.indexOf(tokenizer.identifier())}"
            )
        )

        loop@ while (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        ',' -> outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        ';' -> {
                            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, symbol.toString()))
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.IDENTIFIER -> {
                    symbolTable.define(tokenizer.identifier(), type, Kind.find(kind))
                    outputFile.writeWithLF(
                        wrapWithTag(
                            TagName.IDENTIFIER,
                            tokenizer.identifier() + ", $kind, defined, $kind, ${symbolTable.indexOf(tokenizer.identifier())}"
                        )
                    )
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.CLASS_VAR_DEC.value))
    }

    private fun compileSubroutine() {
        symbolTable.startSubroutine()
        var nLocals = 0

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
                            vmWriter.writeFunction("$className.$subroutineName", symbolTable.varCount(Kind.VAR))
                            compileStatements()
                        }
                        "var" -> {
                            compileVarDec()
                        }
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
                    val category = if (tokenizer.nextToken() == "(") {
                        subroutineName = tokenizer.identifier()
                        "subroutine"
                    } else "class"
                    outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", $category"))
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.SUBROUTINE_DEC.value))
    }

    private fun compileParameterList() {
        outputFile.writeWithLF(genStartTag(TagName.PARAMETER_LIST.value))
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            outputFile.writeWithLF(genEndTag(TagName.PARAMETER_LIST.value))
            return
        }

        var type = ""
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            type = tokenizer.identifier()
            outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
        }
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            type = tokenizer.keyword()
            outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, tokenizer.keyword()))
        }
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "int", "char", "boolean" -> {
                            type = keyword
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
                    if (tokenizer.nextToken() == "," || tokenizer.nextToken() == ")") {
                        symbolTable.define(tokenizer.identifier(), type, Kind.ARG)
                        outputFile.writeWithLF(
                            wrapWithTag(
                                TagName.IDENTIFIER,
                                tokenizer.identifier() + ", argument, used, ${symbolTable.kindOf(tokenizer.identifier())}, ${symbolTable.indexOf(
                                    tokenizer.identifier()
                                )}"
                            )
                        )
                    } else {
                        type = tokenizer.identifier()
                        outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, "$type, class"))
                    }
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
            var type = ""
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "int", "char", "boolean" -> {
                            type = tokenizer.keyword()
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
                    if (tokenizer.nextToken() == ";" || tokenizer.nextToken() == ",") {
                        symbolTable.define(tokenizer.identifier(), type, Kind.VAR)
                        val index = symbolTable.indexOf(tokenizer.identifier())
                        outputFile.writeWithLF(
                            wrapWithTag(
                                TagName.IDENTIFIER,
                                tokenizer.identifier() + ", var, defined, var, $index"
                            )
                        )
                    } else {
                        type = tokenizer.identifier()
                        outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier()))
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.VAR_DEC.value))
    }

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
                    outputFile.writeWithLF(
                        wrapWithTag(
                            TagName.IDENTIFIER,
                            tokenizer.identifier() + ", ${symbolTable.kindOf(tokenizer.identifier())}, used, ${symbolTable.kindOf(
                                tokenizer.identifier()
                            )}, ${symbolTable.indexOf(tokenizer.identifier())}"
                        )
                    )
                }
                else -> throw IllegalArgumentException()
            }
        }

        outputFile.writeWithLF(genEndTag(TagName.LET_STATEMENT.value))
    }

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

    private fun compileDo() {
        var callSubroutineName = ""
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        ';' -> {
                            break@loop
                        }
                    }
                }
                TokenType.IDENTIFIER -> {
                    callSubroutineName += tokenizer.identifier()
                    if (symbolTable.kindOf(tokenizer.identifier()) == Kind.NONE) {
                        val category = if (tokenizer.nextToken() == "(") "subroutine" else "class"
                        outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", $category"))
                    } else {
                        outputFile.writeWithLF(
                            wrapWithTag(
                                TagName.IDENTIFIER,
                                tokenizer.identifier() + ", var, used, ${symbolTable.kindOf(tokenizer.identifier())}, ${symbolTable.indexOf(
                                    tokenizer.identifier()
                                )}"
                            )
                        )
                    }
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
                                    tokenizer.advance()
                                    callSubroutineName += ".${tokenizer.identifier()}"
                                    tokenizer.advance()
                                    compileExpressionList()
                                    tokenizer.advance()
                                }
                                else -> throw IllegalArgumentException()
                            }
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        vmWriter.writeCall(callSubroutineName, nArgs)
        vmWriter.writePop(Segment.TEMP, 0)
    }

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
        }else{
            vmWriter.writePush(Segment.CONST,0)
        }
        vmWriter.writeReturn()
        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
        outputFile.writeWithLF(genEndTag(TagName.RETURN_STATEMENT.value))
    }

    private fun compileExpression() {
        outputFile.writeWithLF(genStartTag(TagName.EXPRESSION.value))
        compileTerm()
        loop@ while (tokenizer.hasMoreTokens()) {
            if (!opeSet.contains(tokenizer.nextToken())) break@loop
            tokenizer.advance()
            val ope = sanitize(tokenizer.symbol().toString())
            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, sanitize(tokenizer.symbol().toString())))
            tokenizer.advance()
            compileTerm()
            if (ope == "*" || ope == "/") {
                val command = if (ope == "*") "Math.multiply" else "Math.divide"
                vmWriter.writeCall(command, 2)
            } else {
                vmWriter.writeArithmetic(Command.find(ope, unary = false))
            }
        }
        outputFile.writeWithLF(genEndTag(TagName.EXPRESSION.value))
    }

    private fun compileExpressionList() {
        nArgs = 0
        outputFile.writeWithLF(genStartTag(TagName.EXPRESSION_LIST.value))
        if (tokenizer.nextToken() == ")" || tokenizer.nextToken() == "]") {
            outputFile.writeWithLF(genEndTag(TagName.EXPRESSION_LIST.value))
            return
        }
        tokenizer.advance()
        nArgs++
        compileExpression()
        loop@ while (tokenizer.hasMoreTokens()) {
            if (tokenizer.nextToken() != ",") break@loop
            tokenizer.advance()
            outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
            tokenizer.advance()
            nArgs++
            compileExpression()
        }
        outputFile.writeWithLF(genEndTag(TagName.EXPRESSION_LIST.value))
    }

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
                when (tokenizer.nextToken()) {
                    "[" -> {
                        outputFile.writeWithLF(
                            wrapWithTag(
                                TagName.IDENTIFIER,
                                tokenizer.identifier() + ", ${symbolTable.kindOf(tokenizer.identifier())}, used, ${symbolTable.kindOf(
                                    tokenizer.identifier()
                                )}, ${symbolTable.indexOf(tokenizer.identifier())}"
                            )
                        )
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        tokenizer.advance()
                        compileExpression()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                    "(" -> {
                        outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", subroutine"))
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        compileExpressionList()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                    "." -> {
                        if (symbolTable.kindOf(tokenizer.identifier()) == Kind.NONE) {
                            outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", class"))
                        } else {
                            outputFile.writeWithLF(
                                wrapWithTag(
                                    TagName.IDENTIFIER,
                                    tokenizer.identifier() + ", ${symbolTable.kindOf(tokenizer.identifier())}, used, ${symbolTable.kindOf(
                                        tokenizer.identifier()
                                    )}, ${symbolTable.indexOf(tokenizer.identifier())}"
                                )
                            )
                        }
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.IDENTIFIER, tokenizer.identifier() + ", subroutine"))
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                        compileExpressionList()
                        tokenizer.advance()
                        outputFile.writeWithLF(wrapWithTag(TagName.SYMBOL, tokenizer.symbol().toString()))
                    }
                }
            }
            TokenType.INT_CONST -> {
                vmWriter.writePush(Segment.CONST, tokenizer.intVal())
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
