package assembler

class SymbolTable {

    private val symbolTable: MutableMap<String, Int> = mutableMapOf()

    /**
     * シンボルテーブルに(symbol, address)のペアを追加する
     */
    fun addEntry(symbol: String, address: Int){
        TODO()
    }

    /**
     * シンボルテーブルに引数で与えたsymbolが存在するかどうか
     */
    fun contains(symbol: String): Boolean{
        TODO()
    }

    /**
     * symbolに結び付けられたアドレスを返す
     */
    fun getAddress(symbol: String): Int{
        TODO()
    }
}
