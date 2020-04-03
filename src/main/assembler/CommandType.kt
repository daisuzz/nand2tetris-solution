package assembler


enum class CommandType {

    A_COMMAND {
        override fun toBinaryCode(str: String): String {
            return "0${str.toInt().toString(radix = 2).padStart(15, '0')}"
        }
    },
    C_COMMAND {
        override fun toBinaryCode(str: String): String {
            return "111$str"
        }
    },
    L_COMMAND {
        override fun toBinaryCode(str: String): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    };

    abstract fun toBinaryCode(str: String): String
}
