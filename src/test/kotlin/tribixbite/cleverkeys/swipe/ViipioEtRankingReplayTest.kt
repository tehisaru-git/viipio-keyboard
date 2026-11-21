package tribixbite.cleverkeys.swipe

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.json.JSONObject
import org.junit.Assume
import org.junit.Test
import tribixbite.cleverkeys.PredictionResult
import tribixbite.cleverkeys.StaticContextLm
import tribixbite.cleverkeys.ViipioSwipeRanking
import tribixbite.cleverkeys.swipe.ctc.CtcAzProjection
import tribixbite.cleverkeys.swipe.ctc.CtcCkdtLexicon
import tribixbite.cleverkeys.swipe.ctc.CtcFuzzyRescue
import tribixbite.cleverkeys.swipe.ctc.CtcLayout
import tribixbite.cleverkeys.swipe.ctc.CtcLexiconMerge
import tribixbite.cleverkeys.swipe.ctc.CtcLexiconTrie
import tribixbite.cleverkeys.swipe.ctc.CtcScoringParams
import tribixbite.cleverkeys.swipe.ctc.CtcSwipeDecoder
import tribixbite.cleverkeys.swipe.geometric.CkdtDictionaryReader
import java.io.File
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Fixed, nonpersonal ET key-centre traces, NOT human traces or an accuracy estimate.
 * This builds the CKDT branch of the shipped CTC decoder over the actual ET asset and existing
 * ONNX encoder. Only the Android adapter (Context/asset I/O and display overlay) is bypassed:
 * the CKDT rank scale, merge, a–z collision policy, trie, preset, rescue and score conversion
 * are the production implementations. There are no user words, preferences or imported packs.
 * The golden a–z layout is the model's committed fixture, not an ET phone keyboard measurement.
 *
 * et.cklm is inspected as an OFFLINE candidate separately. ViipioSwipeRanking currently uses
 * static LM evidence for EN only: passing the ET model into its enModel slot must not activate
 * ET static reranking. The comparison below therefore expects unchanged order and zero
 * corrections/regressions; LM coverage is not an ET swipe improvement claim.
 */
class ViipioEtRankingReplayTest {
    private data class Case(val target: String, val previous: String)

    // Both directions are declared before decoding. sis is deliberately the one NEGATIVE
    // dictionary case; too/töö share a projected surface, so only its frequency winner can
    // appear in the decoder slate. Never require both canonical forms to coexist there.
    private val cases = listOf(
        Case("tere", "ütle"), Case("tee", "õige"),
        Case("too", "tuled"), Case("töö", "käib"),
        Case("siis", "ja"), Case("sis", "ja"),
        Case("tule", "ei"), Case("tulla", "appi"),
    )

    private val dictionaryFile = File("src/main/assets/dictionaries/et_enhanced.bin")
    private val lmFile = File("src/main/assets/lm/et.cklm")
    private val modelFile = File(CtcReplayEngine.MODEL_ASSET)

    private fun entries() = dictionaryFile.inputStream().use { CkdtDictionaryReader.readEntries(it) }
    private fun etModel() = StaticContextLm.parse(lmFile.readBytes())
    private fun projected() = CtcAzProjection.projectLexicon(
        CtcLexiconMerge.merge(CtcCkdtLexicon.frequencyPairs(entries()), emptyList(), emptySet())
    )
    private fun displayed(surface: String, display: Map<String, String>) = display[surface] ?: surface

