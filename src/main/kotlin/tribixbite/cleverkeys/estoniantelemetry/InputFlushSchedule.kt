package tribixbite.cleverkeys.estoniantelemetry

/** Called under TelemetryRuntime's input lock: at most one timer and one immediate flush. */
internal class InputFlushSchedule {
    private var delayed = false
    private var immediate = false

    fun requestDelayed(): Boolean {
        if (delayed) return false
        delayed = true
        return true
    }

    fun requestImmediate(pending: Int): Boolean {
        if (pending < 64 || immediate) return false
        immediate = true
        return true
    }

    fun finishDelayed() { delayed = false }
    fun finishImmediate() { immediate = false }
}
