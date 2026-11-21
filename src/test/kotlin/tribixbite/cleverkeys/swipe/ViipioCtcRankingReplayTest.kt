package tribixbite.cleverkeys.swipe

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume
import org.junit.Test
import tribixbite.cleverkeys.PredictionResult
import tribixbite.cleverkeys.StaticContextLm
import tribixbite.cleverkeys.ViipioSwipeRanking
import java.io.File

/**
 * Fixed, nonpersonal EN synthetic gestures through the shipped CTC replay stack and en.cklm.
 * These are key-centre paths, NOT human traces or an estimate of human accuracy. Cases and
 * contexts are declared before decoding, in both directions for each confusing pair. The
 * same decoded slate is compared before/after ranking; missing or below-floor targets are
 * counted, never inserted or silently dropped from the denominator. No personal preference.
 * CTC is skipped only when ONNX natives are unavailable; the CKLM boundary test always runs.
 */
class ViipioCtcRankingReplayTest {
    private data class Case(val target: String, val context: String)

    private val cases = listOf(
        Case("then", "and"), Case("than", "rather"),
        Case("there", "over"), Case("their", "with"),
        Case("to", "want"), Case("too", "far"),
    )

    private fun shippedEnModel(): StaticContextLm =
        StaticContextLm.parse(File("src/main/assets/lm/en.cklm").readBytes())

    @Test fun shippedCklmPromotesToOnlyAtTheInclusiveDecoderFloor() {
        val model = shippedEnModel()
        assertThat(model.language).isEqualTo("en")
        assertThat(model.listedProbability("want", "to")).isGreaterThan(0f)
        assertThat(model.listedProbability("want", "too")).isEqualTo(0f)

        val eligible = PredictionResult(listOf("too", "to"), listOf(1000, 650))
        val promoted = ViipioSwipeRanking.rank(eligible, "en", "want", model)
        assertThat(promoted.words).containsExactly("to", "too").inOrder()
        assertThat(promoted.scores).containsExactly(650, 1000).inOrder()

        val ineligible = PredictionResult(listOf("too", "to"), listOf(1000, 649))
        assertThat(ViipioSwipeRanking.rank(ineligible, "en", "want", model))
            .isSameInstanceAs(ineligible)
    }

    @Test fun fixedBothDirectionCtcSlatesKeepCandidateAndScoreInvariants() {
        Assume.assumeTrue(
            "ONNX natives absent — run through runPureTests with extracted ONNX natives",
            CtcReplayEngine.ortAvailable(),
        )
        val model = shippedEnModel()
        var decoded = 0
        var multiCandidate = 0
        var absent = 0
        var belowFloor = 0
        var baselineTop1 = 0
        var rankedTop1 = 0
        var baselineTop3 = 0
        var rankedTop3 = 0
        var corrections = 0
        var regressions = 0

        CtcReplayEngine.build("en").use { engine ->
            for (case in cases) {
                assertWithMessage("${case.context} -> ${case.target} must be listed in shipped CKLM")
                    .that(model.listedProbability(case.context, case.target)).isGreaterThan(0f)
                val (x, y, t) = CtcTraceShapes.straight(case.target, engine.layoutGeometry)
                val slate = engine.decode(x, y, t) // exactly one decode per declared gesture
                assertWithMessage("${case.target}: decoder word/score alignment")
                    .that(slate.scores.size).isEqualTo(slate.words.size)
                if (slate.words.isNotEmpty()) decoded++
                if (slate.words.size > 1) multiCandidate++

                val original = PredictionResult(slate.words, slate.scores)
                val ranked = ViipioSwipeRanking.rank(original, "en", case.context, model)
                assertWithMessage("${case.target}: ranking must not add or remove candidates")
                    .that(ranked.words).containsExactlyElementsIn(slate.words)
                assertWithMessage("${case.target}: ranking must retain each word's decoder score")
                    .that(ranked.words.zip(ranked.scores))
                    .containsExactlyElementsIn(slate.words.zip(slate.scores))
                val promotedIndex = if (ranked.words.isEmpty()) -1 else slate.words.indexOf(ranked.words[0])
                assertWithMessage("${case.target}: only the promoted head may move")
                    .that(ranked.words.drop(1))
                    .containsExactlyElementsIn(slate.words.filterIndexed { i, _ -> i != promotedIndex })
                    .inOrder()

                val targetIndex = slate.words.indexOf(case.target)
                if (targetIndex < 0) absent++
                else if (targetIndex > 0 && slate.scores[targetIndex].toDouble() < 0.65 * slate.scores[0]) {
                    belowFloor++
                    assertWithMessage("${case.target}: below-floor candidate cannot become head")
                        .that(ranked.words.first()).isEqualTo(slate.words.first())
                }
                val before1 = slate.words.firstOrNull() == case.target
                val after1 = ranked.words.firstOrNull() == case.target
                if (before1) baselineTop1++
                if (after1) rankedTop1++
                if (case.target in slate.words.take(3)) baselineTop3++
                if (case.target in ranked.words.take(3)) rankedTop3++
                if (!before1 && after1) corrections++
                if (before1 && !after1) regressions++
                println("[viipio-ctc] target=${case.target} prev=${case.context} " +
                    "baseline=${slate.words.take(3)} scores=${slate.scores.take(3)} " +
                    "ranked=${ranked.words.take(3)}")
            }
        }
        assertThat(cases.size).isEqualTo(6)
        assertWithMessage("no CTC slate decoded; replay must not pass vacuously")
            .that(decoded).isGreaterThan(0)
        assertWithMessage("no multi-candidate CTC slate; ranking was never exercised")
            .that(multiCandidate).isGreaterThan(0)
        assertThat(baselineTop3).isAtLeast(baselineTop1)
        assertThat(rankedTop3).isAtLeast(rankedTop1)
        assertThat(rankedTop1 - baselineTop1).isEqualTo(corrections - regressions)
        assertThat(absent + belowFloor).isAtMost(cases.size)
        println("[viipio-ctc] fixed synthetic cases=${cases.size} decoded=$decoded " +
            "multi=$multiCandidate absent=$absent belowFloor=$belowFloor " +
            "top1=$baselineTop1->$rankedTop1 top3=$baselineTop3->$rankedTop3 " +
            "corrections=$corrections regressions=$regressions (not human accuracy)")
    }
}
