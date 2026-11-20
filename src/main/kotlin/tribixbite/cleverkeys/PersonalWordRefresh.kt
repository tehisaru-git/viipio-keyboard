package tribixbite.cleverkeys

/** Narrow persistence-to-decoder refresh seam for explicit personal-word actions. */
internal class PersonalWordRefresh(
    private val words: ViipioPersonalWords,
    private val onChanged: () -> Unit,
) {
    fun add(language: String, word: String): ViipioPersonalWords.AddResult {
        val result = words.add(language, word)
        if (result == ViipioPersonalWords.AddResult.ADDED) onChanged()
        return result
    }

    fun remove(language: String, word: String): Boolean {
        val removed = words.remove(language, word)
        if (removed) onChanged()
        return removed
    }
}