    @Test fun actualAssetsAndProjectionExcludeExactSisWithoutAssumingAccentCoexistence() {
        val entries = entries()
        val dictionaryWords = entries.mapTo(HashSet()) { it.word }
        assertThat(entries.size).isEqualTo(94_999)
        assertThat(cases.map { it.target }.toSet()).containsExactly(
            "tere", "tee", "too", "töö", "siis", "sis", "tule", "tulla"
        )
        assertThat(dictionaryWords).doesNotContain("sis")
        for (word in cases.map { it.target }.filter { it != "sis" }) {
            assertWithMessage("ET CKDT is missing $word").that(dictionaryWords).contains(word)
        }
        assertThat(dictionaryWords).contains("sisu")
        assertThat(dictionaryWords).contains("sisse")
        val projected = projected()
        assertThat(CtcAzProjection.project("too")).isEqualTo(CtcAzProjection.project("töö"))
        val winner = displayed("too", projected.display)
        // The shipped ranks select töö (16) over too (25); both remain CKDT entries.
        assertThat(winner).isEqualTo("töö")
        assertThat(projected.freqs).containsKey("too")
        assertThat(projected.freqs).containsKey("siis")
        assertThat(projected.freqs).doesNotContainKey("sis")
        // No separate 'töö' emission path exists; its display depends on CKDT ranks.
        assertThat(projected.freqs).doesNotContainKey("töö")
        val lm = etModel()
        assertThat(lm.language).isEqualTo("et")
        assertThat(lm.contains("sis")).isFalse()
        assertThat(lm.contains("siis")).isTrue()
        for ((previous, target) in listOf(
            "õige" to "tee", "tuled" to "too", "käib" to "töö",
            "ja" to "siis", "ei" to "tule", "appi" to "tulla",
        )) {
            assertWithMessage("shipped ET CKLM $previous -> $target")
                .that(lm.listedProbability(previous, target)).isGreaterThan(0f)
        }
        assertThat(lm.listedProbability("ja", "sis")).isEqualTo(0f)
        val listed = cases.count { lm.listedProbability(it.previous, it.target) > 0f }
        assertThat(listed).isEqualTo(6)
        println("[viipio-et-assets] ckdt=${entries.size} projection=${projected.freqs.size} " +
            "collisions=${projected.collisions} too/töö winner=$winner " +
            "cklm vocabulary=${lm.vocabSize} pairs=${lm.pairCount} listed=$listed/${cases.size} " +
            "sis=absent (ET static swipe ranking disabled)")
    }

    @Test fun etModelCannotPromoteEvenAnEligibleRivalAtTheScoreFloor() {
        val model = etModel()
        for (score in listOf(650, 649)) {
            val baseline = PredictionResult(listOf("too", "töö"), listOf(1000, score))
            val ranked = ViipioSwipeRanking.rank(baseline, "et", "käib", model)
            assertThat(ranked).isSameInstanceAs(baseline)
            assertThat(ranked.words.zip(ranked.scores))
                .containsExactlyElementsIn(baseline.words.zip(baseline.scores)).inOrder()
        }
    }

