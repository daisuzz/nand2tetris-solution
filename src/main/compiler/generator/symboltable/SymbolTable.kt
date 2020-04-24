package compiler.symboltable

class SymbolTable {

    private val classHashMap = mutableMapOf<String, SymbolInfo>()

    private val subroutineMap = mutableMapOf<String, SymbolInfo>()

    /**
     * 新しいサブルーチンのスコープを開始する
     */
    fun startSubroutine() {
        subroutineMap.clear()
    }

    /**
     * 引数の名前、型、属性で指定された新しい識別子を定義し、それに実行インデックスを割り当てる
     */
    fun define(name: String, type: String, kind: Kind) {
        when (kind) {
            Kind.STATIC, Kind.FIELD -> {
                val maxIndex = classHashMap.entries
                    .filter { it.value.kind == kind }
                    .maxBy { it.value.index }
                    ?.value?.index ?: -1
                classHashMap[name] = SymbolInfo(type, kind, maxIndex + 1)
            }
            Kind.ARG, Kind.VAR -> {
                val maxIndex = subroutineMap.entries
                    .filter { it.value.kind == kind }
                    .maxBy { it.value.index }
                    ?.value?.index ?: -1
                subroutineMap[name] = SymbolInfo(type, kind, maxIndex + 1)
            }
            Kind.NONE -> throw IllegalArgumentException()
        }
    }

    /**
     * 引数で与えられた属性について、それが現在のスコープで定義されている数を返す
     */
    fun varCount(kind: Kind): Int {
        return when (kind) {
            Kind.STATIC, Kind.FIELD -> classHashMap.count { it.value.kind == kind }
            Kind.ARG, Kind.VAR -> subroutineMap.count { it.value.kind == kind }
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * 引数で与えられた名前の識別子を現在のスコープで探し、その属性を返す
     * 見つかれなければNONEを返す
     */
    fun kindOf(name: String): Kind {
        return subroutineMap[name]?.kind ?: classHashMap[name]?.kind ?: Kind.NONE
    }

    /**
     * 引数で与えられた名前の識別子を現在のスコープで探し、その型を返す
     */
    fun typeOf(name: String): String {
        return subroutineMap[name]?.type ?: classHashMap[name]?.type ?: throw IllegalArgumentException()
    }

    /**
     * 引数で与えられた名前の識別子を現在のスコープで探し、そのインデックスを返す
     */
    fun indexOf(name: String): Int {
        return subroutineMap[name]?.index ?: classHashMap[name]?.index ?: throw IllegalArgumentException()
    }
}
