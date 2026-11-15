package tribixbite.cleverkeys

/** Only successful Viipio key commits may feed this short-lived buffer; no editor text is read here. */
internal class ViipioTypedWordTracker {
    private val letters = StringBuilder()
    fun letter(value: String): Boolean {
        if (value.length != 1 || !value[0].isLetter() ||
            Character.UnicodeScript.of(value[0].code) != Character.UnicodeScript.LATIN ||
            letters.length >= 40) {
            clear()
            return false
        }
        letters.append(value)
        return true
    }
    fun backspace(): Boolean {
        if (letters.isEmpty()) return false
        letters.deleteCharAt(letters.lastIndex)
        return true
    }
    fun complete(): String? {
        val value = letters.toString()
        clear()
        return value.takeIf(ViipioPersonalWords::validWord)
    }
    fun clear() { letters.clear() }
    val pending: String get() = letters.toString()
}
