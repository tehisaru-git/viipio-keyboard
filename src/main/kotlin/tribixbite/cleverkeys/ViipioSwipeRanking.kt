package tribixbite.cleverkeys

import kotlin.math.ln
import kotlin.math.max

/**
 * Pure, conservative reordering of a decoder's *existing* swipe slate. The active IME owns field
 * eligibility, model loading and explicitly confirmed correction counts; this object owns no state,
 * editor text, I/O or learning. Original integer scores are decoder evidence, NOT probabilities:
 * they travel with their words unchanged. Only the first slot may change; all other slots retain
 * their original relative order. A caller can omit [preferred] to disable personal correction.
 */
internal object ViipioSwipeRanking {
    private const val MAX_SUFFIX = 96
    private const val MAX_WORD = 40
    private const val MAX_CANDIDATES = 8
    private const val MAX_SCORE = 1000
    private const val MIN_RELATIVE_SCORE = 0.65
    private const val MAX_BOOST = 2.0
    private const val UNLISTED_FLOOR = 0.01

    /**
     * Extract a complete prior word from an editor-supplied suffix (at most 96 chars inspected).
     * Requires whitespace *after* the word and a separator *before* it: a suffix may begin mid-word,
     * so even an apparent word at its left edge is not evidence of a complete word. A sentence end
     * or newline immediately before the cursor invalidates context. No raw suffix is retained.
     */
    fun previousWord(before: String): String? {
        if (before.isEmpty()) return null
        val from = (before.length - MAX_SUFFIX).coerceAtLeast(0)
        var end = before.length
        if (before[end - 1] != ' ' && before[end - 1] != '\t') return null
        while (end > from && (before[end - 1] == ' ' || before[end - 1] == '\t')) end--
        if (end == from || before[end - 1] in ".!?" || before[end - 1] == '\n') return null
        var start = end
        while (start > from && (before[start - 1].isLetter() || before[start - 1] == '\'' || before[start - 1] == '’')) {
            start--
            if (end - start > MAX_WORD) return null
        }
        if (start == from || !before[start - 1].isWhitespace()) return null
        val word = before.substring(start, end)
        if (!word.first().isLetter() || !word.last().isLetter()) return null
        return word.lowercase()
    }

    /**
     * [preferred] returns the caller's *net confirmed* choices (head→rival minus rival→head),
     * not raw text frequency. It is called only with (original head, rival); the caller computes
     * any bidirectional difference. At least two net choices enable a boost, capped at ×2.
     * The default zero callback supplies no personal signal.
     *
     * For English context, only *listed* CKLM conditional probabilities count. The ratio of the
     * rival's P(word|prev) to the head's listed P(word|prev), with a fixed 0.01 floor when the head
     * is unlisted, enters a bounded log boost. Decoder rank integers are never interpreted as LM
     * probabilities. Estonian has no static model path. Either signal can win, never their product.
     */
    fun rank(
        result: PredictionResult,
        language: String,
        previousWord: String?,
        enModel: StaticContextLm?,
        preferred: (String, String) -> Int = { _, _ -> 0 },
    ): PredictionResult {
        val lang = language.lowercase()
        if (lang != "et" && lang != "en") return result
        val words = result.words
        val scores = result.scores
        val languages = result.languages
        if (words.size !in 2..MAX_CANDIDATES || scores.size != words.size ||
            (languages != null && languages.size != words.size) ||
            words.any { it.isBlank() || it.length > MAX_WORD } || scores.any { it !in 0..MAX_SCORE } ||
            (languages != null && languages.any { it.lowercase() != "et" && it.lowercase() != "en" }) ||
            (languages != null && languages[0].lowercase() != lang) || scores[0] == 0
        ) return result

        val head = words[0].lowercase()
        val topScore = scores[0]
        val model = enModel?.takeIf { lang == "en" && it.language.lowercase() == "en" }
        val context = previousWord?.takeIf { it.length in 1..MAX_WORD &&
            it.first().isLetter() && it.last().isLetter() &&
            it.all { char -> char.isLetter() || char == '\'' || char == '’' } }
        val headP = if (model != null && context != null)
            model.listedProbability(context, head).toDouble() else 0.0
        var bestIndex = 0
        var bestEvidence = topScore.toDouble()
        for (i in 1 until words.size) {
            if (languages != null && languages[i].lowercase() != lang) continue
            val score = scores[i]
            if (score <= 0 || score > topScore || score.toDouble() < MIN_RELATIVE_SCORE * topScore) continue
            val candidate = words[i].lowercase()
            if (candidate == head) continue

            val net: Int
            try {
                net = preferred(head, candidate)
            } catch (_: RuntimeException) {
                continue // an unavailable preference store must not break typing
            }
            val personalBoost = if (net >= 2) 1.0 + minOf(net, 4) * 0.25 else 1.0

            val listed = if (model != null && context != null)
                model.listedProbability(context, candidate).toDouble() else 0.0
            val staticBoost = if (listed > max(headP, UNLISTED_FLOOR)) {
                (1.0 + 0.35 * ln(listed / max(headP, UNLISTED_FLOOR))).coerceAtMost(MAX_BOOST)
            } else 1.0
            val evidence = score * max(personalBoost, staticBoost)
            // Strict > preserves the original order for ties, including the original head.
            if (evidence > bestEvidence) {
                bestIndex = i
                bestEvidence = evidence
            }
        }
        if (bestIndex == 0) return result
        val order = listOf(bestIndex) + words.indices.filter { it != bestIndex }
        return PredictionResult(order.map(words::get), order.map(scores::get), languages?.let { labels -> order.map(labels::get) })
    }
}
