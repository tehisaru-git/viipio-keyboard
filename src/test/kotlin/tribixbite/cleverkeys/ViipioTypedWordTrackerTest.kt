package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class ViipioTypedWordTrackerTest {
    @Test fun explicitLettersOnlyCompleteAtDelimiterAndDoNotCarryAcrossWords() {
        val tracker = ViipioTypedWordTracker()
        for (letter in "mhm") assertTrue(tracker.letter(letter.toString()))
        assertEquals("mhm", tracker.complete())
        assertNull(tracker.complete())
        for (letter in "mhmh") assertTrue(tracker.letter(letter.toString()))
        assertEquals("mhmh", tracker.complete())
    }

    @Test fun backspaceChangesOnlyAnOwnedPendingWord() {
        val tracker = ViipioTypedWordTracker()
        for (letter in "mhmh") tracker.letter(letter.toString())
        assertTrue(tracker.backspace())
        assertEquals("mhm", tracker.complete())
        assertFalse(tracker.backspace())
    }

    @Test fun invalidInputAndExternalChangeInvalidateProvenance() {
        val tracker = ViipioTypedWordTracker()
        tracker.letter("m")
        assertFalse(tracker.letter("paste"))
        assertNull(tracker.complete())
        tracker.letter("m")
        tracker.letter("h")
        tracker.clear()
        assertNull(tracker.complete())
        assertFalse(tracker.letter("🙂"))
    }

    @Test fun lengthIsBoundedAndSingleLetterIsNotOffered() {
        val tracker = ViipioTypedWordTracker()
        assertTrue(tracker.letter("a"))
        assertNull(tracker.complete())
        repeat(40) { assertTrue(tracker.letter("a")) }
        assertFalse(tracker.letter("a"))
        assertNull(tracker.complete())
    }
}
