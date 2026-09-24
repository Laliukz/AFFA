package com.lkz.affa.core.common.util

/**
 * Utilidad para desempaquetar código JavaScript protegido con el empaquetador Dean Edwards (eval(function(p,a,c,k,e,d)...))
 * muy común en servidores de streaming de video como StreamWish, FileMoon, VidHide, etc.
 */
object JsUnpacker {

    fun unpack(packedJs: String): String? {
        val regex = Regex("""eval\(function\(p,a,c,k,e,[rd]\)\{.*\}\(['"](.*?)['"],\s*(\d+),\s*(\d+),\s*['"](.*?)['"]\.split\('\|'\)""")
        val match = regex.find(packedJs) ?: return null

        val payload = match.groupValues[1]
        val radix = match.groupValues[2].toIntOrNull() ?: 36
        val count = match.groupValues[3].toIntOrNull() ?: 0
        val symTab = match.groupValues[4].split("|")

        fun encodeRadix(num: Int, rad: Int): String {
            val digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            if (num < rad) return digits[num].toString()
            return encodeRadix(num / rad, rad) + digits[num % rad].toString()
        }

        val dict = mutableMapOf<String, String>()
        for (i in 0 until count) {
            val key = encodeRadix(i, radix)
            val word = symTab.getOrNull(i)
            dict[key] = if (!word.isNullOrEmpty()) word else key
        }

        val wordRegex = Regex("""\b\w+\b""")
        return wordRegex.replace(payload) { m ->
            dict[m.value] ?: m.value
        }
    }
}
