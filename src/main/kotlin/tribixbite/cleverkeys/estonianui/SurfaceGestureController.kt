package tribixbite.cleverkeys.estonianui

/** Raw samples remain in view-local coordinates; no decoding or editor side effects. */
data class SurfaceSample(val x: Float, val y: Float, val timeMs: Long, val key: SurfaceKey?)
sealed class SurfaceGesture {
    data class Tap(val key: SurfaceKey, val language: KeyboardLanguage, val timeMs: Long = 0L) : SurfaceGesture()
    data class Hold(val key: SurfaceKey, val language: KeyboardLanguage) : SurfaceGesture()
    data class LongPress(val value: String, val language: KeyboardLanguage) : SurfaceGesture()
    data class DeleteWord(val language: KeyboardLanguage) : SurfaceGesture()
    data class RepeatBackspace(val language: KeyboardLanguage) : SurfaceGesture()
    data class Swipe(val samples: List<SurfaceSample>, val language: KeyboardLanguage) : SurfaceGesture()
}

class SurfaceGestureController(
    private val geometry: KeyboardGeometry,
    private val dispatch: (SurfaceGesture) -> Unit,
) {
    companion object { const val MAX_SAMPLES = 8_192 }
    private val samples = mutableListOf<SurfaceSample>()
    private var origin: SurfaceKey? = null
    fun down(x: Float, y: Float, timeMs: Long) {
        cancel()
        origin = geometry.hit(x, y)
        if (origin != null) sample(x, y, timeMs)
    }
    fun move(x: Float, y: Float, timeMs: Long) {
        if (origin != null) sample(x, y, timeMs)
    }
    fun up(x: Float, y: Float, timeMs: Long) {
        val start = origin ?: return
        sample(x, y, timeMs)
        val end = samples.last().key
        // A touch just above QWERTY may land on the number-row boundary. Keep a
        // stationary number tap, but start a genuine letter-to-letter glide at its
        // first letter; never send number-row coordinates to the decoder.
        val numberBoundary = !geometry.symbolMode && geometry.numericLayout == null &&
            start.kind == KeyKind.NUMBER && start.row == 0 &&
            samples.first().y >= start.bounds.bottom -
                maxOf(8f, (start.bounds.bottom - start.bounds.top) * 0.28f)
        val letterPath = if (numberBoundary)
            samples.dropWhile { it.key?.kind != KeyKind.LETTER }
                .filter { it.key?.kind == KeyKind.LETTER } else samples
        val first = samples.first()
        val pathStart = letterPath.firstOrNull()
        val minSwipeDistance = maxOf(12f,
            (pathStart?.key?.bounds?.let { it.right - it.left } ?: start.bounds.right - start.bounds.left) * 0.55f)
        val movedFar = samples.any {
            val dx = it.x - first.x
            val dy = it.y - first.y
            dx * dx + dy * dy >= minSwipeDistance * minSwipeDistance
        }
        val letterMovedFar = pathStart != null && letterPath.any {
            val dx = it.x - pathStart.x
            val dy = it.y - pathStart.y
            dx * dx + dy * dy >= minSwipeDistance * minSwipeDistance
        }
        val qualifiedSwipe = !geometry.symbolMode &&
            ((start.kind == KeyKind.LETTER && movedFar) || (numberBoundary && letterMovedFar)) &&
            letterPath.any { it.key?.kind == KeyKind.LETTER && it.key != pathStart?.key }
        val heldAlternative = end == start && samples.all { it.key == start } &&
            timeMs - first.timeMs >= 450 &&
            geometry.language == KeyboardLanguage.ESTONIAN && !geometry.symbolMode &&
            start.kind == KeyKind.LETTER && start.label in setOf("s", "z")
        val deleteWord = start.kind == KeyKind.BACKSPACE &&
            first.x - x > maxOf(40f, geometry.width * 0.12f) &&
            kotlin.math.abs(first.y - y) < (start.bounds.bottom - start.bounds.top) * 1.5f
        val result = when {
            deleteWord -> SurfaceGesture.DeleteWord(geometry.language)
            qualifiedSwipe -> SurfaceGesture.Swipe(letterPath.toList(), geometry.language)
            heldAlternative -> SurfaceGesture.Hold(start, geometry.language)
            end == null -> null
            end == start || !movedFar -> SurfaceGesture.Tap(start, geometry.language, timeMs)
            else -> null
        }
        cancel()
        if (result != null) dispatch(result)
    }
    fun cancel() {
        origin = null
        samples.clear()
    }
    private fun sample(x: Float, y: Float, timeMs: Long) {
        if (samples.size >= MAX_SAMPLES) {
            // Keep endpoints and key-boundary transitions before thinning ordinary moves.
            // Normal gestures never reach this limit; this prevents unbounded raw paths.
            val reduced = ArrayList<SurfaceSample>(MAX_SAMPLES / 2 + 32)
            for (i in samples.indices) {
                val boundary = i == 0 || i == samples.lastIndex ||
                    samples[i].key != samples[i - 1].key || samples[i].key != samples[i + 1].key
                if (boundary || i % 2 == 0) reduced.add(samples[i])
            }
            samples.clear()
            if (reduced.size > MAX_SAMPLES / 2) {
                reduced.forEachIndexed { i, point ->
                    if (i == 0 || i == reduced.lastIndex || i % 2 == 0) samples.add(point)
                }
            } else samples.addAll(reduced)
        }
        samples.add(SurfaceSample(x, y, maxOf(timeMs, samples.lastOrNull()?.timeMs ?: timeMs), geometry.hit(x, y)))
    }
}
