package tribixbite.cleverkeys

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ViipioSwipeRankingTest {
    private val enModel = StaticContextLm.parse(
        StaticContextLmFixtures.encode(
            unigrams = mapOf("want" to 0.01, "to" to 0.02, "too" to 0.01, "other" to 0.01),
            table = mapOf("want" to mapOf("to" to 0.6, "too" to 0.01)),
        )
    )

    @Test fun `Estonian has no corpus promotion without confirmed correction`() {
        val input = PredictionResult(listOf("tere", "tore"), listOf(100, 90))
        assertThat(ViipioSwipeRanking.rank(input, "ET", "eelmine", enModel)).isSameInstanceAs(input)
    }

    @Test fun `English listed continuation can promote an already offered alternative`() {
        val input = PredictionResult(listOf("too", "to", "other"), listOf(100, 80, 50))
        val ranked = ViipioSwipeRanking.rank(input, "EN", "WANT", enModel)
        assertThat(ranked.words).containsExactly("to", "too", "other").inOrder()
        assertThat(ranked.scores).containsExactly(80, 100, 50).inOrder()
    }

    @Test fun `English unlisted target cannot use backoff or a model of another language`() {
        val input = PredictionResult(listOf("to", "other"), listOf(100, 90))
        assertThat(ViipioSwipeRanking.rank(input, "en", "want", enModel)).isSameInstanceAs(input)
        val wrongModel = StaticContextLm.parse(StaticContextLmFixtures.encode(
            mapOf("want" to 0.01, "other" to 0.01),
            mapOf("want" to mapOf("other" to 0.6)), language = "et"
        ))
        assertThat(ViipioSwipeRanking.rank(input, "en", "want", wrongModel)).isSameInstanceAs(input)
        assertThat(ViipioSwipeRanking.rank(input, "en", "want", null)).isSameInstanceAs(input)
    }

    @Test fun `local corrections are opt-in and require two net confirmed choices`() {
        val input = PredictionResult(listOf("tere", "tore"), listOf(100, 80))
        fun choice(forward: Int, reverse: Int) = ViipioSwipeRanking.rank(
            input, "et", null, null, preferred = { from, to ->
                check(from == "tere" && to == "tore") // only the original head competes
                forward - reverse // the caller owns confirmed, bidirectional counts
            }
        )
        assertThat(choice(1, 0)).isSameInstanceAs(input)
        assertThat(choice(3, 2)).isSameInstanceAs(input)
        assertThat(choice(4, 2).words).containsExactly("tore", "tere").inOrder()
        assertThat(choice(2, 4)).isSameInstanceAs(input)
        assertThat(choice(100, 0).words).containsExactly("tore", "tere").inOrder()
    }

    @Test fun `parallel candidate languages and scores move together without inventing candidates`() {
        val input = PredictionResult(listOf("Tere", "Tore", "Hello"), listOf(100, 80, 77), listOf("ET", "et", "en"))
        val ranked = ViipioSwipeRanking.rank(input, "eT", null, null, preferred = { from, to ->
            if (from == "tere" && to == "tore") 2 else 0
        })
        assertThat(ranked.words).containsExactly("Tore", "Tere", "Hello").inOrder()
        assertThat(ranked.scores).containsExactly(80, 100, 77).inOrder()
        assertThat(ranked.languages).containsExactly("et", "ET", "en").inOrder()
        assertThat(ViipioSwipeRanking.rank(input, "en", null, null, preferred = { _, _ -> 100 }))
            .isSameInstanceAs(input)
    }

    @Test fun `malformed alignment and invalid scores fail closed`() {
        val inputs = listOf(
            PredictionResult(listOf("a", "b"), listOf(100)),
            PredictionResult(listOf("a", "b"), listOf(100, 80), listOf("et")),
            PredictionResult(listOf("a", ""), listOf(100, 80)),
            PredictionResult(listOf("a", "b"), listOf(-1, 80)),
            PredictionResult(listOf("a", "b"), listOf(100, 1001)),
            PredictionResult(listOf("a", "b"), listOf(100, 80), listOf("et", "xx")),
            PredictionResult(listOf("a", "b"), listOf(100, 80), listOf("en", "et")),
        )
        for (input in inputs) {
            assertThat(ViipioSwipeRanking.rank(input, "et", null, null, preferred = { _, _ -> 100 }))
                .isSameInstanceAs(input)
        }
    }

    @Test fun `zero score and strong top candidate are not overridden`() {
        val zero = PredictionResult(listOf("tere", "tore"), listOf(0, 0))
        val strong = PredictionResult(listOf("tere", "tore"), listOf(1000, 649))
        val boost: (String, String) -> Int = { _, _ -> 100 }
        assertThat(ViipioSwipeRanking.rank(zero, "et", null, null, preferred = boost)).isSameInstanceAs(zero)
        assertThat(ViipioSwipeRanking.rank(strong, "et", null, null, preferred = boost)).isSameInstanceAs(strong)
        val atBoundary = PredictionResult(listOf("tere", "tore"), listOf(1000, 650))
        assertThat(ViipioSwipeRanking.rank(atBoundary, "et", null, null, preferred = boost).words)
            .containsExactly("tore", "tere").inOrder()
    }

    @Test fun `equal effective evidence keeps original order`() {
        val input = PredictionResult(listOf("tere", "tore"), listOf(100, 100))
        assertThat(ViipioSwipeRanking.rank(input, "et", null, null, preferred = { _, _ -> 0 })).isSameInstanceAs(input)
    }

    @Test fun `unbounded slates and previous words do not reach correction or model lookup`() {
        val longSlate = PredictionResult(List(9) { "word$it" }, List(9) { 80 })
        assertThat(ViipioSwipeRanking.rank(longSlate, "et", null, null,
            preferred = { _, _ -> error("must not query corrections") })).isSameInstanceAs(longSlate)
        val input = PredictionResult(listOf("too", "to"), listOf(100, 80))
        assertThat(ViipioSwipeRanking.rank(input, "en", "x".repeat(100), enModel))
            .isSameInstanceAs(input)
    }

    @Test fun `English context cannot overcome the original score floor`() {
        val input = PredictionResult(listOf("too", "to"), listOf(100, 64))
        assertThat(ViipioSwipeRanking.rank(input, "en", "want", enModel)).isSameInstanceAs(input)
    }

    @Test fun `previous word extraction requires a complete word and a proven left boundary`() {
        assertThat(ViipioSwipeRanking.previousWord("Hello ")).isNull()
        assertThat(ViipioSwipeRanking.previousWord("older hello ")).isEqualTo("hello")
        assertThat(ViipioSwipeRanking.previousWord("older hello")).isNull()
        assertThat(ViipioSwipeRanking.previousWord("older hello. ")).isNull()
        assertThat(ViipioSwipeRanking.previousWord("older hello!  ")).isNull()
        assertThat(ViipioSwipeRanking.previousWord("older hello\n")).isNull()
        assertThat(ViipioSwipeRanking.previousWord("older hello? New ")).isEqualTo("new")
        assertThat(ViipioSwipeRanking.previousWord("prefixof" + "a".repeat(100) + " ")).isNull()
    }
}
