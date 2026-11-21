package tribixbite.cleverkeys.estonianemoji

import android.widget.ListView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EmojiPanelViewTest {
    private fun panel() = EmojiPanelView(RuntimeEnvironment.getApplication())
    private fun list(panel: EmojiPanelView): ListView = when (val content = panel.getChildAt(1)) {
        is ListView -> content
        is android.view.ViewGroup -> content.getChildAt(0) as ListView
        else -> error("Emoji catalog ListView is missing")
    }

    @Before fun resetRecents() {
        RuntimeEnvironment.getApplication().getSharedPreferences("emoji_browser_recents", 0)
            .edit().clear().commit()
        RuntimeEnvironment.getApplication().getSharedPreferences("emoji_browser_appearance", 0)
            .edit().clear().commit()
    }

    @Test fun browseStartsWithSearchCategoriesAndScrollableLabeledEmojiRows() {
        val panel = panel()
        assertFalse(panel.isSearchMode)
        assertTrue(panel.getChildAt(0) is LinearLayout)
        val list = list(panel)
        assertTrue(list.adapter.getView(0, null, list) is TextView) // section header
        assertTrue(list.adapter.getView(1, null, list) is LinearLayout) // eight emoji cells
        assertEquals("Smileys and emotions", panel.browseSectionTitles().first())
        assertTrue(panel.browseSectionTitles().contains("People"))
        assertTrue(panel.visibleEmoji.size > 40)
        assertFalse(panel.containsEditableTextField())
    }

    @Test fun browseIsOneContinuousCatalogInsteadOfReplacingTheListPerTab() {
        val panel = panel()
        val expected = listOf("Smileys and emotions", "People", "Animals and nature", "Food and drink",
            "Activities", "Travel and places", "Objects", "Symbols", "Flags", "New emoji",
            "Emoticons", "Kaomoji")
        assertEquals(expected, panel.browseSectionTitles())
        val list = list(panel)
        val initialCount = list.adapter.count
        panel.selectCategory(2)
        assertEquals(expected, panel.browseSectionTitles())
        assertEquals(initialCount, list.adapter.count)
    }

    @Test fun scrollingToAnotherSectionUpdatesTheSelectedCategoryTab() {
        val panel = panel()
        val exact = android.view.View.MeasureSpec.EXACTLY
        panel.measure(android.view.View.MeasureSpec.makeMeasureSpec(720, exact),
            android.view.View.MeasureSpec.makeMeasureSpec(668, exact))
        panel.layout(0, 0, 720, 668)
        val list = list(panel)
        val peopleHeader = (0 until list.adapter.count).first { index ->
            (list.adapter.getView(index, null, list) as? TextView)?.text == "PEOPLE"
        }
        // Robolectric does not always materialize distant ListView rows from setSelection.
        // The production OnScrollListener uses this same first-visible-row mapping.
        panel.syncCategoryFromScroll(peopleHeader)
        val bar = (panel.getChildAt(0) as LinearLayout).getChildAt(1) as android.widget.HorizontalScrollView
        val tabs = bar.getChildAt(0) as LinearLayout
        assertTrue("The active tab must follow the first visible section", tabs.getChildAt(2).isSelected)
        assertFalse(tabs.getChildAt(1).isSelected)
        panel.syncCategoryFromScroll(0)
        assertTrue("Scrolling back selects the earlier section", tabs.getChildAt(1).isSelected)
    }

    @Test fun browseShowsOneWavingHandInsteadOfSixAdjacentSkinTones() {
        val panel = panel()
        assertTrue(panel.visibleEmoji.contains("👋"))
        assertFalse("Tone choices should open on demand, not fill the catalog",
            panel.visibleEmoji.contains("👋🏻"))
        assertEquals(1, panel.visibleEmoji.count { it.startsWith("👋") })
    }

    private fun openVariantPopup(panel: EmojiPanelView, emoji: String): LinearLayout {
        val exact = android.view.View.MeasureSpec.EXACTLY
        panel.measure(android.view.View.MeasureSpec.makeMeasureSpec(720, exact),
            android.view.View.MeasureSpec.makeMeasureSpec(668, exact))
        panel.layout(0, 0, 720, 668)
        val list = list(panel)
        val rowIndex = (0 until list.adapter.count).first { index ->
            val row = list.adapter.getView(index, null, list) as? LinearLayout
            row != null && (0 until row.childCount).any { (row.getChildAt(it) as TextView).text == emoji }
        }
        // Robolectric does not materialize far-away ListView children from setSelection.
        val row = list.adapter.getView(rowIndex, null, list) as LinearLayout
        val container = panel.getChildAt(1) as android.widget.FrameLayout
        container.addView(row, android.widget.FrameLayout.LayoutParams(720, 90))
        row.measure(android.view.View.MeasureSpec.makeMeasureSpec(720, exact),
            android.view.View.MeasureSpec.makeMeasureSpec(90, exact))
        row.layout(0, 0, 720, 90)
        val cell = (0 until row.childCount).map { row.getChildAt(it) as TextView }
            .single { it.text == emoji }
        assertTrue("The cell must offer long-press variants", cell.performLongClick())
        return panel.findViewWithTag<LinearLayout>("emoji_tone_popup")!!
    }
    private fun openHandPopup(panel: EmojiPanelView) = openVariantPopup(panel, "👋")

    @Test fun variationSelectorBaseAndSkinTonesShareOneBrowsableFamily() {
        val panel = panel()
        assertEquals("The ✌️ base and its five tones occupy one catalog cell",
            1, panel.visibleEmoji.count { it.startsWith("✌") })
        panel.onEmojiPicked = { true }
        val popup = openVariantPopup(panel, "✌️")
        assertEquals(6, popup.childCount)
        assertTrue(popup.getChildAt(4).performClick())
        assertEquals("✌🏾", panel.recentEmoji.first())
        assertTrue(panel.visibleEmoji.contains("👋🏾"))
    }

    @Test fun holdingWavingHandOpensSixChoicesAndOnlyChosenToneIsCommitted() {
        val panel = panel()
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = { picked.add(it); true }
        val popup = openHandPopup(panel)
        assertTrue("Opening variants must not commit an emoji", picked.isEmpty())
        val choices = (0 until popup.childCount).map { (popup.getChildAt(it) as TextView).text.toString() }
        assertEquals(listOf("👋", "👋🏻", "👋🏼", "👋🏽", "👋🏾", "👋🏿"), choices)
        assertTrue(popup.getChildAt(4).performClick())
        assertEquals(listOf("👋🏾"), picked)
        assertEquals("👋🏾", panel.recentEmoji.first())
        assertNull(panel.findViewWithTag<LinearLayout>("emoji_tone_popup"))
    }

    @Test fun chosenSkinToneUpdatesOtherHandsAndSurvivesPanelRecreation() {
        val panel = panel()
        panel.onEmojiPicked = { true }
        val popup = openHandPopup(panel)
        assertTrue(popup.getChildAt(4).performClick())
        assertTrue("Other hands inherit the chosen tone", panel.visibleEmoji.contains("👍🏾"))
        assertFalse(panel.visibleEmoji.contains("👍"))
        val recreated = panel()
        assertTrue("Tone choice must survive an IME view recreation", recreated.visibleEmoji.contains("👍🏾"))
        assertEquals("👋🏾", recreated.recentEmoji.first())
    }

    @Test fun choosingToneRebindsCatalogOnceSoViewportDoesNotJumpBackward() {
        val panel = panel()
        panel.onEmojiPicked = { true }
        val popup = openHandPopup(panel)
        var changes = 0
        list(panel).adapter.registerDataSetObserver(object : android.database.DataSetObserver() {
            override fun onChanged() { changes++ }
        })
        assertTrue(popup.getChildAt(4).performClick())
        assertEquals("Recent insertion and global tone must update the list in one pass", 1, changes)
    }

    @Test fun mixedTonePeoplePopupKeepsEveryCatalogVariantReachable() {
        val panel = panel()
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = { picked.add(it); true }
        val popup = openVariantPopup(panel, "🧑‍🤝‍🧑")
        fun choices(view: android.view.View): List<TextView> = when (view) {
            is TextView -> listOf(view)
            is android.view.ViewGroup -> (0 until view.childCount).flatMap { choices(view.getChildAt(it)) }
            else -> emptyList()
        }
        val options = choices(popup)
        assertEquals("Every packaged mixed-tone choice must be reachable", 26, options.size)
        val mixed = options.single { it.text == "🧑🏾‍🤝‍🧑🏿" }
        assertTrue(mixed.performClick())
        assertEquals(listOf("🧑🏾‍🤝‍🧑🏿"), picked)
        assertEquals("🧑🏾‍🤝‍🧑🏿", panel.recentEmoji.first())
    }

    @Test fun choosingAnotherEmojiDismissesAnOpenTonePopup() {
        val panel = panel()
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = { picked.add(it); true }
        openHandPopup(panel)
        panel.pickForTest("😀")
        assertEquals(listOf("😀"), picked)
        assertNull("A stale tone popup must not cover the new catalog state",
            panel.findViewWithTag<LinearLayout>("emoji_tone_popup"))
    }

    @Test fun blankSpaceInsideTonePopupConsumesTouchWithoutSelectingUnderlyingEmoji() {
        val panel = panel()
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = { picked.add(it); true }
        val popup = openHandPopup(panel)
        val exact = android.view.View.MeasureSpec.EXACTLY
        val width = popup.layoutParams.width
        val height = popup.layoutParams.height
        popup.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, exact),
            android.view.View.MeasureSpec.makeMeasureSpec(height, exact))
        popup.layout(0, 0, width, height)
        val down = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN,
            (width - 1).toFloat(), (height - 1).toFloat(), 0)
        val up = android.view.MotionEvent.obtain(0, 10, android.view.MotionEvent.ACTION_UP,
            (width - 1).toFloat(), (height - 1).toFloat(), 0)
        assertTrue("The blank popup margin must not pass a tap to the catalog",
            popup.dispatchTouchEvent(down))
        popup.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
        assertTrue(picked.isEmpty())
        assertNotNull(panel.findViewWithTag<LinearLayout>("emoji_tone_popup"))
    }

    @Test fun tappingOutsideTonePopupDismissesItWithoutCommittingText() {
        val panel = panel()
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = { picked.add(it); true }
        openHandPopup(panel)
        val content = panel.getChildAt(1) as android.widget.FrameLayout
        val down = android.view.MotionEvent.obtain(1, 1, android.view.MotionEvent.ACTION_DOWN,
            (content.width - 4).toFloat(), (content.height - 4).toFloat(), 0)
        content.dispatchTouchEvent(down)
        down.recycle()
        assertNull(panel.findViewWithTag<LinearLayout>("emoji_tone_popup"))
        assertTrue("Dismissing outside must not insert a tone", picked.isEmpty())
    }

    @Test fun tonePreferenceStorageFailureDoesNotCrashAfterConfirmedEmojiCommit() {
        val base = RuntimeEnvironment.getApplication()
        val failingContext = object : android.content.ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                val prefs = super.getSharedPreferences(name, mode)
                return if (name == "emoji_browser_appearance")
                    object : android.content.SharedPreferences by prefs {
                        override fun edit(): android.content.SharedPreferences.Editor =
                            throw IllegalStateException("synthetic tone-store failure")
                    } else prefs
            }
        }
        val panel = EmojiPanelView(failingContext)
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = { picked.add(it); true }
        val popup = openHandPopup(panel)
        val attempt = runCatching { popup.getChildAt(4).performClick() }
        assertTrue("Storage errors must not crash the IME", attempt.isSuccess)
        assertEquals(listOf("👋🏾"), picked)
        assertEquals("👋🏾", panel.recentEmoji.first())
        assertTrue("Failed persistence must not falsely change the global default",
            panel.visibleEmoji.contains("👍"))
    }

    @Test fun recentStorageExceptionsDoNotCrashAfterConfirmedEmojiCommit() {
        for (stage in listOf("edit", "apply")) {
            resetRecents() // Clear the real backing store before each synthetic failure.
            val base = RuntimeEnvironment.getApplication()
            val failingContext = object : android.content.ContextWrapper(base) {
                override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                    val prefs = super.getSharedPreferences(name, mode)
                    return if (name == "emoji_browser_recents")
                        object : android.content.SharedPreferences by prefs {
                            override fun edit(): android.content.SharedPreferences.Editor {
                                if (stage == "edit") throw IllegalStateException("synthetic edit failure")
                                val editor = prefs.edit()
                                return object : android.content.SharedPreferences.Editor by editor {
                                    override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor {
                                        editor.putString(key, value)
                                        return this // Preserve the wrapper across the fluent editor chain.
                                    }
                                    override fun apply() = throw IllegalStateException("synthetic apply failure")
                                }
                            }
                        } else prefs
                }
            }
            val instance = EmojiPanelView(failingContext)
            val picked = mutableListOf<String>()
            instance.onEmojiPicked = { picked.add(it); true }
            val attempt = runCatching { instance.pickForTest("😀") }
            assertTrue("Recents $stage failure must not crash after editor commit", attempt.isSuccess)
            assertEquals(listOf("😀"), picked)
            assertEquals("😀", instance.recentEmoji.first())
            assertTrue("Unwritten recents must not masquerade as durable",
                EmojiPanelView(base).recentEmoji.isEmpty())
        }
    }

    @Test fun kaomojiUseWideCellsInsteadOfClippedEightColumnGlyphs() {
        val panel = panel()
        val list = list(panel)
        val adapter = list.adapter
        val header = (0 until adapter.count).first { adapter.getItem(it).toString() == "Header(title=Kaomoji)" }
        val kaomojiRow = adapter.getView(header + 1, null, list) as LinearLayout
        assertEquals("Long kaomoji must have room for their complete glyphs", 2, kaomojiRow.childCount)
        assertTrue((kaomojiRow.getChildAt(0) as TextView).textSize <=
            18f * panel.resources.displayMetrics.scaledDensity)
    }

    @Test fun browseGridUsesEightUnboxedEmojiCellsLikeObservedReference() {
        val panel = panel()
        val list = list(panel)
        val firstEmojiRow = list.adapter.getView(1, null, list) as LinearLayout
        assertEquals("Gboard's observed browse density uses eight columns", 8, firstEmojiRow.childCount)
        assertNull("Emoji glyphs should not each sit on a separate white tile",
            (firstEmojiRow.getChildAt(0) as TextView).background)
    }

    @Test fun toneCapableEmojiAnnouncesItsLongPressAction() {
        val panel = panel()
        val list = list(panel)
        val hands = (0 until list.adapter.count).asSequence().mapNotNull { index ->
            list.adapter.getView(index, null, list) as? LinearLayout
        }.flatMap { row -> (0 until row.childCount).asSequence().map { row.getChildAt(it) as TextView } }
        val waving = hands.first { it.text == "👋" }
        assertTrue(waving.isLongClickable)
        assertTrue("TalkBack must expose how to open tone choices",
            waving.contentDescription.toString().contains("hold for skin tones"))
        val face = (list.adapter.getView(1, null, list) as LinearLayout).getChildAt(0) as TextView
        assertFalse("Ordinary emoji must not advertise unavailable tone choices", face.isLongClickable)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun toneCapableCellHasCornerCueThatPlainEmojiDoesNot() {
        val panel = panel()
        val list = list(panel)
        val handIndex = (0 until list.adapter.count).first { index ->
            val row = list.adapter.getView(index, null, list) as? LinearLayout
            row != null && (0 until row.childCount).any { (row.getChildAt(it) as TextView).text == "👋" }
        }
        fun cornerPixel(rowIndex: Int, entry: String): Int {
            val row = list.adapter.getView(rowIndex, null, list) as LinearLayout
            val exact = android.view.View.MeasureSpec.EXACTLY
            row.measure(android.view.View.MeasureSpec.makeMeasureSpec(720, exact),
                android.view.View.MeasureSpec.makeMeasureSpec(90, exact))
            row.layout(0, 0, 720, 90)
            val cell = (0 until row.childCount).map { row.getChildAt(it) as TextView }
                .single { it.text == entry }
            val image = android.graphics.Bitmap.createBitmap(720, 90,
                android.graphics.Bitmap.Config.ARGB_8888)
            row.draw(android.graphics.Canvas(image))
            return image.getPixel(cell.right - 7, cell.bottom - 7)
        }
        assertNotEquals("Variant marker should be visible near the hand's corner",
            0, cornerPixel(handIndex, "👋"))
        assertEquals("An ordinary emoji must not show a false variant marker",
            0, cornerPixel(1, "😀"))
    }

    @Test fun browseFooterHasEditorBackspaceAndNoClearOrFakeSend() {
        val panel = panel()
        panel.onEmojiPicked = { true }
        panel.pickForTest("😀")
        val footer = panel.getChildAt(2) as LinearLayout
        val controls = (0 until footer.childCount).mapNotNull { footer.getChildAt(it) as? TextView }
        var editorDeletes = 0
        panel.onEditorDelete = { editorDeletes++ }
        assertTrue(controls.any { it.text.toString() == "ABC" })
        val backspace = controls.singleOrNull { it.contentDescription == "Delete from text field" }
        assertNotNull("Gboard's emoji browse footer deletes from the editor", backspace)
        assertTrue("Backspace needs at least a 48dp touch target",
            backspace!!.layoutParams.height >= (48 * panel.resources.displayMetrics.density).toInt())
        assertTrue(backspace.performClick())
        assertEquals(1, editorDeletes)
        assertFalse("Clear must not occupy the emoji browse footer",
            controls.any { it.text.toString() == "Clear" })
        assertFalse("Gboard's emoji browse footer has no Send action",
            controls.any { it.contentDescription == "Send" })
        assertEquals(listOf("😀"), panel.recentEmoji)
    }

    @Test fun tappingCategoryTabsKeepsOneCatalogAndHighlightsTheTarget() {
        val panel = panel()
        val allEmoji = panel.visibleEmoji.toList()
        val bar = (panel.getChildAt(0) as LinearLayout).getChildAt(1) as android.widget.HorizontalScrollView
        val tabs = bar.getChildAt(0) as LinearLayout
        for (target in listOf(2, 1, 11, 12, 10)) {
            panel.selectCategory(target)
            assertEquals("Switching tabs must not replace the scrollable catalog", allEmoji, panel.visibleEmoji)
            assertTrue("Category $target must be selected", tabs.getChildAt(target).isSelected)
        }
        assertTrue(allEmoji.contains(":)"))
        assertTrue(allEmoji.any { it.contains('ツ') })
        assertTrue(panel.browseSectionTitles().contains("New emoji"))
    }

    @Test fun searchToggleCompactsPanelAndRetainsOfflineLookup() {
        val panel = panel()
        val modes = mutableListOf<Boolean>()
        panel.onSearchModeChanged = modes::add
        panel.setSearchIndex(OfflineEmojiSearch.load(ByteArrayInputStream("süda\t❤️,💙\n".toByteArray())))
        panel.setSearchMode(true)
        panel.appendQuery("sü")
        assertEquals(listOf("❤️", "💙"), panel.visibleEmoji)
        assertEquals("sü", panel.query)
        assertEquals((56 * panel.resources.displayMetrics.density).toInt(), panel.getChildAt(1).layoutParams.height)
        panel.backspaceQuery()
        assertEquals("s", panel.query)
        panel.setSearchMode(false)
        assertEquals(listOf(true, false), modes)
        assertEquals("", panel.query)
        assertFalse(panel.containsEditableTextField())
    }

    @Test fun pickedEmojiIsSavedLocallyAndRecentCanBeCleared() {
        val panel = panel()
        val picked = mutableListOf<String>()
        panel.onEmojiPicked = picked::add
        panel.pickForTest("😀")
        assertEquals(listOf("😀"), picked)
        panel.selectCategory(0)
        assertEquals("😀", panel.recentEmoji.first())
        assertEquals(listOf("Recently used", "Smileys and emotions"), panel.browseSectionTitles().take(2))
        val recreated = panel()
        assertEquals("😀", recreated.recentEmoji.first())
        recreated.clearRecent()
        assertTrue(recreated.recentEmoji.isEmpty())
        assertEquals("Smileys and emotions", recreated.browseSectionTitles().first())
    }

    @Test fun settingsErasureClearsSavedRecentsAndRefreshesAnExistingPanel() {
        val panel = panel()
        panel.onEmojiPicked = { true }
        panel.pickForTest("😀")
        assertEquals(listOf("😀"), panel.recentEmoji)
        assertTrue(EmojiPanelView.clearSavedRecents(RuntimeEnvironment.getApplication()))
        panel.reloadRecents()
        assertTrue(panel.recentEmoji.isEmpty())
        assertEquals("Smileys and emotions", panel.browseSectionTitles().first())
    }

    @Test fun clearingRecentsWhileAnotherCategoryIsSelectedRemovesStaleSection() {
        val panel = panel()
        panel.onEmojiPicked = { true }
        panel.pickForTest("😀")
        panel.selectCategory(2)
        assertTrue(panel.browseSectionTitles().contains("Recently used"))
        assertTrue(EmojiPanelView.clearSavedRecents(RuntimeEnvironment.getApplication()))
        panel.reloadRecents()
        assertFalse("A retained panel must not resurrect erased recents",
            panel.browseSectionTitles().contains("Recently used"))
    }

    @Test fun pickingWhileFarDownStillMakesTheRecentTabAvailableImmediately() {
        val panel = panel()
        panel.onEmojiPicked = { true }
        panel.selectCategory(2)
        panel.pickForTest("🐶")
        assertEquals("🐶", panel.recentEmoji.first())
        panel.selectCategory(0)
        assertEquals("Recently used", panel.browseSectionTitles().first())
        val recentRow = list(panel).adapter.getView(1, null, list(panel)) as LinearLayout
        assertEquals("🐶", (recentRow.getChildAt(0) as TextView).text.toString())
    }

    @Test fun failedEditorCommitDoesNotPolluteRecents() {
        val panel = panel()
        panel.onEmojiPicked = { false }
        panel.pickForTest("😀")
        assertTrue(panel.recentEmoji.isEmpty())
        panel.selectCategory(0)
        assertEquals("Smileys and emotions", panel.browseSectionTitles().first())
    }

    @Test fun closeResetsSearchButDoesNotDiscardRecents() {
        val panel = panel()
        var closed = false
        panel.onClose = { closed = true }
        panel.setSearchMode(true)
        panel.appendQuery("cat")
        panel.close()
        assertTrue(closed)
        assertEquals("", panel.query)
        assertFalse(panel.isSearchMode)
    }

    @Test fun searchResultsAreOriginalEmojiRowsNotPlatformButtons() {
        val panel = panel()
        panel.setSearchMode(true)
        val list = list(panel)
        val row = list.adapter.getView(0, null, list) as LinearLayout
        assertTrue(row.getChildAt(0) is TextView)
    }
}
