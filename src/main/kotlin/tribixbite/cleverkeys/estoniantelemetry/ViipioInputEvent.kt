package tribixbite.cleverkeys.estoniantelemetry

/** Only confirmed changes made by this keyboard; never editor context, clipboard, or search text. */
enum class InputEventKind(val wire: String) {
    COMMIT("commit"), SWIPE("swipe"), CANDIDATE("candidate"), DELETE("delete"), LANGUAGE("language")
}
enum class InputEventSource(val wire: String) { KEY("key"), SWIPE("swipe"), CANDIDATE("candidate"), SYSTEM("system") }
enum class InputEventOutcome(val wire: String) { APPLIED("applied"), REPLACED("replaced") }

data class ViipioInputEvent(
    val sessionId: String,
    val seq: Long,
    val relativeTimeMs: Long,
    val language: String,
    val kind: InputEventKind,
    val text: String,
    val source: InputEventSource,
    val outcome: InputEventOutcome,
    val textLength: Int = text.length,
)

internal object InputEventPolicy {
    // Narrow printable alphabet for keyboard-owned input, never pasted/clipboard/emoji/search strings.
    private val ownText = Regex("[a-zA-ZõäöüšžÕÄÖÜŠŽ0-9 .,!?;:'\"()–-]{1,64}")
    fun valid(event: ViipioInputEvent): Boolean {
        if (!TelemetryPolicy.id(event.sessionId) || event.seq !in 1..1_000_000_000 ||
            event.relativeTimeMs !in 0..86_400_000 ||
            event.language.lowercase(java.util.Locale.ROOT) !in setOf("et", "et-ee", "en", "en-us", "en-gb") ||
            event.textLength !in 0..64) return false
        return when (event.kind) {
            InputEventKind.COMMIT -> event.source == InputEventSource.KEY &&
                ownText.matches(event.text) && event.textLength == event.text.length
            InputEventKind.SWIPE -> event.source == InputEventSource.SWIPE &&
                ownText.matches(event.text) && event.textLength == event.text.length
            InputEventKind.CANDIDATE -> event.source == InputEventSource.CANDIDATE &&
                ownText.matches(event.text) && event.textLength == event.text.length
            InputEventKind.DELETE -> event.source == InputEventSource.KEY &&
                event.text.isEmpty() && event.textLength in 1..64
            InputEventKind.LANGUAGE -> event.source == InputEventSource.SYSTEM &&
                event.text.isEmpty() && event.textLength == 0
        }
    }
}
