package compiler

import java.io.BufferedReader
import java.io.File

class JackTokenizer(inputFile: File) {

    private val bufferedReader: BufferedReader = inputFile.bufferedReader()

    private val nextTokenList: MutableList<String> = mutableListOf()

    var currentToken: String = ""

    private val keywords: Set<String> = setOf(
        "class",
        "constructor",
        "function",
        "method",
        "field",
        "static",
        "var",
        "int",
        "char",
        "boolean",
        "void",
        "true",
        "false",
        "null",
        "this",
        "let",
        "do",
        "if",
        "else",
        "while",
        "return"
    )

    private val symbols: Set<String> = setOf(
        "{",
        "}",
        "(",
        ")",
        "[",
        "]",
        ".",
        ",",
        ";",
        "+",
        "-",
        "*",
        "/",
        "&",
        "|",
        "<",
        ">",
        "=",
        "~"
    )

    /**
     * 入力.jackファイルを読み込み、トークンの一覧を生成してnuxtTokenListに追加する
     */
    init {
        var line = nextLine()
        while (line != null) {
            if (line.isEmpty()) {
                line = nextLine()
                continue
            }
            val commentStartIdx = line.indexOf("/*")
            val commentEndIdx = line.indexOf("*/")

            if (commentStartIdx != -1 && commentEndIdx != -1) {
                line = line.replaceRange(commentStartIdx, commentEndIdx + 2, "")
            }
            if (commentStartIdx != -1 && commentEndIdx == -1) {
                line = line.substringBefore("/*")
            }
            if (commentStartIdx == -1 && commentEndIdx != -1) {
                line = line.substringAfter("*/")
            }

            val doubleQuoteSegments = line.split("\"")
            for (index in doubleQuoteSegments.indices) {
                if (index % 2 != 0) {
                    nextTokenList.add("\"${doubleQuoteSegments[index]}\"")
                    continue
                }
                val trimmedLine = doubleQuoteSegments[index].replace("(\\s)+".toRegex(), " ")
                trimmedLine.split(" ")
                    .filterNot { it.isEmpty() }
                    .forEach { word -> splitSymbol(word, nextTokenList) }
            }

            line = nextLine()
        }
    }

    /**
     * シンボルを含んだテキストをトークンに分割してnextTokenListに追加する
     */
    private fun splitSymbol(word: String, tokenList: MutableList<String>) {
        val symbolIdx = word.indexOfFirst { symbols.contains(it.toString()) }
        if (word.length == 1 || symbolIdx == -1) {
            tokenList.add(word)
            return
        }

        val token = if (symbolIdx == 0) word.first().toString() else word.substring(0, symbolIdx)
        val rest = if (symbolIdx == 0) word.substring(1) else word.substring(symbolIdx)
        tokenList.add(token)
        splitSymbol(rest, tokenList)
    }

    /**
     * 入力にまだトークンは存在するかを返す
     */
    fun hasMoreTokens(): Boolean {
        return nextTokenList.isNotEmpty()
    }

    /**
     * 入力から次のトークンを取得し、それを現在のトークンとする。
     * このルーチンは、hasMoreTokens()がtrueの場合のみ呼び出すことができる。
     */
    fun advance() {
        currentToken = nextTokenList.first()
        nextTokenList.removeAt(0)
    }

    /**
     * 現トークンの種類を返す
     */
    fun tokenType(): TokenType {

        return when {
            keywords.contains(currentToken) -> TokenType.KEYWORD
            symbols.contains(currentToken) -> TokenType.SYMBOL
            isInteger(currentToken) -> TokenType.INT_CONST
            currentToken.startsWith("\"") && currentToken.endsWith("\"") -> TokenType.STRING_CONST
            isIdentifier(currentToken) -> TokenType.IDENTIFIER
            else -> throw NoSuchElementException()
        }
    }

    /**
     * 現トークンのキーワードを返す。
     * このルーチンはtokenType()がKEYWORDの場合のみ呼び出すことができる
     */
    fun keyword(): String {
        return currentToken
    }

    fun symbol(): Char {
        return currentToken.first()
    }

    fun identifier(): String {
        return currentToken
    }

    fun intVal(): Int {
        return currentToken.toInt()
    }

    fun stringVal(): String {
        return currentToken.trim('"')
    }

    private fun nextLine(): String? {
        return bufferedReader.readLine()?.trim()?.substringBefore("//")
    }

    private fun isInteger(token: String): Boolean {
        val pattern = Regex("^0$|^[1-9][0-9]*$")
        return pattern.containsMatchIn(token)
    }

    private fun isIdentifier(token: String): Boolean {
        val pattern = Regex("^[^0-9]+.*")
        return pattern.containsMatchIn(token)
    }
}
