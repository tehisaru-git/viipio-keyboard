package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.HorizontalScrollView
import org.robolectric.RuntimeEnvironment
import android.view.View
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.Looper
import java.util.concurrent.TimeUnit
import org.robolectric.Shadows.shadowOf
import tribixbite.cleverkeys.swipeui.SwipeSuggestionSession
import tribixbite.cleverkeys.PredictionResult
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnectionWrapper
import tribixbite.cleverkeys.estonianui.*
import tribixbite.cleverkeys.estonianemoji.EmojiPanelView
import tribixbite.cleverkeys.a11y.KeyboardGeometry as AdapterGeometry

@RunWith(RobolectricTestRunner::class)
class ViipioInputMethodServiceTest {
    private fun tap(geometry: KeyboardGeometry, kind: KeyKind, label: String? = null, timeMs: Long = 0L) =
        SurfaceGesture.Tap(geometry.keys.first { it.kind == kind && (label == null || it.label == label) }, geometry.language, timeMs)

    @Test fun tapLettersShiftBackspaceSpaceAndEnter() {
        val texts = mutableListOf<String>()
        var deletes = 0
        var enters = 0
        val input = EstonianTapInput({ texts.add(it) }, { deletes++ }, { enters++ })
        val board = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        input.accept(tap(board, KeyKind.LETTER, "õ"))
        input.accept(tap(board, KeyKind.SHIFT))
        input.accept(tap(board, KeyKind.LETTER, "ä"))
        input.accept(tap(board, KeyKind.LETTER, "ü"))
        input.accept(tap(board, KeyKind.BACKSPACE))
        input.accept(tap(board, KeyKind.SPACE))
        input.accept(tap(board, KeyKind.ENTER))
        assertEquals(listOf("õ", "Ä", "ü", " "), texts)
        assertEquals(1, deletes)
        assertEquals(1, enters)
    }

