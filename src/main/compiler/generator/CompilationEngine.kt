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

    private var callSubroutineName = ""

    private var currentSubroutineKind = ""

    private var nArgs = 0

    private var whileLabel = 0

    private var ifLabel = 0

    fun compileClass() {
        outputFile.writeWithLF(genStartTag(TagName.CLASS.value))

        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (tokenizer.keyword()) {
                        "static", "field" -> compileClassVarDec()
                        "constructor", "function", "method" -> compileSubroutine()
                    }
                }
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '{' -> {
                        }
                        '}' -> {
                            break@loop
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                TokenType.IDENTIFIER -> {
                    className = tokenizer.identifier()
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun compileClassVarDec() {
        // field, static
        val kind = tokenizer.keyword()
        tokenizer.advance()
        // type
        val type = when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> tokenizer.keyword()
            TokenType.IDENTIFIER -> tokenizer.identifier()
            else -> throw IllegalArgumentException()
        }
        tokenizer.advance()
        // fieldName
        symbolTable.define(tokenizer.identifier(), type, Kind.find(kind))

        loop@ while (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            tokenizer.advance()
            if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
                symbolTable.define(tokenizer.identifier(), type, Kind.find(kind))
            }
        }
    }

    private fun compileSubroutine() {
        symbolTable.startSubroutine()
        currentSubroutineKind = tokenizer.keyword()
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
                            if (currentSubroutineKind == "constructor") {
                                vmWriter.writePush(Segment.CONST, symbolTable.varCount(Kind.FIELD))
                                vmWriter.writeCall("Memory.alloc", 1)
                                vmWriter.writePop(Segment.POINTER, 0)
                            }
                            if (currentSubroutineKind == "method") {
                                vmWriter.writePush(Segment.ARG, 0)
                                vmWriter.writePop(Segment.POINTER, 0)
                            }
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
                            tokenizer.advance()
                            compileParameterList()
                        }
                        '}' -> {
                            break@loop
                        }
                    }
                }
                TokenType.IDENTIFIER -> {
                    if (tokenizer.nextToken() == "(") {
                        subroutineName = tokenizer.identifier()
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun compileParameterList() {
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return
        }
        var type = when (tokenizer.tokenType()) {
            TokenType.IDENTIFIER -> tokenizer.identifier()
            TokenType.KEYWORD -> tokenizer.keyword()
            else -> ""
        }
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.KEYWORD -> {
                    when (val keyword = tokenizer.keyword()) {
                        "int", "char", "boolean" -> {
                            type = keyword
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
                    } else {
                        type = tokenizer.identifier()
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }
        outputFile.writeWithLF(genEndTag(TagName.PARAMETER_LIST.value))
    }

    private fun compileVarDec() {
        // var
        tokenizer.advance()
        // type
        val type = when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> tokenizer.keyword()
            TokenType.IDENTIFIER -> tokenizer.identifier()
            else -> throw IllegalStateException()
        }
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (tokenizer.symbol()) {
                        ';' -> break@loop
                    }
                }
                TokenType.IDENTIFIER -> symbolTable.define(tokenizer.identifier(), type, Kind.VAR)
                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun compileStatements() {
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
    }

    private fun compileLet() {
        var isArrayAccess = false
        nArgs = 0
        tokenizer.advance()
        val varName = tokenizer.identifier()
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (tokenizer.symbol()) {
                        '[' -> {
                            isArrayAccess = true
                            vmWriter.writePush(Segment.find(symbolTable.kindOf(varName)), symbolTable.indexOf(varName))
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            vmWriter.writeArithmetic(Command.ADD)
                            vmWriter.writePop(Segment.POINTER, 1)
                        }
                        '=' -> {
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            break@loop
                        }
                    }
                }
            }
        }
        if (isArrayAccess) {
            vmWriter.writePop(Segment.THAT, 0)
            return
        }
        val segment = Segment.find(symbolTable.kindOf(varName))
        vmWriter.writePop(segment, symbolTable.indexOf(varName))
    }

    private fun compileIf() {
        nArgs = 0
        val branchLabel = "if.else.$subroutineName.$className$ifLabel"
        val exitLabel = "if.exit.$subroutineName.$className$ifLabel"
        ifLabel++
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (tokenizer.symbol()) {
                        '(' -> {
                            // (
                            tokenizer.advance()
                            compileExpression()
                            // )
                            tokenizer.advance()
                            vmWriter.writeArithmetic(Command.NOT)
                            vmWriter.writeIf(branchLabel)
                        }
                        '{' -> {
                            // {
                            tokenizer.advance()
                            compileStatements()
                            // }
                            tokenizer.advance()
                            vmWriter.writeGoto(exitLabel)
                            break@loop
                        }
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        vmWriter.writeLabel(branchLabel)
        if (tokenizer.nextToken() == "else") {
            tokenizer.advance()
            // else
            tokenizer.advance()
            // {
            tokenizer.advance()
            compileStatements()
            tokenizer.advance()
            // }
        }
        vmWriter.writeLabel(exitLabel)
    }

    private fun compileDo() {
        nArgs = 0
        callSubroutineName = ""
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
                    callSubroutineName = ""
                    callSubroutineName += if (symbolTable.kindOf(tokenizer.identifier()) == Kind.NONE) {
                        if (tokenizer.nextToken() == "(") {
                            nArgs++
                            vmWriter.writePush(Segment.POINTER, 0)
                            "$className.${tokenizer.identifier()}"
                        } else {
                            tokenizer.identifier()
                        }
                    } else {
                        // varName
                        nArgs++
                        vmWriter.writePush(
                            Segment.find(symbolTable.kindOf(tokenizer.identifier())),
                            symbolTable.indexOf(tokenizer.identifier())
                        )
                        symbolTable.typeOf(tokenizer.identifier())
                    }
                    tokenizer.advance()
                    when (tokenizer.tokenType()) {
                        TokenType.SYMBOL -> {
                            when (val symbol = tokenizer.symbol()) {
                                '(' -> {
                                    // (
                                    compileExpressionList()
                                    tokenizer.advance()
                                    // )
                                }
                                '.' -> {
                                    // .
                                    tokenizer.advance()
                                    callSubroutineName += ".${tokenizer.identifier()}"
                                    tokenizer.advance()
                                    // (
                                    compileExpressionList()
                                    tokenizer.advance()
                                    // )
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
        val loopLabel = "while.loop.$subroutineName.$className$whileLabel"
        val exitLabel = "while.exit.$subroutineName.$className$whileLabel"
        whileLabel++
        vmWriter.writeLabel(loopLabel)
        loop@ while (tokenizer.hasMoreTokens()) {
            tokenizer.advance()
            when (tokenizer.tokenType()) {
                TokenType.SYMBOL -> {
                    when (val symbol = tokenizer.symbol()) {
                        '(' -> {
                            // (
                            tokenizer.advance()
                            compileExpression()
                            tokenizer.advance()
                            // )
                            vmWriter.writeArithmetic(Command.NOT)
                            vmWriter.writeIf(exitLabel)
                        }
                        '{' -> {
                            // {
                            tokenizer.advance()
                            compileStatements()
                            tokenizer.advance()
                            // }
                            vmWriter.writeGoto(loopLabel)
                            break@loop
                        }
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }
        vmWriter.writeLabel(exitLabel)
    }

    /**
     * init -> return
     * end -> ;
     */
    private fun compileReturn() {
        // return
        tokenizer.advance()
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            if (tokenizer.identifier() == "this") {
                vmWriter.writePush(Segment.POINTER, 0)
            } else {
                compileExpression()
            }
            tokenizer.advance()
        } else {
            vmWriter.writePush(Segment.CONST, 0)
        }
        // ;
        vmWriter.writeReturn()
    }

    private fun compileExpression() {
        compileTerm()
        loop@ while (tokenizer.hasMoreTokens()) {
            if (!opeSet.contains(tokenizer.nextToken())) break@loop
            tokenizer.advance()
            val ope = tokenizer.symbol().toString()
            tokenizer.advance()
            compileTerm()
            if (ope == "*" || ope == "/") {
                val command = if (ope == "*") "Math.multiply" else "Math.divide"
                vmWriter.writeCall(command, 2)
            } else {
                vmWriter.writeArithmetic(Command.find(ope, unary = false))
            }
        }
    }

    private fun compileExpressionList() {
        // (
        if (tokenizer.nextToken() == ")") {
            return
        }
        tokenizer.advance()
        nArgs++
        compileExpression()
        loop@ while (tokenizer.hasMoreTokens()) {
            if (tokenizer.nextToken() != ",") break@loop
            tokenizer.advance()
            tokenizer.advance()
            nArgs++
            compileExpression()
        }
    }

    private fun compileTerm() {
        when (tokenizer.tokenType()) {
            TokenType.KEYWORD -> {
                when (val keyword = tokenizer.keyword()) {
                    "true" -> {
                        vmWriter.writePush(Segment.CONST, 1)
                        vmWriter.writeArithmetic(Command.NEG)
                    }
                    "false" -> {
                        vmWriter.writePush(Segment.CONST, 0)
                    }
                    "this" -> {
                        vmWriter.writePush(Segment.POINTER, 0)
                    }
                    "null" -> {
                        outputFile.writeWithLF(wrapWithTag(TagName.KEYWORD, keyword))
                    }
                    else -> throw IllegalArgumentException()
                }
            }
            TokenType.SYMBOL -> {
                when (val symbol = tokenizer.symbol()) {
                    '-', '~' -> {
                        val ope = symbol.toString()
                        tokenizer.advance()
                        compileTerm()
                        vmWriter.writeArithmetic(Command.find(ope, true))
                    }
                    '(' -> {
                        tokenizer.advance()
                        compileExpression()
                        tokenizer.advance()
                    }
                }
            }
            TokenType.IDENTIFIER -> {
                when (tokenizer.nextToken()) {
                    "[" -> {
                        vmWriter.writePush(
                            Segment.find(symbolTable.kindOf(tokenizer.identifier())),
                            symbolTable.indexOf(tokenizer.identifier())
                        )
                        tokenizer.advance()
                        tokenizer.advance()
                        compileExpression()
                        vmWriter.writeArithmetic(Command.ADD)
                        vmWriter.writePop(Segment.POINTER, 1)
                        vmWriter.writePush(Segment.THAT, 0)
                        tokenizer.advance()
                    }
                    "(" -> {
                        // subroutineName
                        callSubroutineName = "$className."
                        callSubroutineName += tokenizer.identifier()
                        tokenizer.advance()
                        // (
                        compileExpressionList()
                        tokenizer.advance()
                        // )
                        vmWriter.writePush(Segment.POINTER, 0)
                        vmWriter.writeCall(callSubroutineName, nArgs)
                    }
                    "." -> {
                        callSubroutineName = ""
                        callSubroutineName += if (symbolTable.kindOf(tokenizer.identifier()) == Kind.NONE) {
                            // className
                            tokenizer.identifier()
                        } else {
                            // varName
                            nArgs++
                            vmWriter.writePush(
                                Segment.find(symbolTable.kindOf(tokenizer.identifier())),
                                symbolTable.indexOf(tokenizer.identifier())
                            )
                            symbolTable.typeOf(tokenizer.identifier())
                        }
                        tokenizer.advance()
                        // .
                        tokenizer.advance()
                        // subroutineName
                        callSubroutineName += ".${tokenizer.identifier()}"
                        // (
                        tokenizer.advance()
                        compileExpressionList()
                        // )
                        tokenizer.advance()
                        vmWriter.writeCall(callSubroutineName, nArgs)
                    }
                    else -> {
                        when (symbolTable.kindOf(tokenizer.identifier())) {
                            Kind.ARG -> {
                                vmWriter.writePush(Segment.ARG, symbolTable.indexOf(tokenizer.identifier()))
                            }
                            Kind.VAR -> {
                                vmWriter.writePush(Segment.LOCAL, symbolTable.indexOf(tokenizer.identifier()))
                            }
                            Kind.FIELD -> {
                                vmWriter.writePush(Segment.THIS, symbolTable.indexOf(tokenizer.identifier()))
                            }
                            Kind.STATIC -> {
                                vmWriter.writePush(Segment.STATIC, symbolTable.indexOf(tokenizer.identifier()))
                            }
                            else -> throw IllegalArgumentException()
                        }
                    }
                }
            }
            TokenType.INT_CONST -> {
                vmWriter.writePush(Segment.CONST, tokenizer.intVal())
            }
            TokenType.STRING_CONST -> {
                val str = tokenizer.stringVal()
                vmWriter.writePush(Segment.CONST, str.length)
                vmWriter.writeCall("String.new", 1)
                str.forEach {
                    vmWriter.writePush(Segment.CONST, it.toInt())
                    vmWriter.writeCall("String.appendChar", 2)
                }
            }
        }
    }

    private fun genStartTag(str: String) = "<$str>"

    private fun genEndTag(str: String) = "</$str>"

    private fun wrapWithTag(tagName: TagName, value: String): String {
        return genStartTag(tagName.value) + value + genEndTag(tagName.value)
    }
}
