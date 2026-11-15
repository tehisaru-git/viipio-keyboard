package tribixbite.cleverkeys

/** A typed replacement may link to a swipe only when its immediate undo resolution names it. */
internal object ViipioCorrectionEvidence {
    fun directUndoId(ownedId: String?, keyTyped: String, replacement: String,
                     resolved: SwipeCorrectionTracker.Correction?): String? {
        if (ownedId == null || keyTyped.isEmpty() || keyTyped != replacement ||
            resolved?.chosen != replacement) return null
        return ownedId.takeIf { id -> resolved.rejected.any { it.traceId == id } }
    }

    fun absoluteSelection(offset: Int, start: Int, end: Int): Pair<Int, Int>? {
        val first = offset.toLong() + start
        val last = offset.toLong() + end
        if (offset < 0 || start < 0 || end < start || last > Int.MAX_VALUE) return null
        return first.toInt() to last.toInt()
    }

    /**
     * Android may deliver the selection-of-owned-word callback after our Backspace already
     * deleted that word. In that narrow case trust only a freshly read, collapsed editor
     * selection at the exact verified undo anchor; all other selection changes revoke it.
     */
    fun ownUndoSelection(expected: Pair<Int, Int>?, start: Int, end: Int,
                         anchor: String?, readback: String?, sameEditor: Boolean,
                         sameConnection: Boolean, liveSelection: Pair<Int, Int>? = null,
                         undoneWordLength: Int = 0): Boolean {
        if (!sameEditor || !sameConnection || anchor == null || readback == null ||
            readback.takeLast(SwipeCorrectionTracker.ANCHOR_WINDOW) != anchor ||
            expected == null || liveSelection != expected) return false
        if (expected == (start to end) && start == end) return true
        return expected.first == expected.second && liveSelection == expected &&
            undoneWordLength in 1..40 && start == expected.first &&
            end > start && end - start == undoneWordLength
    }
}
