from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / 'src/main/kotlin/tribixbite/cleverkeys/EstonianSwipeService.kt'


class ManualDiagnosticsRouteTest(unittest.TestCase):
    def test_active_service_only_records_after_verified_completed_space(self):
        text = SOURCE.read_text(encoding='utf-8')
        commit = text.split('private fun commitManualValue(value: String): Boolean {', 1)[1].split('private val taps =', 1)[0]
        self.assertTrue('typedWord.completeAfterSpace(' in commit, 'missing verified completion route')
        self.assertTrue('TelemetryRuntime.recordManualWord(' in commit, 'missing opt-in manual event')
        self.assertLess(commit.index('if (!success)'), commit.index('TelemetryRuntime.recordManualWord('))

    def test_active_swipe_path_passes_decoder_origin_and_aligned_scores(self):
        text = SOURCE.read_text(encoding='utf-8')
        self.assertTrue('SwipeDecoderSource.CTC' in text, 'CTC source must be explicit')
        self.assertTrue('SwipeDecoderSource.GEOMETRIC' in text, 'fallback source must be explicit')
        self.assertTrue('candidateScores = ' in text, 'candidate scores must be attached by the service')

    def test_cold_start_keeps_verified_offer_until_dictionary_is_ready(self):
        text = SOURCE.read_text(encoding='utf-8')
        self.assertTrue('deferredPersonalOffer' in text, 'verified word is discarded before dictionary load')
        self.assertTrue('resolveDeferredPersonalOffer()' in text, 'dictionary load must resolve pending offer')

    def test_active_service_offers_explicit_personal_word_not_passive_add(self):
        text = SOURCE.read_text(encoding='utf-8')
        self.assertTrue('manualCompletions?.contains(' in text, 'missing dictionary membership gate')
        self.assertTrue('personalWords.add(lang, offer.word)' in text, 'missing explicit add click')
        self.assertTrue('personalOfferRow' in text, 'missing inline row')


if __name__ == '__main__':
    unittest.main()
