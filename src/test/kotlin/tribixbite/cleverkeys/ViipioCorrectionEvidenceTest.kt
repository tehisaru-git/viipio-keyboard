package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class ViipioCorrectionEvidenceTest {
    private fun correction(id: String, chosen: String) = SwipeCorrectionTracker.Correction(
        chosen, listOf(SwipeCorrectionTracker.SwipeRecord("sis", listOf("sis", "siis"), id)))

    @Test fun directLinkRequiresResolvedOwnedSwipeAndExactlyKeyTypedReplacement() {
        assertEquals("swipe1", ViipioCorrectionEvidence.directUndoId(
            "swipe1", "siis", "siis", correction("swipe1", "siis")))
        assertNull(ViipioCorrectionEvidence.directUndoId("swipe1", "siis", "siis", null))
        assertNull(ViipioCorrectionEvidence.directUndoId("swipe1", "siis", "siis", correction("other", "siis")))
        assertNull(ViipioCorrectionEvidence.directUndoId("swipe1", "siis", "sis", correction("swipe1", "siis")))
        assertNull(ViipioCorrectionEvidence.directUndoId("swipe1", "siis", "siis", correction("swipe1", "mhm")))
    }

    @Test fun ownUndoCallbackSurvivesOnlyAtItsExpectedCaretAndVerifiedAnchor() {
        val expected = 5 to 5
        assertTrue(ViipioCorrectionEvidence.ownUndoSelection(expected, 5, 5,
            "algus", "algus", true, true, expected, 4))
        assertFalse(ViipioCorrectionEvidence.ownUndoSelection(expected, 4, 4,
            "algus", "algus", true, true))
        assertFalse(ViipioCorrectionEvidence.ownUndoSelection(expected, 5, 5,
            "algus", "other", true, true))
        assertFalse(ViipioCorrectionEvidence.ownUndoSelection(expected, 5, 5,
            null, "algus", true, true))
        assertFalse(ViipioCorrectionEvidence.ownUndoSelection(expected, 5, 5,
            "algus", "algus", false, true))
        assertFalse(ViipioCorrectionEvidence.ownUndoSelection(expected, 5, 5,
            "algus", "algus", true, false))
    }

    @Test fun delayedSelectionOfJustUndoneWordDoesNotRevokeVerifiedLiveCaret() {
        val actual = 5 to 5
        assertTrue(ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 9,
            "hello", "hello", true, true, actual, 4))
        assertFalse("actual editor selection moved", ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 9,
            "hello", "hello", true, true, 5 to 9, 4))
        assertFalse("unrelated selected range", ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 10,
            "hello", "hello", true, true, actual, 4))
        assertFalse("moved cursor is not a stale word selection", ViipioCorrectionEvidence.ownUndoSelection(actual, 4, 4,
            "hello", "hello", true, true, actual, 4))
        assertFalse("changed anchor", ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 9,
            "hello", "other", true, true, actual, 4))
        assertFalse("different editor", ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 9,
            "hello", "hello", false, true, actual, 4))
        assertFalse("matching callback must not override a live cursor move",
            ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 5,
                "hello", "hello", true, true, 4 to 4, 4))
        assertFalse("unreadable live cursor is insufficient evidence",
            ViipioCorrectionEvidence.ownUndoSelection(actual, 5, 5,
                "hello", "hello", true, true, null, 4))
    }

    @Test fun extractedTextSelectionAddsTheEditorOffset() {
        assertEquals(105 to 105, ViipioCorrectionEvidence.absoluteSelection(100, 5, 5))
        assertNull(ViipioCorrectionEvidence.absoluteSelection(-1, 5, 5))
        assertNull(ViipioCorrectionEvidence.absoluteSelection(Int.MAX_VALUE, 5, 5))
    }
}