    @Test fun fixedEtCtcSlatesRemainUnchangedByNonpersonalRanking() {
        Assume.assumeTrue(
            "ONNX natives absent — ET CKDT/CKLM checks still run; use runPureTests with ONNX natives",
            CtcReplayEngine.ortAvailable(),
        )
        val lm = etModel()
        val lexicon = projected()
        val az = CharArray(26) { 'a' + it }
        val layoutJson = JSONObject(File(CtcReplayEngine.GOLDEN_LAYOUT).readText()).getJSONObject("layout")
        val xCenters = layoutJson.getJSONArray("cx")
        val yCenters = layoutJson.getJSONArray("cy")
        val layout = CtcLayout.of(
            layoutJson.getString("letters").toList(),
            (0 until xCenters.length()).map { xCenters.getDouble(it).toFloat() },
            (0 until yCenters.length()).map { yCenters.getDouble(it).toFloat() },
        )
        assertThat(layout.alphabet.toList()).containsExactlyElementsIn(az.toList()).inOrder()
        val trie = CtcLexiconTrie.loadFromFrequencyMap(az, lexicon.freqs)
        val rescue = CtcFuzzyRescue.fromFrequencies(lexicon.freqs, az.toHashSet())
        val env = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions()
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        // Same XNNPACK-first / CPU-fallback setup as CtcReplayEngine.build and ModelLoader.
        val provider = try {
            options.addXnnpack(mapOf("intra_op_num_threads" to "2"))
            options.setIntraOpNumThreads(2)
            "xnnpack(2)"
        } catch (e: Throwable) {
            options.setIntraOpNumThreads(0)
            "cpu (xnnpack unavailable: ${e::class.java.simpleName})"
        }
        val session = env.createSession(modelFile.readBytes(), options)
        try {
            val decoder = CtcSwipeDecoder(
                OnnxCtcEmissionModel(env, session), layout, trie,
                CtcScoringParams.presetFor("et", topK = CtcReplayEngine.TOP_K),
            )
            assertThat(CtcScoringParams.presetFor("et").lambda).isEqualTo(2.0)
            var decoded = 0
            var multi = 0
            var absent = 0
            var belowFloor = 0
            var atOrAboveFloor = 0
            var baselineTop1 = 0
            var rankedTop1 = 0
            var baselineTop3 = 0
            var rankedTop3 = 0
            var corrections = 0
            var regressions = 0
            for (case in cases) {
                val surface = CtcAzProjection.project(case.target)!!
                val (x, y, t) = CtcTraceShapes.straight(surface, layout)
                val detail = decoder.decodeDetailed(x, y, t) // one decode per fixed case
                val candidates = detail.candidates
                val beamWords = candidates.map { it.word }
                val rescued = rescue.find(detail.greedy, beamWords.toHashSet())
                val beamScores = if (candidates.isEmpty()) emptyList() else {
                    val max = candidates.maxOf { it.finalScore }
                    val exps = candidates.map { exp(it.finalScore - max) }
                    val sum = exps.sum()
                    exps.map { ((it / sum) * 1000.0).roundToInt().coerceIn(0, 1000) }
                }
                val (surfaces, scores) = CtcFuzzyRescue.mergeIntoBeam(
                    beamWords, beamScores, rescued, CtcReplayEngine.TOP_K,
                )
                val words = surfaces.map { displayed(it, lexicon.display) }
                assertWithMessage("${case.target}: decoder word/score alignment")
                    .that(scores.size).isEqualTo(words.size)
                assertThat(words.size).isAtMost(CtcReplayEngine.TOP_K)
                assertWithMessage("${case.target}: duplicate displayed candidates")
                    .that(words.toSet().size).isEqualTo(words.size)
                assertWithMessage("${case.target}: corrupt decoder score")
                    .that(scores.all { it in 0..1000 }).isTrue()
                if (words.isNotEmpty()) decoded++
                if (words.size > 1) multi++

                val original = PredictionResult(words, scores)
                val ranked = ViipioSwipeRanking.rank(original, "et", case.previous, lm)
                // ET has no static swipe LM path and this replay supplies no personal callback.
                assertWithMessage("${case.target}: ET static LM must not promote")
                    .that(ranked).isSameInstanceAs(original)
                assertThat(ranked.words).containsExactlyElementsIn(words).inOrder()
                assertThat(ranked.words.zip(ranked.scores))
                    .containsExactlyElementsIn(words.zip(scores)).inOrder()
                val targetIndex = words.indexOf(case.target)
                if (targetIndex < 0) absent++
                else if (targetIndex > 0 && scores[targetIndex].toDouble() < 0.65 * scores[0]) belowFloor++
                else if (targetIndex > 0) atOrAboveFloor++
                val before1 = words.firstOrNull() == case.target
                val after1 = ranked.words.firstOrNull() == case.target
                if (before1) baselineTop1++
                if (after1) rankedTop1++
                if (case.target in words.take(3)) baselineTop3++
                if (case.target in ranked.words.take(3)) rankedTop3++
                if (!before1 && after1) corrections++
                if (before1 && !after1) regressions++
                println("[viipio-et-ctc] target=${case.target} prev=${case.previous} " +
                    "listed=${lm.listedProbability(case.previous, case.target) > 0f} " +
                    "baseline=${words.take(3)} scores=${scores.take(3)} ranked=${ranked.words.take(3)}")
            }
            assertWithMessage("no ET CTC slate decoded; replay must not pass vacuously")
                .that(decoded).isGreaterThan(0)
            assertThat(baselineTop3).isAtLeast(baselineTop1)
            assertThat(rankedTop3).isAtLeast(rankedTop1)
            assertThat(rankedTop1 - baselineTop1).isEqualTo(corrections - regressions)
            assertThat(corrections).isEqualTo(0)
            assertThat(regressions).isEqualTo(0)
            assertThat(absent + belowFloor + atOrAboveFloor + baselineTop1).isEqualTo(cases.size)
            println("[viipio-et-ctc] synthetic=${cases.size} decoded=$decoded multi=$multi " +
                "absent=$absent belowFloor=$belowFloor atOrAboveFloor=$atOrAboveFloor " +
                "top1=$baselineTop1->$rankedTop1 top3=$baselineTop3->$rankedTop3 " +
                "corrections=$corrections regressions=$regressions provider=$provider " +
                "(not human accuracy; ET static swipe ranking disabled)")
        } finally {
            session.close()
        }
    }
}
