package tribixbite.cleverkeys.swipeui

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import tribixbite.cleverkeys.estonianui.KeyboardLanguage
import tribixbite.cleverkeys.swipe.geometric.CkdtDictionaryReader
import android.content.Context
import tribixbite.cleverkeys.ViipioPersonalWords
import tribixbite.cleverkeys.StaticContextLm
import tribixbite.cleverkeys.ViipioRankingFieldPolicy
import java.util.Locale

/** Read-only, bounded lookup of the bundled tap dictionaries; no learning or autocorrect. */
internal class ManualCompletions private constructor(
    private val indexes: Map<KeyboardLanguage, PrefixIndex>,
    private val personalWords: (KeyboardLanguage) -> List<String>,
    private val personalPresence: (KeyboardLanguage, String) -> Boolean?,
) {
    private data class Offer(val connection: InputConnection, val caret: Int, val before: String,
                             val after: String, val start: Int, val end: Int,
                             val word: String, val words: List<String>)
    private var offer: Offer? = null
    var words: List<String> = emptyList()
        private set

    fun clear() { offer = null; words = emptyList() }

    /** Only known words suppress the explicit inline personal-word offer. */
    fun contains(word: String, language: KeyboardLanguage): Boolean {
        if (!ViipioPersonalWords.validWord(word)) return false
        val present = try { personalPresence(language, word) } catch (_: Exception) { null }
        if (present != false) return true // Unreadable CE storage is not an empty dictionary.
        return indexes[language]?.contains(word) ?: true
    }

    fun update(connection: InputConnection, info: EditorInfo, language: KeyboardLanguage,
               contextModel: StaticContextLm? = null): List<String> {
        return try {
        clear()
        if (!allowed(info)) return words
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return words
        val caretLong = extracted.startOffset.toLong() + extracted.selectionEnd
        if (extracted.startOffset < 0 || extracted.selectionStart != extracted.selectionEnd ||
            caretLong !in 0..Int.MAX_VALUE.toLong()) return words
        val caret = caretLong.toInt()
        val before = connection.getTextBeforeCursor(256, 0)?.toString() ?: return words
        val after = connection.getTextAfterCursor(64, 0)?.toString() ?: return words
        // Android returns null, not an empty string, for a collapsed selection in many editors.
        if (connection.getSelectedText(0)?.isNotEmpty() == true) return words
        var start = before.length
        while (start > 0 && (before[start - 1].isLetter() || before[start - 1] == '\'' || before[start - 1] == '-')) start--
        var suffixLength = 0
        while (suffixLength < after.length && (after[suffixLength].isLetter() ||
                   after[suffixLength] == '\'' || after[suffixLength] == '-')) suffixLength++
        if ((start == before.length && suffixLength == 0) ||
            (start == 0 && caret > before.length) ||
            (suffixLength == after.length && after.length >= 64)) return words
        val word = before.substring(start) + after.substring(0, suffixLength)
        if (word.length > 48 || word.any { !it.isLetter() }) return words
        val prefix = word.lowercase(Locale.ROOT)
        val personal = try { personalWords(language) } catch (_: Exception) { emptyList() }
        val index = indexes[language]
        val prefixMatches = if (suffixLength == 0) {
            personal.asSequence().filter { candidate ->
                ViipioPersonalWords.validWord(candidate) && candidate.startsWith(prefix, ignoreCase = true) &&
                    !candidate.equals(word, ignoreCase = true)
            }.take(3).toList() + index?.find(prefix).orEmpty()
                .filterNot { it.equals(word, ignoreCase = true) }
        } else emptyList()
        // A moved caret into an earlier word refers to the *whole* word. Prefer
        // small local edit candidates over completing only its left-hand fragment.
        val typoMatches = if (word.length in 3..24 && (suffixLength > 0 || prefixMatches.isEmpty())) {
            personal.filter { ViipioPersonalWords.validWord(it) &&
                PrefixIndex.oneEditAway(prefix, it.lowercase(Locale.ROOT)) }.take(3) +
                index?.near(prefix, language).orEmpty()
        } else emptyList()
        val matches = (prefixMatches + typoMatches)
            .filterNot { it.equals(word, ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ROOT) }.take(3)
        if (matches.isEmpty()) return words
        // Context can change only the order of existing, user-actionable suggestions. The
        // current prefix is not a previous word; a truncated left boundary is not evidence.
        val code = if (language == KeyboardLanguage.ENGLISH) "en" else "et"
        val previous = if (suffixLength == 0 && ViipioRankingFieldPolicy.allowed(info) &&
            contextModel?.language == code) {
            val priorText = before.substring(0, start)
            val atStart = caret == before.length
            tribixbite.cleverkeys.ViipioSwipeRanking.previousWord(
                if (atStart) " $priorText" else priorText)
        } else null
        val rankedMatches = rankTyped(matches, previous, contextModel)
        words = when {
            word.length > 1 && word.all(Char::isUpperCase) ->
                rankedMatches.map { it.uppercase(Locale.ROOT) }
            word.first().isUpperCase() ->
                rankedMatches.map { it.replaceFirstChar { letter -> letter.titlecase(Locale.ROOT) } }
            else -> rankedMatches
        }
        offer = Offer(connection, caret, before, after, caret - (before.length - start),
            caret + suffixLength, word, words)
        return words
    } catch (_: Exception) {
        clear()
        words
        }
    }

    fun choose(connection: InputConnection, index: Int): Boolean {
        val current = offer ?: return false
        val chosen = current.words.getOrNull(index) ?: return false
        clear() // A failed write must never leave an actionable stale candidate.
        if (connection !== current.connection) return false
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return false
        val caret = extracted.startOffset.toLong() + extracted.selectionEnd
        if (extracted.startOffset < 0 || extracted.selectionStart != extracted.selectionEnd || caret != current.caret.toLong() ||
            connection.getSelectedText(0)?.isNotEmpty() == true ||
            connection.getTextBeforeCursor(256, 0)?.toString() != current.before ||
            connection.getTextAfterCursor(64, 0)?.toString() != current.after) return false
        if (current.start < 0 || current.end < current.start ||
            !connection.setSelection(current.start, current.end)) return false
        if (connection.getSelectedText(0)?.toString() != current.word) {
            connection.setSelection(current.caret, current.caret)
            return false
        }
        if (!connection.commitText(chosen, 1)) {
            connection.setSelection(current.caret, current.caret)
            return false
        }
        return true
    }

    private fun allowed(info: EditorInfo): Boolean {
        // Explicitly exclude private/incognito fields *before* reading personal words.
        if (!ViipioRankingFieldPolicy.allowed(info)) return false
        return true
    }

    private class PrefixIndex(entries: List<Pair<String, Int>>) {
        private val ranked = entries.asSequence().filter { (word, _) ->
            word.length in 2..48 && word.all(Char::isLetter)
        }.sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first })
            .map { it.first }.distinct().toList()
        // Only the short prefixes need a precomputed index; longer queries use a
        // lexicographic range and keep just three ranked entries while scanning it.
        private val top = HashMap<String, MutableList<String>>()
        private val alphabetic = ranked.withIndex().sortedBy { it.value }
        private val exactRanks = ranked.withIndex().associate { it.value.lowercase(Locale.ROOT) to it.index }
        fun contains(word: String): Boolean = word.lowercase(Locale.ROOT) in exactRanks
        init {
            for (word in ranked) for (length in 1..minOf(3, word.length)) {
                val bucket = top.getOrPut(word.substring(0, length)) { ArrayList(3) }
                if (bucket.size < 3) bucket.add(word)
            }
        }
        fun find(prefix: String): List<String> {
            if (prefix.length <= 3) return top[prefix]?.toList().orEmpty()
            var low = 0
            var high = alphabetic.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (alphabetic[middle].value < prefix) low = middle + 1 else high = middle
            }
            val best = ArrayList<IndexedValue<String>>(3)
            while (low < alphabetic.size && alphabetic[low].value.startsWith(prefix)) {
                val entry = alphabetic[low++]
                if (best.size < 3 || entry.index < best.last().index) {
                    best.add(entry)
                    best.sortBy { it.index }
                    if (best.size > 3) best.removeAt(3)
                }
            }
            return best.map { it.value }
        }
        /** Bounded edit-1 lookup: no scan of the full dictionary on the UI thread. */
        fun near(word: String, language: KeyboardLanguage): List<String> {
            if (word.length !in 3..24) return emptyList()
            val alphabet = if (language == KeyboardLanguage.ESTONIAN)
                "abcdefghijklmnopqrstuvwxyzõäöüšž" else "abcdefghijklmnopqrstuvwxyz"
            val hits = HashMap<String, Int>()
            fun consider(value: String) {
                if (value != word) exactRanks[value]?.let { hits[value] = it }
            }
            for (i in word.indices) {
                consider(word.removeRange(i, i + 1))
                if (i + 1 < word.length && word[i] != word[i + 1]) {
                    val swapped = word.toCharArray()
                    swapped[i] = word[i + 1]
                    swapped[i + 1] = word[i]
                    consider(String(swapped))
                }
                for (letter in alphabet) if (letter != word[i])
                    consider(word.replaceRange(i, i + 1, letter.toString()))
            }
            for (i in 0..word.length) for (letter in alphabet)
                consider(word.substring(0, i) + letter + word.substring(i))
            return hits.entries.sortedWith(compareBy<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key }).take(3).map { it.key }
        }
        companion object {
            /** Includes a single adjacent swap; only up to 200 personal words are checked. */
            fun oneEditAway(a: String, b: String): Boolean {
                if (kotlin.math.abs(a.length - b.length) > 1 || a == b) return false
                if (a.length == b.length) {
                    val differences = a.indices.filter { a[it] != b[it] }
                    return differences.size == 1 || (differences.size == 2 &&
                        differences[1] == differences[0] + 1 &&
                        a[differences[0]] == b[differences[1]] &&
                        a[differences[1]] == b[differences[0]])
                }
                val (longer, shorter) = if (a.length > b.length) a to b else b to a
                var i = 0; var j = 0; var skipped = false
                while (i < longer.length && j < shorter.length) {
                    if (longer[i] == shorter[j]) { i++; j++ }
                    else if (skipped) return false
                    else { skipped = true; i++ }
                }
                return true
            }
        }
    }

    companion object {
        /** Suggestions are not auto-committed; only listed language-matched evidence may reorder. */
        internal fun rankTyped(candidates: List<String>, previous: String?,
                               model: StaticContextLm?): List<String> {
            if (candidates.size < 2 || previous == null || model == null) return candidates
            val listed = candidates.map { model.listedProbability(previous, it).toDouble() }
            val best = listed.indices.maxByOrNull { listed[it] } ?: return candidates
            if (best == 0 || listed[best] < 0.02 || listed[best] <= listed[0] * 1.5) return candidates
            return listOf(candidates[best]) + candidates.filterIndexed { i, _ -> i != best }
        }

        fun fromEntries(et: List<Pair<String, Int>>, en: List<Pair<String, Int>>,
                        personalPresence: ((KeyboardLanguage, String) -> Boolean?)? = null,
                        personalWords: (KeyboardLanguage) -> List<String> = { emptyList() }): ManualCompletions {
            val presence = personalPresence ?: { language: KeyboardLanguage, word: String ->
                personalWords(language).any { it.equals(word, ignoreCase = true) }
            }
            return ManualCompletions(mapOf(KeyboardLanguage.ESTONIAN to PrefixIndex(et),
                KeyboardLanguage.ENGLISH to PrefixIndex(en)), personalWords, presence)
        }

        fun fromBundledAssets(context: Context): ManualCompletions {
            val personal = ViipioPersonalWords(context)
            return fromEntries(
            context.assets.open("dictionaries/et_enhanced.bin").use { input ->
                CkdtDictionaryReader.readEntries(input).map { it.word to it.rank }
            },
            context.assets.open("dictionaries/en_enhanced.bin").use { input ->
                CkdtDictionaryReader.readEntries(input).map { it.word to it.rank }
            }, personalPresence = { language, word ->
                personal.containsExact(if (language == KeyboardLanguage.ESTONIAN) "et" else "en", word)
            }) { language -> personal.words(if (language == KeyboardLanguage.ESTONIAN) "et" else "en") }
        }
    }
}