    @Test fun doubleShiftLocksUppercaseUntilShiftIsTappedAgain() {
        val text = mutableListOf<String>()
        val input = EstonianTapInput(text::add, {}, {})
        val et = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 1000L))
        assertEquals(ShiftMode.ONCE, input.shiftMode)
        input.accept(tap(et, KeyKind.LETTER, "a", 1100L))
        assertEquals(ShiftMode.OFF, input.shiftMode)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 2000L))
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 2250L))
        assertEquals(ShiftMode.LOCKED, input.shiftMode)
        input.accept(tap(et, KeyKind.LETTER, "õ", 2500L))
        input.accept(tap(et, KeyKind.LETTER, "b", 3000L))
        assertEquals(listOf("A", "Õ", "B"), text)
        assertEquals(ShiftMode.LOCKED, input.shiftMode)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 3500L))
        assertEquals(ShiftMode.OFF, input.shiftMode)
        input.accept(tap(et, KeyKind.LETTER, "c", 3700L))
        assertEquals("c", text.last())
    }

    @Test fun automaticSentenceShiftCanBeSuppressedWithoutDisablingManualShift() {
        val text = mutableListOf<String>()
        val input = EstonianTapInput(text::add, {}, {})
        val et = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        input.setAutomaticShift(true)
        assertEquals(ShiftMode.ONCE, input.shiftMode)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 1000L))
        assertEquals(ShiftMode.OFF, input.shiftMode)
        input.setAutomaticShift(true)
        assertEquals("manual OFF must not be overridden", ShiftMode.OFF, input.shiftMode)
        input.accept(tap(et, KeyKind.LETTER, "a", 1200L))
        assertEquals("a", text.last())
        input.setAutomaticShift(false)
        input.accept(tap(et, KeyKind.PERIOD))
        input.accept(tap(et, KeyKind.SPACE))
        input.setAutomaticShift(true)
        assertEquals(ShiftMode.ONCE, input.shiftMode)
        input.accept(tap(et, KeyKind.LETTER, "b", 2000L))
        assertEquals("B", text.last())
        assertEquals(ShiftMode.OFF, input.shiftMode)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 2500L))
        input.accept(tap(et, KeyKind.LETTER, "õ", 2800L))
        assertEquals("Õ", text.last())
    }

    @Test fun swipeCandidatesUseTheSameCaseAsTapAndOnlySuccessfulSwipeConsumesShift() {
        val input = EstonianTapInput({ true }, {}, {})
        val et = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        val raw = PredictionResult(listOf("tere", "tõre", "õun"), listOf(90, 70, 50))
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 1000L))
        val first = input.caseCandidates(raw)
        assertEquals(listOf("Tere", "Tõre", "Õun"), first.words)
        assertEquals(raw.scores, first.scores)
        assertEquals(ShiftMode.ONCE, input.shiftMode)
        input.consumeSwipeShift()
        assertEquals(ShiftMode.OFF, input.shiftMode)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 2000L))
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 2200L))
        assertEquals(listOf("TERE", "TÕRE", "ÕUN"), input.caseCandidates(raw).words)
        input.consumeSwipeShift()
        assertEquals(ShiftMode.LOCKED, input.shiftMode)
    }

    @Test fun rejectedEditorWriteDoesNotConsumeOneShotShift() {
        var editorAccepts = false
        val written = mutableListOf<String>()
        val input = EstonianTapInput({ value ->
            if (!editorAccepts) false else { written.add(value); true }
        }, {}, {})
        val et = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        input.accept(tap(et, KeyKind.SHIFT, timeMs = 1000L))
        input.accept(tap(et, KeyKind.LETTER, "a", 1100L))
        assertEquals(ShiftMode.ONCE, input.shiftMode)
        assertTrue(written.isEmpty())
        editorAccepts = true
        input.accept(tap(et, KeyKind.LETTER, "õ", 1200L))
        assertEquals(listOf("Õ"), written)
        assertEquals(ShiftMode.OFF, input.shiftMode)
    }

    @Test fun activeImeReflectsManualShiftAndCapsLockOnVisibleSurface() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val view = layout.getChildAt(2) as KeyboardSurfaceView
        service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        service.onStartInputView(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        view.layout(0, 0, 330, 250)
        val shift = view.geometry.keys.first { it.kind == KeyKind.SHIFT }
        view.onGesture!!.invoke(SurfaceGesture.Tap(shift, view.language, 1000L))
        assertEquals(ShiftMode.ONCE, view.shiftMode)
        view.onGesture!!.invoke(SurfaceGesture.Tap(shift, view.language, 1200L))
        assertEquals(ShiftMode.LOCKED, view.shiftMode)
        view.onGesture!!.invoke(SurfaceGesture.Tap(shift, view.language, 1600L))
        assertEquals(ShiftMode.OFF, view.shiftMode)
    }

    @Test fun switchLanguageRejectsOldSurfaceEventsAndResetsShift() {
        val texts = mutableListOf<String>()
        val input = EstonianTapInput({ texts.add(it) }, {}, {})
        val et = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        val en = KeyboardGeometry(301, 235, KeyboardLanguage.ENGLISH)
        input.accept(tap(et, KeyKind.SHIFT))
        input.switchLanguage()
        input.accept(tap(et, KeyKind.LETTER, "õ"))
        input.accept(tap(en, KeyKind.LETTER, "a"))
        assertEquals(KeyboardLanguage.ENGLISH, input.language)
        assertEquals(listOf("a"), texts)
    }

    @Test fun symbolsAndEmojiDoNotChangeLanguage() {
        val texts = mutableListOf<String>()
        var symbols = 0
        var emoji = 0
        val input = EstonianTapInput({ texts.add(it) }, {}, {}, { symbols++ }, { emoji++ })
        val letters = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN)
        val page = KeyboardGeometry(301, 235, KeyboardLanguage.ESTONIAN, symbolMode = true)
        input.accept(tap(letters, KeyKind.SYMBOLS))
        input.accept(tap(page, KeyKind.SYMBOL, "€"))
        input.accept(tap(page, KeyKind.EMOJI))
        assertEquals(1, symbols)
        assertEquals(1, emoji)
        assertEquals(listOf("€"), texts)
        assertEquals(KeyboardLanguage.ESTONIAN, input.language)
    }

    private class Buffer : SwipeSuggestionSession.Sink {
        var text = ""
        var composing = ""
        override fun beforeCursor(): String = text
        override fun setComposingText(value: String): Boolean {
            if (composing.isNotEmpty()) text = text.removeSuffix(composing)
            text += value
            composing = value
            return true
        }
        override fun finishComposingText(): Boolean { composing = ""; return true }
        override fun commitText(value: String): Boolean { text += value; return true }
    }

    @Test fun suggestionControllerComposesAndRendersThreeSelectableSlots() {
        val buffer = Buffer()
        val controller = EstonianSuggestionController()
        controller.start(buffer, allowed = true)
        val ticket = controller.begin()!!
        assertTrue(controller.accept(ticket, PredictionResult(listOf("tere", "tore", "tõre", "terve"), listOf(4, 3, 2, 1))))
        assertEquals(listOf("tere", "tore", "tõre"), controller.words)
        assertEquals("tere", buffer.text)
        assertTrue(controller.choose(2))
        assertEquals("tõre", buffer.text)
        assertEquals("tõre", buffer.composing)
    }

    @Test fun suggestionControllerConsecutiveSwipesAndExternalCursorRevoke() {
        val buffer = Buffer()
        val controller = EstonianSuggestionController()
        controller.start(buffer, allowed = true)
        controller.accept(controller.begin()!!, PredictionResult(listOf("tere", "tore"), listOf(2, 1)))
        controller.accept(controller.begin()!!, PredictionResult(listOf("maailm", "maa"), listOf(2, 1)))
        assertEquals("tere maailm", buffer.text)
        assertEquals(listOf("maailm", "maa"), controller.words)
        controller.cursorChanged()
        assertFalse(controller.choose(1))
        assertTrue(controller.words.isEmpty())
    }

    @Test fun suggestionControllerDoesNotCorrectExternallyMutatedText() {
        val buffer = Buffer()
        val controller = EstonianSuggestionController()
        controller.start(buffer, allowed = true)
        controller.accept(controller.begin()!!, PredictionResult(listOf("tere", "tore"), listOf(2, 1)))
        buffer.text = "different"
        assertFalse(controller.choose(1))
        assertEquals("different", buffer.text)
        assertTrue(controller.words.isEmpty())
    }

    @Test fun suggestionControllerClearsOnTapFieldAndRejectsStaleDecode() {
        val buffer = Buffer()
        val controller = EstonianSuggestionController()
        controller.start(buffer, allowed = true)
        val pending = controller.begin()!!
        controller.clear()
        assertFalse(controller.accept(pending, PredictionResult(listOf("leak"), listOf(1))))
        assertEquals("", buffer.text)
        controller.start(Buffer(), allowed = false)
        assertNull(controller.begin())
        assertTrue(controller.words.isEmpty())
    }

    @Test fun longPressCommitsOnlyChosenCharacterInMatchingLanguageAndRespectsShift() {
        val typed = mutableListOf<String>()
        val input = EstonianTapInput(typed::add, {}, {})
        input.acceptLongPress("?", KeyboardLanguage.ESTONIAN)
        input.acceptLongPress("ğ", KeyboardLanguage.ENGLISH)
        val geometry = KeyboardGeometry(330, 250, KeyboardLanguage.ESTONIAN)
        input.accept(SurfaceGesture.Tap(geometry.keys.first { it.kind == KeyKind.SHIFT }, KeyboardLanguage.ESTONIAN))
        input.acceptLongPress("ğ", KeyboardLanguage.ESTONIAN)
        assertEquals(listOf("?", "Ğ"), typed)
    }

    @Test fun emojiBrowseAndSearchReuseKeyboardHeightWithoutHidingEditor() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val bar = layout.getChildAt(0)
        val panel = layout.getChildAt(1) as EmojiPanelView
        val keys = layout.getChildAt(2) as KeyboardSurfaceView
        val base = bar.layoutParams.height + keys.layoutParams.height
        panel.visibility = View.VISIBLE
        service.resizeForEmoji(true, false)
        assertEquals(View.GONE, bar.visibility)
        assertEquals(View.GONE, keys.visibility)
        assertEquals(base, panel.layoutParams.height)
        service.resizeForEmoji(true, true)
        assertEquals(View.GONE, bar.visibility)
        assertEquals(View.VISIBLE, keys.visibility)
        assertEquals(base, panel.layoutParams.height + keys.layoutParams.height)
        service.resizeForEmoji(false)
        panel.visibility = View.GONE
        assertEquals(View.VISIBLE, bar.visibility)
        assertEquals(base, bar.layoutParams.height + keys.layoutParams.height)
    }

    @Test fun longCandidatesStayInOneStableRowAndAreFullyReachable() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val strip = layout.getChildAt(0) as FrameLayout
        val row = strip.getChildAt(1) as LinearLayout
        row.visibility = View.VISIBLE
        val words = listOf("funktsionaalsus", "funktsionaalseid", "funktsionaalsuses")
        val track = (row.getChildAt(1) as HorizontalScrollView).getChildAt(0) as LinearLayout
        val buttons = (0..2).map { track.getChildAt(it * 2) as CandidateLabelView }
        buttons.zip(words).forEach { (button, word) -> button.bind(word) }
        val width = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY)
        strip.measure(width, height)
        strip.layout(0, 0, 720, 88)
        buttons.forEachIndexed { index, button ->
            assertEquals(words[index], button.contentDescription)
            assertEquals(1, button.lineCount)
            assertTrue(button.measuredHeight <= 88)
            assertTrue(button.isClickable)
        }
        assertEquals(88, strip.measuredHeight)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun longCandidateWordsStayWholeAndTheStripScrollsSideways() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val bar = layout.getChildAt(0) as FrameLayout
        val candidates = bar.getChildAt(1) as LinearLayout
        candidates.visibility = View.VISIBLE
        val scroll = candidates.getChildAt(1) as HorizontalScrollView
        val strip = scroll.getChildAt(0) as LinearLayout
        val first = strip.getChildAt(0) as CandidateLabelView
        val second = strip.getChildAt(2) as CandidateLabelView
        val third = strip.getChildAt(4) as CandidateLabelView
        val words = listOf("rahvusvahelistumine", "ebaproportsionaalsusega", "mitmetähenduslikkusest")
        listOf(first, second, third).forEachIndexed { index, button -> button.bind(words[index]) }
        strip.getChildAt(1).visibility = View.VISIBLE
        strip.getChildAt(3).visibility = View.VISIBLE
        val width = View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY)
        candidates.measure(width, height)
        candidates.layout(0, 0, 360, 88)
        assertEquals(words[0], first.text.toString())
        assertEquals(words[2], third.contentDescription.toString())
        assertNull("a long word must not be silently ellipsized", first.ellipsize)
        assertTrue("candidate viewport must be measured: ${scroll.width}", scroll.width > 0)
        assertTrue("candidate strip must be measured: ${strip.width}", strip.width > 0)
        assertTrue("the complete alternatives need horizontal overflow: ${strip.width} vs ${scroll.width}",
            strip.width > scroll.width)
        scroll.scrollTo(180, 0)
        assertTrue(scroll.scrollX > 0)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun shortCandidatesCannotScrollWhenAllWordsFitTheRow() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val candidates = (layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout
        candidates.visibility = View.VISIBLE
        val scroll = candidates.getChildAt(1) as HorizontalScrollView
        val track = scroll.getChildAt(0) as LinearLayout
        listOf("ja", "on", "ei").forEachIndexed { index, word ->
            (track.getChildAt(index * 2) as CandidateLabelView).bind(word)
        }
        track.getChildAt(1).visibility = View.VISIBLE
        track.getChildAt(3).visibility = View.VISIBLE
        val width = View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY)
        candidates.measure(width, height)
        candidates.layout(0, 0, 320, 88)
        assertTrue("short words should fit without overflow", track.width <= scroll.width)
        assertEquals("fitting words should use the whole row", scroll.width, track.width)
        val labels = (0..2).map { track.getChildAt(it * 2) as CandidateLabelView }
        assertTrue("short words should be spread evenly: ${labels.map { it.width }}",
            labels.maxOf { it.width } - labels.minOf { it.width } <= 2)
        assertTrue("each short word should receive a wide tap slot", labels.all { it.width >= (scroll.width - 2) / 3 - 2 })
        scroll.scrollTo(100, 0)
        assertEquals("a fitting row must not move", 0, scroll.scrollX)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun draggingAnOverflowingRowNeverClicksAWord() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val row = (layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout
        row.visibility = View.VISIBLE
        val scroll = row.getChildAt(1) as HorizontalScrollView
        val track = scroll.getChildAt(0) as LinearLayout
        var selected = 0
        listOf("rahvusvahelistumine", "ebaproportsionaalsusega", "mitmetähenduslikkusest")
            .forEachIndexed { index, word ->
                (track.getChildAt(index * 2) as CandidateLabelView).apply {
                    bind(word)
                    setOnClickListener { selected++ }
                }
            }
        track.getChildAt(1).visibility = View.VISIBLE
        track.getChildAt(3).visibility = View.VISIBLE
        row.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY))
        row.layout(0, 0, 360, 88)
        fun send(action: Int, x: Float, time: Long) {
            MotionEvent.obtain(0, time, action, x, 44f, 0).also {
                scroll.dispatchTouchEvent(it)
                it.recycle()
            }
        }
        send(MotionEvent.ACTION_DOWN, 230f, 1)
        send(MotionEvent.ACTION_MOVE, 130f, 60)
        send(MotionEvent.ACTION_MOVE, 30f, 120)
        send(MotionEvent.ACTION_UP, 30f, 150)
        assertTrue("overflow should move with a finger", scroll.scrollX > 0)
        assertEquals("dragging must not select a word", 0, selected)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun rebindingFitOverflowFitResetsScrollAndSpreadsShortWordsAgain() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val row = (layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout
        row.visibility = View.VISIBLE
        val scroll = row.getChildAt(1) as HorizontalScrollView
        val track = scroll.getChildAt(0) as LinearLayout
        track.getChildAt(1).visibility = View.VISIBLE
        track.getChildAt(3).visibility = View.VISIBLE
        fun bind(words: List<String>, viewport: Int) {
            words.forEachIndexed { index, word ->
                (track.getChildAt(index * 2) as CandidateLabelView).bind(word)
            }
            row.measure(View.MeasureSpec.makeMeasureSpec(viewport, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY))
            row.layout(0, 0, viewport, 88)
        }
        bind(listOf("ja", "on", "ei"), 320)
        assertEquals(scroll.width, track.width)
        bind(listOf("rahvusvahelistumine", "ebaproportsionaalsusega", "mitmetähenduslikkusest"), 240)
        assertTrue(track.width > scroll.width)
        scroll.scrollTo(120, 0)
        assertTrue(scroll.scrollX > 0)
        bind(listOf("ja", "on", "ei"), 320)
        assertEquals(scroll.width, track.width)
        assertEquals(0, scroll.scrollX)
        val labels = (0..2).map { track.getChildAt(it * 2) as CandidateLabelView }
        assertTrue(labels.maxOf { it.width } - labels.minOf { it.width } <= 2)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun fittingMixedLengthWordsGiveTheLongWordItsFullNaturalWidth() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val row = (layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout
        row.visibility = View.VISIBLE
        val scroll = row.getChildAt(1) as HorizontalScrollView
        val track = scroll.getChildAt(0) as LinearLayout
        listOf("ja", "rahvusvahelistumine", "ei").forEachIndexed { index, word ->
            (track.getChildAt(index * 2) as CandidateLabelView).bind(word)
        }
        track.getChildAt(1).visibility = View.VISIBLE
        track.getChildAt(3).visibility = View.VISIBLE
        val labels = (0..2).map { track.getChildAt(it * 2) as CandidateLabelView }
        val naturalWidths = labels.map { label ->
            label.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY))
            label.measuredWidth
        }
        val separatorWidths = track.getChildAt(1).layoutParams.width + track.getChildAt(3).layoutParams.width
        val natural = naturalWidths.sum() + separatorWidths
        val rowWidth = natural + 24 + row.getChildAt(0).layoutParams.width
        row.measure(View.MeasureSpec.makeMeasureSpec(rowWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(88, View.MeasureSpec.EXACTLY))
        row.layout(0, 0, rowWidth, 88)
        assertTrue("test precondition: all complete words fit naturally", natural <= scroll.width)
        val long = labels[1]
        val longNatural = naturalWidths[1]
        assertTrue("test precondition: long word exceeds an equal slot",
            longNatural > (scroll.width - separatorWidths) / 3)
        assertTrue("fitting mixed row must not clip its long word: ${long.width} < $longNatural",
            long.width >= longNatural)
        assertEquals(scroll.width, track.width)
        scroll.scrollTo(100, 0)
        assertEquals(0, scroll.scrollX)
    }

    private fun attachInlineHintWindow(layout: LinearLayout): android.app.Activity {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        activity.setContentView(root)
        root.addView(layout, FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.BOTTOM))
        val width = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY)
        root.measure(width, height)
        root.layout(0, 0, 720, 1600)
        assertTrue("hint anchor must have a real window", layout.isAttachedToWindow)
        assertTrue("hint anchor must be laid out", layout.getChildAt(0).width > 0)
        return activity
    }

    private fun activeInlineHintPopup(service: ViipioInputMethodService): android.widget.PopupWindow =
        ViipioInputMethodService::class.java.getDeclaredField("inlineHintPopup")
            .apply { isAccessible = true }.get(service) as? android.widget.PopupWindow
            ?: error("inline hint popup was not displayed")

    @Test fun firstAddHintIsVisibleInsideTheImeWithoutReplacingCandidates() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val prefs = service.applicationContext.getSharedPreferences(
            "viipio_inline_word_guidance", android.content.Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        val layout = service.onCreateInputView() as LinearLayout
        val candidatesBar = layout.getChildAt(0)
        attachInlineHintWindow(layout)
        val info = EditorInfo().apply {
            packageName = "org.example.synthetic"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val connection = EditText(service).onCreateInputConnection(info)
        val offer = ViipioInlinePersonalOffer("sii", KeyboardLanguage.ESTONIAN,
            info, connection, 3, completed = false)
        ViipioInputMethodService::class.java.getDeclaredMethod("maybeShowInlineWordHint",
            ViipioInlinePersonalOffer::class.java).apply { isAccessible = true }.invoke(service, offer)
        assertTrue("fresh eligible offer must claim the durable hint flag",
            prefs.getBoolean("shown", false))
        val popup = activeInlineHintPopup(service)
        val hint = popup.contentView as TextView
        assertTrue("hint must be visible even when OS suppresses Toasts", popup.isShowing)
        assertEquals("viipio-inline-first-hint", hint.tag)
        assertTrue(hint.text.toString().contains("Tap + Add"))
        assertTrue(hint.text.toString().contains("undo"))
        assertEquals("popup must not add a row to the IME", 3, layout.childCount)
        assertSame("suggestion row must remain present", candidatesBar, layout.getChildAt(0))
    }

    @Test fun firstAddHintIsRemovedWhenTheImeIsDestroyed() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val prefs = service.applicationContext.getSharedPreferences(
            "viipio_inline_word_guidance", android.content.Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        val layout = service.onCreateInputView() as LinearLayout
        attachInlineHintWindow(layout)
        val info = EditorInfo().apply {
            packageName = "org.example.synthetic"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val offer = ViipioInlinePersonalOffer("sii", KeyboardLanguage.ESTONIAN,
            info, EditText(service).onCreateInputConnection(info), 3, completed = false)
        ViipioInputMethodService::class.java.getDeclaredMethod("maybeShowInlineWordHint",
            ViipioInlinePersonalOffer::class.java).apply { isAccessible = true }.invoke(service, offer)
        val popup = activeInlineHintPopup(service)
        assertTrue(popup.isShowing)
        // Robolectric's bare IME has no window root; only its framework super teardown throws.
        val teardown = runCatching { service.onDestroy() }.exceptionOrNull()
        assertTrue(teardown is NullPointerException &&
            teardown.message.orEmpty().contains("mRootView"))
        assertFalse("destroyed IME must dismiss transient popup and callback", popup.isShowing)
    }

    @Test fun firstAddHintSurvivesTheNextOwnedLetterRefresh() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val prefs = service.applicationContext.getSharedPreferences(
            "viipio_inline_word_guidance", android.content.Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        val layout = service.onCreateInputView() as LinearLayout
        attachInlineHintWindow(layout)
        val info = EditorInfo().apply {
            packageName = "org.example.synthetic"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val connection = EditText(service).onCreateInputConnection(info)
        val offer = ViipioInlinePersonalOffer("sii", KeyboardLanguage.ESTONIAN,
            info, connection, 3, completed = false)
        ViipioInputMethodService::class.java.getDeclaredField("editor")
            .apply { isAccessible = true }.set(service, info)
        val slot = ViipioInputMethodService::class.java.getDeclaredField("personalOffer")
            .apply { isAccessible = true }
        slot.set(service, offer)
        ViipioInputMethodService::class.java.getDeclaredMethod("maybeShowInlineWordHint",
            ViipioInlinePersonalOffer::class.java).apply { isAccessible = true }.invoke(service, offer)
        val popup = activeInlineHintPopup(service)
        assertTrue(popup.isShowing)
        slot.set(service, null) // handleGesture temporarily clears the prior offer on each own key.
        ViipioInputMethodService::class.java.getDeclaredMethod("renderSuggestions")
            .apply { isAccessible = true }.invoke(service)
        assertTrue("next letter must not consume the one-time guidance", popup.isShowing)
    }

    @Test fun firstAddHintIsHiddenBeforeSwitchingToPasswordEditor() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val prefs = service.applicationContext.getSharedPreferences(
            "viipio_inline_word_guidance", android.content.Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        val layout = service.onCreateInputView() as LinearLayout
        attachInlineHintWindow(layout)
        val info = EditorInfo().apply {
            packageName = "org.example.synthetic"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        service.onStartInput(info, false)
        val offer = ViipioInlinePersonalOffer("sii", KeyboardLanguage.ESTONIAN,
            info, EditText(service).onCreateInputConnection(info), 3, completed = false)
        ViipioInputMethodService::class.java.getDeclaredMethod("maybeShowInlineWordHint",
            ViipioInlinePersonalOffer::class.java).apply { isAccessible = true }.invoke(service, offer)
        val popup = activeInlineHintPopup(service)
        assertTrue(popup.isShowing)
        val password = EditorInfo().apply {
            packageName = "org.example.synthetic.secret"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        service.onStartInput(password, false)
        assertFalse("first-use hint must not leak into a password editor", popup.isShowing)
    }

    @Test fun firstAddHintDoesNotIncreaseImeMeasuredHeight() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val prefs = service.applicationContext.getSharedPreferences(
            "viipio_inline_word_guidance", android.content.Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        val layout = service.onCreateInputView() as LinearLayout
        attachInlineHintWindow(layout)
        val width = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY)
        val available = View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.AT_MOST)
        layout.measure(width, available)
        val originalHeight = layout.measuredHeight
        val info = EditorInfo().apply {
            packageName = "org.example.synthetic"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val offer = ViipioInlinePersonalOffer("sii", KeyboardLanguage.ESTONIAN,
            info, EditText(service).onCreateInputConnection(info), 3, completed = false)
        ViipioInputMethodService::class.java.getDeclaredMethod("maybeShowInlineWordHint",
            ViipioInlinePersonalOffer::class.java).apply { isAccessible = true }.invoke(service, offer)
        assertTrue("height check must exercise a displayed popup", activeInlineHintPopup(service).isShowing)
        layout.measure(width, available)
        assertEquals("first-use help must not move the IME boundary", originalHeight,
            layout.measuredHeight)
    }

    @Test fun candidateStripKeepsThreeLabelsBesidePinnedInlineAddAction() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val bar = layout.getChildAt(0) as FrameLayout
        val candidates = bar.getChildAt(1) as LinearLayout
        val action = (0 until candidates.childCount).map { candidates.getChildAt(it) }
            .firstOrNull { it.tag == "viipio-inline-add-action" }
        assertNotNull("unknown own-key word needs a pinned Add/Undo action", action)
        val dp = service.resources.displayMetrics.density
        assertTrue(action!!.layoutParams.width / dp >= 48f)
        assertEquals(3, candidates.childCount)
        val track = (candidates.getChildAt(1) as HorizontalScrollView).getChildAt(0) as LinearLayout
        assertEquals(5, track.childCount)
        assertTrue((0..2).all { track.getChildAt(it * 2) is CandidateLabelView })
        assertEquals((48 * dp).toInt(), bar.layoutParams.height)
    }

    @Test fun visibleInlineAddActionNamesExactWordAndLanguageAlongsideCandidates() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val candidates = ((layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout)
        val action = candidates.getChildAt(2) as TextView
        val controller = ViipioInputMethodService::class.java.getDeclaredField("suggestions")
            .apply { isAccessible = true }.get(service) as EstonianSuggestionController
        EstonianSuggestionController::class.java.getDeclaredField("words")
            .apply { isAccessible = true }.set(controller, listOf("first", "second", "third"))
        val info = EditorInfo().apply { packageName = "org.example.synthetic"; inputType = InputType.TYPE_CLASS_TEXT }
        val connection = EditText(service).onCreateInputConnection(info)
        val slot = ViipioInputMethodService::class.java.getDeclaredField("personalOffer")
            .apply { isAccessible = true }
        val render = ViipioInputMethodService::class.java.getDeclaredMethod("renderSuggestions")
            .apply { isAccessible = true }
        slot.set(service, ViipioInlinePersonalOffer("mhm", KeyboardLanguage.ESTONIAN,
            info, connection, 3, completed = false))
        render.invoke(service)
        assertEquals(View.VISIBLE, action.visibility)
        assertTrue(action.text.toString().contains("mhm"))
        assertTrue(action.text.toString().contains("ET"))
        assertTrue(action.contentDescription.toString().contains("mhm"))
        slot.set(service, ViipioInlinePersonalOffer("zoop", KeyboardLanguage.ENGLISH,
            info, connection, 4, completed = false))
        render.invoke(service)
        assertTrue(action.text.toString().contains("zoop"))
        assertTrue(action.text.toString().contains("EN"))
    }

    @Test fun longInlineLabelDoesNotEllipsizeTheWordOrLanguage() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val candidates = (layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout
        val action = candidates.getChildAt(2) as TextView
        val controller = ViipioInputMethodService::class.java.getDeclaredField("suggestions")
            .apply { isAccessible = true }.get(service) as EstonianSuggestionController
        EstonianSuggestionController::class.java.getDeclaredField("words")
            .apply { isAccessible = true }.set(controller, listOf("one", "two", "three"))
        val info = EditorInfo().apply { packageName = "org.example.synthetic"; inputType = InputType.TYPE_CLASS_TEXT }
        val connection = EditText(service).onCreateInputConnection(info)
        val word = "W".repeat(40)
        ViipioInputMethodService::class.java.getDeclaredField("personalOffer")
            .apply { isAccessible = true }.set(service, ViipioInlinePersonalOffer(word,
                KeyboardLanguage.ESTONIAN, info, connection, 40, completed = false))
        ViipioInputMethodService::class.java.getDeclaredMethod("renderSuggestions")
            .apply { isAccessible = true }.invoke(service)
        val w = action.layoutParams.width
        val h = action.layoutParams.height
        action.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        action.layout(0, 0, w, h)
        val lines = action.layout ?: error("text layout missing")
        val last = lines.lineCount - 1
        assertTrue(action.text.toString().contains(word))
        assertTrue(action.text.toString().endsWith("ET"))
        assertEquals(action.text.length, lines.getLineEnd(last))
        assertEquals(0, lines.getEllipsisCount(last))
        assertTrue("last line must remain inside the 48dp tap target",
            lines.getLineBottom(last) <= h - action.paddingTop - action.paddingBottom)
    }

    @Test
    @org.robolectric.annotation.Config(qualifiers = "w320dp-h640dp")
    fun longInlineLabelFitsAtLargeAccessibilityTextScale() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val resources = service.resources
        val original = android.content.res.Configuration(resources.configuration)
        val metrics = android.util.DisplayMetrics().apply { setTo(resources.displayMetrics) }
        try {
            val enlarged = android.content.res.Configuration(original).apply { fontScale = 2f }
            metrics.scaledDensity = metrics.density * 2f
            resources.updateConfiguration(enlarged, metrics)
            val layout = service.onCreateInputView() as LinearLayout
            val candidates = (layout.getChildAt(0) as FrameLayout).getChildAt(1) as LinearLayout
            val action = candidates.getChildAt(2) as TextView
            val controller = ViipioInputMethodService::class.java.getDeclaredField("suggestions")
                .apply { isAccessible = true }.get(service) as EstonianSuggestionController
            EstonianSuggestionController::class.java.getDeclaredField("words")
                .apply { isAccessible = true }.set(controller, listOf("one", "two", "three"))
            val info = EditorInfo().apply { packageName = "org.example.synthetic"; inputType = InputType.TYPE_CLASS_TEXT }
            val connection = EditText(service).onCreateInputConnection(info)
            val word = "W".repeat(40)
            ViipioInputMethodService::class.java.getDeclaredField("personalOffer")
                .apply { isAccessible = true }.set(service, ViipioInlinePersonalOffer(word,
                    KeyboardLanguage.ENGLISH, info, connection, 40, completed = false))
            ViipioInputMethodService::class.java.getDeclaredMethod("renderSuggestions")
                .apply { isAccessible = true }.invoke(service)
            assertTrue("the fixture must actually enlarge the chip font",
                action.textSize > 16.5f * resources.displayMetrics.density)
            val w = action.layoutParams.width
            val h = action.layoutParams.height
            action.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            action.layout(0, 0, w, h)
            val lines = action.layout ?: error("text layout missing")
            val last = lines.lineCount - 1
            assertTrue(action.text.toString().contains(word))
            assertTrue(action.text.toString().endsWith("EN"))
            assertEquals(action.text.length, lines.getLineEnd(last))
            assertEquals(0, lines.getEllipsisCount(last))
            assertTrue("large text must fit the existing toolbar height",
                lines.getLineBottom(last) <= h - action.paddingTop - action.paddingBottom)
        } finally {
            resources.updateConfiguration(original, metrics)
        }
    }

    @Test fun inlinePersonalWordOfferFitsExistingToolbarAndStartsHidden() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val bar = layout.getChildAt(0) as FrameLayout
        val offer = (0 until bar.childCount).map { bar.getChildAt(it) }
            .firstOrNull { it.tag == "viipio-personal-word-offer" }
        assertNotNull("a dedicated inline add row must exist", offer)
        assertEquals(View.GONE, offer!!.visibility)
        assertTrue("inline action and dismissal need reachable touch targets",
            (offer as LinearLayout).childCount >= 2)
        assertEquals("the offer must replace the toolbar, not grow the IME",
            (48 * service.resources.displayMetrics.density).toInt(), layout.getChildAt(0).layoutParams.height)
    }

    @Test fun candidateNavigationHasAccessibleTouchAreaWithoutGrowingIme() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val bar = layout.getChildAt(0) as FrameLayout
        val keys = layout.getChildAt(2) as KeyboardSurfaceView
        val candidates = bar.getChildAt(1) as LinearLayout
        val arrow = candidates.getChildAt(0)
        val dp = service.resources.displayMetrics.density
        assertTrue("candidate row should be at least 48dp high", bar.layoutParams.height / dp >= 48f)
        assertTrue("toolbar arrow needs a 48dp target", arrow.layoutParams.width / dp >= 48f)
        assertTrue("toolbar arrow needs a 48dp target", arrow.layoutParams.height / dp >= 48f)
        assertEquals("larger touch area must not hide the editor", 334f,
            (bar.layoutParams.height + keys.layoutParams.height) / dp, 0.1f)
    }

    @Test fun inputViewKeepsToolbarAndCandidatesInOneFixedHeightSlot() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        assertEquals(3, layout.childCount)
        val bar = layout.getChildAt(0) as FrameLayout
        assertEquals(View.VISIBLE, bar.visibility)
        assertTrue(bar.layoutParams.height > 0)
        val toolbar = bar.getChildAt(0) as LinearLayout
        val candidates = bar.getChildAt(1) as LinearLayout
        assertEquals(View.VISIBLE, toolbar.visibility)
        assertEquals(View.GONE, candidates.visibility)
        assertTrue((layout.getChildAt(2).layoutParams.height / service.resources.displayMetrics.density) >= 280f)
        assertEquals(3, candidates.childCount)
        assertEquals("viipio-inline-add-action", candidates.getChildAt(2).tag)
        val track = (candidates.getChildAt(1) as HorizontalScrollView).getChildAt(0) as LinearLayout
        assertEquals(5, track.childCount)
        assertTrue((0..2).all { track.getChildAt(it * 2) is CandidateLabelView })
        assertTrue(toolbar.childCount >= 3)
        assertTrue(layout.getChildAt(1) is EmojiPanelView)
        assertEquals(View.GONE, layout.getChildAt(1).visibility)
        assertEquals((100 * service.resources.displayMetrics.density).toInt(), layout.getChildAt(1).layoutParams.height)
        assertTrue(layout.getChildAt(2) is KeyboardSurfaceView)
    }

    @Test fun recreatingInputViewResetsToolbarCompactState() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        fun toolbar() = ((service.onCreateInputView() as LinearLayout).getChildAt(0) as FrameLayout)
            .getChildAt(0) as LinearLayout
        val first = toolbar()
        first.getChildAt(0).performClick()
        assertEquals(View.INVISIBLE, first.getChildAt(1).visibility)
        val second = toolbar()
        assertEquals(View.VISIBLE, second.getChildAt(1).visibility)
        second.getChildAt(0).performClick()
        assertEquals(View.INVISIBLE, second.getChildAt(1).visibility)
    }

    @Test fun toolbarLanguageSwitchPreservesKeyboardHeightAndChangesDecoderLayout() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val bar = layout.getChildAt(0) as FrameLayout
        val toolbar = bar.getChildAt(0) as LinearLayout
        val keySurface = layout.getChildAt(2) as KeyboardSurfaceView
        val initialBarHeight = bar.layoutParams.height
        val initialKeysHeight = keySurface.layoutParams.height
        assertEquals(KeyboardLanguage.ESTONIAN, keySurface.language)
        val languageButton = (0 until toolbar.childCount).map { toolbar.getChildAt(it) }
            .filterIsInstance<TextView>().first { it.contentDescription == "Switch to English" }
        languageButton.performClick()
        assertEquals(KeyboardLanguage.ENGLISH, keySurface.language)
        assertEquals(initialBarHeight, bar.layoutParams.height)
        assertEquals(initialKeysHeight, keySurface.layoutParams.height)
        assertEquals("Switch to Estonian", languageButton.contentDescription)
    }

    @Test fun ownCompositionSelectionKeepsOfferButExternalSelectionDoesNot() {
        assertTrue(isOwnSuggestionSelection("tere", "tere", 4, 4, 0, 4, null))
        assertTrue(isOwnSuggestionSelection("tere", "tere", 4, 4, -1, -1, 4 to 4))
        assertFalse(isOwnSuggestionSelection("tere", "ter", 3, 3, 0, 4, 4 to 4))
        assertFalse(isOwnSuggestionSelection("tere", "tere", 2, 4, 0, 4, 4 to 4))
        assertFalse(isOwnSuggestionSelection("tere", null, 4, 4, 0, 4, 4 to 4))
        assertFalse(isOwnSuggestionSelection("tere", "tere", 4, 4, -1, -1, null))
    }

    @Test fun emojiSearchIsAnIndependentOfflinePanelNotAnInputMethodTextField() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        assertTrue(source.contains("OfflineEmojiSearch.fromAssets("))
        assertTrue(source.contains("EmojiPanelView(this)"))
        assertTrue(source.contains("panel.appendQuery("))
        assertFalse(source.contains("EmojiGridView"))
    }

    @Test fun emojiBrowserDeleteIsBoundToTheCapturedEditorAndPane() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        val callback = source.substringAfter("panel.onEditorDelete = {", "")
            .substringBefore("panel.onEmojiPicked = { emoji ->")
        assertTrue("Emoji pane must offer an editor-delete callback", callback.isNotBlank())
        assertTrue(callback.contains("panel.visibility == View.VISIBLE"))
        assertTrue(callback.contains("editor === field"))
        assertTrue(callback.contains("currentInputConnection === connection"))
        assertTrue(callback.contains("deleteBackward(connection)"))
        assertTrue(source.contains("panel.onEditorDelete = null"))
    }

    @Test fun inputViewAndCaretChangesInvalidatePendingDecode() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        assertTrue(source.contains("override fun onFinishInputView("))
        assertTrue(source.contains("override fun onUpdateSelection("))
        assertTrue(source.contains("surface?.onGesture = null"))
    }

    @Test fun recreatedInputViewDropsProvisionalCorrectionBeforeAnyNewKey() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        val startup = source.substringAfter("override fun onCreateInputView(): View {")
            .substringBefore("clearSuggestions()")
        assertTrue("a recreated input view must not inherit the prior editor's pending choice",
            startup.contains("pendingCorrection = null"))
    }

    @Test fun fieldExitNeverLearnsAProvisionalAlternate() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        val finish = source.substringAfter("private fun finishTelemetryField() {")
            .substringBefore("/** Hide the normal word bar")
        assertFalse("switching to another field must not be a confirmed correction",
            finish.contains("finalizePendingCorrection()"))
        for (method in listOf("onStartInput(attribute:", "onFinishInput()", "onFinishInputView(finishingInput:")) {
            val body = source.substringAfter("override fun $method").substringBefore("override fun ")
            assertTrue("$method must forget a provisional correction", body.contains("pendingCorrection = null"))
        }
    }

    @Test fun glideDeleteOwnedUndoRestoresCaretEvidenceAfterClearingCandidates() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        val branch = source.substringAfter("is SurfaceGesture.DeleteWord -> {")
            .substringBefore("is SurfaceGesture.RepeatBackspace -> {")
        assertTrue("DeleteWord must restore verified caret AFTER clearSuggestions() resets it",
            Regex("clearSuggestions\\(\\).*ownSelectionAfterWrite\\(tracked\\.connection\\)", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(branch))
    }

    @Test fun ctcChecksAllEligibilityGatesBeforeDispatch() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        val gate = source.substringAfter("val eligible =")
        assertTrue(gate.contains("engine.hasLexiconSource(language)"))
        assertTrue(gate.contains("engine.isModelPermanentlyUnavailable(language)"))
        assertTrue(gate.contains("engine.supportsLayout(source, params"))
    }

    @Test fun committedSwipeWordSurvivesFollowingSpaceInControlledEditor() {
        val field = EditText(RuntimeEnvironment.getApplication())
        val connection = field.onCreateInputConnection(EditorInfo())
        val sink = OwnedWordSink(connection) { true }
        val session = SwipeSuggestionSession()
        val owner = session.startField(sink, isPassword = false)
        val offer = session.accept(session.beginSwipe(owner)!!,
            PredictionResult(listOf("tere", "tore"), listOf(2, 1)))!!
        assertEquals("tere", field.text.toString())
        assertTrue(session.select(owner, offer, 1))
        assertEquals("tore", field.text.toString())
        sink.forget()
        assertTrue(connection.commitText(" ", 1))
        assertEquals("tore ", field.text.toString())
        val serviceSource = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        assertFalse(serviceSource.contains("currentInputConnection?.finishComposingText()"))
    }

    @Test fun failedCandidateCommitKeepsOriginalWordAndNoStraySeparator() {
        val field = EditText(RuntimeEnvironment.getApplication())
        val base = field.onCreateInputConnection(EditorInfo())
        val connection = object : InputConnectionWrapper(base, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
                if (text?.toString()?.trim() == "tore" || text?.toString()?.trim() == "uus") false
                else super.commitText(text, newCursorPosition)
        }
        val sink = OwnedWordSink(connection) { true }
        val session = SwipeSuggestionSession()
        val owner = session.startField(sink, isPassword = false)
        val first = session.accept(session.beginSwipe(owner)!!,
            PredictionResult(listOf("tere", "tore"), listOf(2, 1)))!!
        assertFalse(session.select(owner, first, 1))
        assertEquals("tere", field.text.toString())
        assertNull(session.accept(session.beginSwipe(owner)!!,
            PredictionResult(listOf("uus"), listOf(1))))
        assertEquals("tere", field.text.toString())
    }

    @Test fun candidateCorrectionWorksWithoutCodePointDeletionSupport() {
        val field = EditText(RuntimeEnvironment.getApplication())
        val base = field.onCreateInputConnection(EditorInfo())
        val connection = object : InputConnectionWrapper(base, false) {
            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int) = false
        }
        val sink = OwnedWordSink(connection) { true }
        val session = SwipeSuggestionSession()
        val owner = session.startField(sink, false)
        val offer = session.accept(session.beginSwipe(owner)!!,
            PredictionResult(listOf("tere", "tore"), listOf(2, 1)))!!
        assertTrue(session.select(owner, offer, 1))
        assertEquals("tore", field.text.toString())
    }

    @Test fun estonianHeldLettersCommitRareDiacriticsWithoutChangingEnglish() {
        val committed = mutableListOf<String>()
        val taps = EstonianTapInput(committed::add, {}, {})
        val geometry = KeyboardGeometry(330, 250, KeyboardLanguage.ESTONIAN)
        taps.acceptHold(SurfaceGesture.Hold(geometry.rows[2][1], KeyboardLanguage.ESTONIAN))
        taps.acceptHold(SurfaceGesture.Hold(geometry.rows[3][1], KeyboardLanguage.ESTONIAN))
        assertEquals(listOf("š", "ž"), committed)
        taps.switchLanguage()
        taps.acceptHold(SurfaceGesture.Hold(geometry.rows[2][1], KeyboardLanguage.ESTONIAN))
        assertEquals(listOf("š", "ž"), committed)
    }

    @Test fun declinedEditorActionFallsBackToEnterKeyEvents() {
        val base = EditText(RuntimeEnvironment.getApplication()).onCreateInputConnection(EditorInfo())
        val events = mutableListOf<Pair<Int, Int>>()
        val connection = object : InputConnectionWrapper(base, false) {
            override fun performEditorAction(editorAction: Int): Boolean = false
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                events += event.action to event.keyCode
                return true
            }
        }
        assertTrue(ViipioInputMethodService.submitEnter(connection, EditorInfo.IME_ACTION_SEND))
        assertEquals(listOf(KeyEvent.ACTION_DOWN to KeyEvent.KEYCODE_ENTER,
            KeyEvent.ACTION_UP to KeyEvent.KEYCODE_ENTER), events)
    }

    @Test fun measuredSurfaceWarmsEstonianDecoderBeforeFirstSwipe() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        assertTrue(source.contains("engine.warmUpAsync(source, params"))
    }

    @Test fun serviceInitializesDecoderConfigOnColdStart() {
        val source = java.io.File("src/main/kotlin/tribixbite/cleverkeys/ViipioInputMethodService.kt").readText()
        assertTrue(source.contains("Config.initGlobalConfig("))
        assertTrue(source.contains("CtcInstalledPacks.bind(this)"))
    }

    @Test fun backspaceImmediatelyAfterSwipeDeletesOnlyOwnedWord() {
        val field = EditText(RuntimeEnvironment.getApplication()).apply { setText("algus "); setSelection(text.length) }
        val connection = field.onCreateInputConnection(EditorInfo())
        val sink = OwnedWordSink(connection) { true }
        val session = SwipeSuggestionSession()
        val owner = session.startField(sink, false)
        session.accept(session.beginSwipe(owner)!!, PredictionResult(listOf("tere"), listOf(1)))!!
        assertEquals("algus tere", field.text.toString())
        assertEquals(true, sink.deleteOwnedWord())
        assertEquals("algus ", field.text.toString())
        assertNull(sink.deleteOwnedWord())
    }

    @Test fun failedOwnedWordDeleteNeverFallsBackToCharacterDeletion() {
        val field = EditText(RuntimeEnvironment.getApplication())
        val base = field.onCreateInputConnection(EditorInfo())
        val connection = object : InputConnectionWrapper(base, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
                if (text?.isEmpty() == true) false else super.commitText(text, newCursorPosition)
        }
        val sink = OwnedWordSink(connection) { true }
        val session = SwipeSuggestionSession()
        val owner = session.startField(sink, false)
        session.accept(session.beginSwipe(owner)!!, PredictionResult(listOf("tere"), listOf(1)))!!
        assertEquals(false, sink.deleteOwnedWord())
        assertEquals("tere", field.text.toString())
    }

    @Test fun wordDeleteSelectsPreviousWordAndSeparatingWhitespace() {
        val field = EditText(RuntimeEnvironment.getApplication()).apply {
            setText("üks kaks "); setSelection(text.length)
        }
        val connection = field.onCreateInputConnection(EditorInfo())
        assertTrue(ViipioInputMethodService.deletePreviousWord(connection))
        assertEquals("üks ", field.text.toString())
        assertTrue(ViipioInputMethodService.deletePreviousWord(connection))
        assertEquals("", field.text.toString())
    }

    @Test fun wordDeleteDeclinesUninspectableEditorWithoutDestroyingText() {
        val field = EditText(RuntimeEnvironment.getApplication()).apply { setText("tere"); setSelection(text.length) }
        val base = field.onCreateInputConnection(EditorInfo())
        val blind = object : InputConnectionWrapper(base, false) {
            override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int) = null
        }
        assertFalse(ViipioInputMethodService.deletePreviousWord(blind))
        assertEquals("tere", field.text.toString())
    }

    @Test fun offsetExtractedTextDeleteWordLeavesIdenticalEarlierWordUntouched() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("foo foo")
        field.setSelection(field.length())
        val connection = offsetConnection(field)
        assertTrue(ViipioInputMethodService.deletePreviousWord(connection))
        assertEquals("foo ", field.text.toString())
    }

    @Test fun offsetExtractedTextOwnedDeleteLeavesIdenticalEarlierWordUntouched() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("foo ")
        field.setSelection(field.length())
        val connection = offsetConnection(field)
        val sink = OwnedWordSink(connection) { true }
        assertTrue(sink.setComposingText("foo"))
        assertEquals("foo foo", field.text.toString())
        assertEquals(true, sink.deleteOwnedWord())
        assertEquals("foo ", field.text.toString())
    }

    @Test fun offsetExtractedTextCorrectionReplacesOnlyOwnedWord() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("foo ")
        field.setSelection(field.length())
        val connection = offsetConnection(field)
        val sink = OwnedWordSink(connection) { true }
        assertTrue(sink.setComposingText("foo"))
        assertTrue(sink.setComposingText("bar"))
        assertEquals("foo bar", field.text.toString())
    }

    private fun offsetConnection(field: EditText): android.view.inputmethod.InputConnection {
        val base = field.onCreateInputConnection(EditorInfo())
        return object : InputConnectionWrapper(base, false) {
            override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int): android.view.inputmethod.ExtractedText? {
                val extracted = super.getExtractedText(request, flags) ?: return null
                extracted.startOffset = 4
                extracted.text = field.text.substring(4)
                extracted.selectionStart -= 4
                extracted.selectionEnd -= 4
                return extracted
            }
        }
    }

    @Test fun blindEditorBackspaceUsesKeyEventsInsteadOfUnsafeCodePointDeletion() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("ab")
        field.setSelection(2)
        val base = field.onCreateInputConnection(EditorInfo())
        var codePointDeletes = 0
        val connection = object : InputConnectionWrapper(base, false) {
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? = null
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL &&
                    field.length() > 0) field.text.delete(field.length() - 1, field.length())
                return true
            }
            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
                codePointDeletes++
                return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
            }
        }
        assertTrue(ViipioInputMethodService.deleteBackward(connection))
        assertEquals("a", field.text.toString())
        assertEquals(0, codePointDeletes)
    }

    @Test fun repeatBackspaceAtEmptyEditorDoesNotSendDeleteCommand() {
        val field = EditText(RuntimeEnvironment.getApplication())
        val base = field.onCreateInputConnection(EditorInfo())
        var deletes = 0
        val connection = object : InputConnectionWrapper(base, false) {
            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
                deletes++
                return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
            }
        }
        assertFalse(ViipioInputMethodService.deleteBackward(connection))
        assertEquals(0, deletes)
    }

    @Test fun backspaceDeletesWholeVariationSelectorEmojiInsteadOfOrphaningSelector() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("a♥️")
        field.setSelection(field.length())
        val connection = field.onCreateInputConnection(EditorInfo())
        assertTrue(ViipioInputMethodService.deleteBackward(connection))
        assertEquals("a", field.text.toString())
    }

    @Test fun backspaceDeletesJoinedModifierAndFlagEmojiWithoutTouchingEarlierText() {
        for (emoji in listOf("👍🏽", "👩‍💻", "🇪🇪")) {
            val field = EditText(RuntimeEnvironment.getApplication())
            field.setText("a$emoji")
            field.setSelection(field.length())
            val connection = field.onCreateInputConnection(EditorInfo())
            assertTrue("Deletion failed for $emoji", ViipioInputMethodService.deleteBackward(connection))
            assertEquals("Earlier text must survive deletion of $emoji", "a", field.text.toString())
        }
    }

    @Test fun truncatedGraphemeWindowDeclinesBlindPartialDeletion() {
        val original = "a" + "b" + "\u0301".repeat(127)
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText(original)
        field.setSelection(field.length())
        val connection = field.onCreateInputConnection(EditorInfo())
        assertFalse(ViipioInputMethodService.deleteBackward(connection))
        assertEquals(original, field.text.toString())
    }

    @Test fun truncatedRegionalIndicatorRunNeverDeletesTwoWhenOnlyOneIsLast() {
        val indicator = "🇦" // one regional-indicator code point, paired by grapheme rules
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("a" + indicator.repeat(65))
        field.setSelection(field.length())
        val connection = field.onCreateInputConnection(EditorInfo())
        val before = field.text.toString()
        val deleted = ViipioInputMethodService.deleteBackward(connection)
        assertTrue("A Backspace may delete one final indicator or safely decline", !deleted ||
            field.text.toString() == "a" + indicator.repeat(64))
        assertTrue("Never remove two regional indicators from a truncated run",
            field.text.toString() == before || field.text.toString() == "a" + indicator.repeat(64))
    }

    @Test fun shortEditorReadbackDoesNotDeleteTwoFromAnOddRegionalIndicatorRun() {
        val indicator = "🇦"
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("a" + indicator.repeat(65))
        field.setSelection(field.length())
        val actual = field.onCreateInputConnection(EditorInfo())
        val capped = object : InputConnectionWrapper(actual, false) {
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? =
                super.getTextBeforeCursor(minOf(n, 64), flags)
        }
        val before = field.text.toString()
        val deleted = ViipioInputMethodService.deleteBackward(capped)
        assertTrue("A short readback may delete only one final indicator or decline",
            !deleted || field.text.toString() == "a" + indicator.repeat(64))
        assertTrue("Never remove two indicators when the editor caps readback",
            field.text.toString() == before || field.text.toString() == "a" + indicator.repeat(64))
    }

    @Test fun backspaceRemovesSelectedTextOrWholeUnicodeCodePoint() {
        val field = EditText(RuntimeEnvironment.getApplication())
        field.setText("a😀b")
        val connection = field.onCreateInputConnection(EditorInfo())
        field.setSelection(1, 3) // the surrogate pair is selected
        assertTrue(ViipioInputMethodService.deleteBackward(connection))
        assertEquals("ab", field.text.toString())
        field.setSelection(1)
        assertTrue(ViipioInputMethodService.deleteBackward(connection))
        assertEquals("b", field.text.toString())
    }

    @Test fun pinEditorUsesDigitOnlyKeypadInsteadOfAlphabetAndHidesToolbar() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        service.onStartInput(info, false)
        service.onStartInputView(info, false)
        val surface = layout.getChildAt(2) as KeyboardSurfaceView
        surface.layout(0, 0, 330, 286)
        val keys = surface.geometry.keys
        assertEquals(('0'..'9').map(Char::toString).toSet(),
            keys.filter { it.kind == KeyKind.NUMBER }.map { it.label }.toSet())
        assertEquals("123", surface.geometry.rows[0].joinToString("") { it.label })
        assertEquals(listOf("123", "456", "789", "⌫0↵"),
            surface.geometry.rows.map { row -> row.joinToString("") { it.label } })
        assertTrue(keys.none { it.kind == KeyKind.LETTER || it.kind == KeyKind.EMOJI ||
            it.kind == KeyKind.SPACE || it.kind == KeyKind.SYMBOLS ||
            it.kind == KeyKind.SHIFT || it.kind == KeyKind.SYMBOL })
        assertEquals(1, keys.count { it.kind == KeyKind.BACKSPACE })
        assertEquals(1, keys.count { it.kind == KeyKind.ENTER })
        assertEquals(View.GONE, layout.getChildAt(0).visibility)
    }

    @Test fun integerNumberFieldUsesKeypadThenRestoresLettersForText() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val numeric = EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER }
        service.onStartInput(numeric, false)
        service.onStartInputView(numeric, false)
        val surface = layout.getChildAt(2) as KeyboardSurfaceView
        surface.layout(0, 0, 330, 286)
        assertEquals(('0'..'9').map(Char::toString).toSet(),
            surface.geometry.keys.filter { it.kind == KeyKind.NUMBER }.map { it.label }.toSet())
        assertFalse(surface.geometry.keys.any { it.kind == KeyKind.LETTER })
        assertEquals(View.GONE, layout.getChildAt(0).visibility)
        val text = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        service.onStartInput(text, false)
        service.onStartInputView(text, false)
        assertTrue(surface.geometry.keys.any { it.kind == KeyKind.LETTER && it.label == "q" })
        assertEquals(View.VISIBLE, layout.getChildAt(0).visibility)
    }

    @Test fun decimalAndSignedNumberFieldsExposeOnlyRequestedSeparators() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val surface = layout.getChildAt(2) as KeyboardSurfaceView
        surface.layout(0, 0, 330, 286)
        val cases = listOf(
            0 to emptySet<String>(),
            InputType.TYPE_NUMBER_FLAG_DECIMAL to setOf("."),
            InputType.TYPE_NUMBER_FLAG_SIGNED to setOf("-"),
            (InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED) to setOf("-", "."),
            InputType.TYPE_NUMBER_VARIATION_PASSWORD to emptySet(),
        )
        for ((flags, expectedSymbols) in cases) {
            val info = EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER or flags }
            service.onStartInput(info, false)
            service.onStartInputView(info, false)
            assertEquals("flags $flags", expectedSymbols,
                surface.geometry.keys.filter { it.kind == KeyKind.SYMBOL }.map { it.label }.toSet())
            assertEquals("flags $flags", ('0'..'9').map(Char::toString).toSet(),
                surface.geometry.keys.filter { it.kind == KeyKind.NUMBER }.map { it.label }.toSet())
            assertFalse(surface.geometry.keys.any { it.kind == KeyKind.LETTER })
        }
    }

    @Test fun phoneAndDateTimeEditorsUseSpecializedNumericKeys() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val surface = layout.getChildAt(2) as KeyboardSurfaceView
        surface.layout(0, 0, 337, 286)
        val cases = listOf(
            InputType.TYPE_CLASS_PHONE to setOf("*", "#", "+", "-"),
            (InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE) to setOf("/", "-"),
            (InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME) to setOf(":"),
            InputType.TYPE_CLASS_DATETIME to setOf("/", ":", "-"),
        )
        for ((type, expectedSymbols) in cases) {
            val info = EditorInfo().apply { inputType = type }
            service.onStartInput(info, false)
            service.onStartInputView(info, false)
            val keys = surface.geometry.keys
            assertEquals("type $type", ('0'..'9').map(Char::toString).toSet(),
                keys.filter { it.kind == KeyKind.NUMBER }.map { it.label }.toSet())
            assertEquals("type $type", expectedSymbols,
                keys.filter { it.kind == KeyKind.SYMBOL }.map { it.label }.toSet())
            assertFalse(keys.any { it.kind == KeyKind.LETTER || it.kind == KeyKind.EMOJI ||
                it.kind == KeyKind.SPACE || it.kind == KeyKind.SYMBOLS })
            assertEquals(1, keys.count { it.kind == KeyKind.BACKSPACE })
            assertEquals(1, keys.count { it.kind == KeyKind.ENTER })
            assertEquals(View.GONE, layout.getChildAt(0).visibility)
        }
    }

    @Test fun heldBackspaceStopsBeforeSwitchingBetweenPinEditors() {
        val service = Robolectric.buildService(ViipioInputMethodService::class.java).get()
        val layout = service.onCreateInputView() as LinearLayout
        val first = EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        service.onStartInput(first, false)
        service.onStartInputView(first, false)
        val surface = layout.getChildAt(2) as KeyboardSurfaceView
        surface.layout(0, 0, 330, 286)
        val emitted = mutableListOf<SurfaceGesture>()
        surface.onGesture = emitted::add
        val backspace = surface.geometry.keys.single { it.kind == KeyKind.BACKSPACE }.bounds
        surface.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN,
            backspace.centerX, backspace.centerY, 0))
        val next = EditorInfo().apply { inputType = first.inputType }
        service.onStartInput(next, false)
        shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS)
        assertTrue("old PIN's repeat must not reach the next PIN editor", emitted.isEmpty())
    }

    @Test fun passwordEditorsNeverEnterSwipePrediction() {
        val variations = listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        for (variation in variations) {
            val editor = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or variation }
            assertFalse("password variation $variation", ViipioInputMethodService.isSwipeAllowed(editor))
        }
        val numeric = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertFalse(ViipioInputMethodService.isSwipeAllowed(numeric))
        val ordinary = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        assertTrue(ViipioInputMethodService.isSwipeAllowed(ordinary))
    }

    @Test fun adapterCellsMatchSurfaceCellsIncludingOddPixelWidths() {
        val board = KeyboardGeometry(301, 233, KeyboardLanguage.ESTONIAN)
        val keyboard = ViipioInputMethodService.adapterKeyboard(board)
        val rects = AdapterGeometry.computeKeyRects(keyboard, AdapterGeometry.Params(1f, 1f, 0f, 0f))
        assertEquals(board.keys.size, rects.size)
        board.keys.zip(rects).forEach { (key, rect) ->
            assertEquals(key.bounds.left.toFloat(), rect.bounds.left, 0.001f)
            assertEquals(key.bounds.right.toFloat(), rect.bounds.right, 0.001f)
            assertEquals(key.bounds.top.toFloat(), rect.bounds.top, 0.001f)
            assertEquals(key.bounds.bottom.toFloat(), rect.bounds.bottom, 0.001f)
        }
    }
}
