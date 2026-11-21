package tribixbite.cleverkeys.estonianemoji

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import tribixbite.cleverkeys.R

/** Inline emoji browser with virtualized, labeled rows. It never focuses an editor inside the IME. */
class EmojiPanelView(context: Context) : LinearLayout(context) {
    companion object {
        private const val RECENTS_FILE = "emoji_browser_recents"
        private val TONE_MODIFIERS = 0x1F3FB..0x1F3FF
        /** Compare only actual catalog sequences; never synthesize a missing ZWJ emoji. */
        private fun toneBase(emoji: String): String = buildString {
            var offset = 0
            while (offset < emoji.length) {
                val point = emoji.codePointAt(offset)
                // The neutral glyph may carry VS16 while its toned forms omit it.
                if (point !in TONE_MODIFIERS && point != 0xFE0F) appendCodePoint(point)
                offset += Character.charCount(point)
            }
        }
        /** Deliberate erasure from Settings, not a text-field editing action. */
        fun clearSavedRecents(context: Context): Boolean = runCatching {
            context.getSharedPreferences(RECENTS_FILE, Context.MODE_PRIVATE)
                .edit().remove("entries").commit()
        }.getOrDefault(false)
    }
    /** Return true only after the editor confirms commit; failed picks are not recent. */
    var onEmojiPicked: ((String) -> Boolean)? = null
    var onClose: (() -> Unit)? = null
    /** Browse-only key: remove from the external editor, never the search query or recents. */
    var onEditorDelete: (() -> Unit)? = null
    /** Host resizes panel/keyboard: browse 334dp, search 100dp, stable total envelope. */
    var onSearchModeChanged: ((Boolean) -> Unit)? = null
    var isSearchMode = false
        private set
    var query: String = ""
        private set
    var visibleEmoji: List<String> = emptyList()
        private set
    internal val recentEmoji: List<String> get() = recent.toList()
    private var index: OfflineEmojiSearch? = null
    private val catalog = loadCatalog(context)
    private val toneGroups: Map<String, List<String>> = catalog.flatten().groupBy(::toneBase)
    private val preferences = context.getSharedPreferences(RECENTS_FILE, Context.MODE_PRIVATE)
    private val appearance = context.getSharedPreferences("emoji_browser_appearance", Context.MODE_PRIVATE)
    private var chosenTone = appearance.getInt("skin_tone", 0).coerceIn(0, 5)
    private fun savedRecent(): List<String> = preferences.getString("entries", "").orEmpty().split('\n')
        .filter { it.isNotEmpty() && it.length <= 32 && '\r' !in it }.distinct().take(48)
    private val recent = savedRecent().toMutableList()
    private var category = 0
    private var sectionStarts = IntArray(13) { -1 }
    private var rowCategories = IntArray(0)
    private val list = ListView(context)
    private var tonePopup: LinearLayout? = null
    private val content = object : FrameLayout(context) {
        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) tonePopup?.let { popup ->
                val params = popup.layoutParams as? FrameLayout.LayoutParams
                val left = if (popup.width > 0) popup.left else params?.leftMargin ?: 0
                val top = if (popup.height > 0) popup.top else params?.topMargin ?: 0
                val right = left + (if (popup.width > 0) popup.width else (params?.width ?: 0))
                val bottom = top + (if (popup.height > 0) popup.height else (params?.height ?: 0))
                if (event.x < left || event.x >= right || event.y < top || event.y >= bottom) {
                    dismissTonePicker() // Let the same touch continue to the underlying catalog.
                }
            }
            return super.onInterceptTouchEvent(event)
        }
    }
    private val categoryBar = HorizontalScrollView(context)
    private val browseBar = LinearLayout(context)
    private val queryBar = LinearLayout(context)
    private val footer = LinearLayout(context)
    private val queryLabel = TextView(context)
    private val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    private val surfaceColor = if (dark) 0xff202127.toInt() else 0xffeef3fb.toInt()
    private val tileColor = if (dark) 0xff303239.toInt() else Color.WHITE
    private val selectedColor = if (dark) 0xff586d98.toInt() else 0xffd3e3fd.toInt()
    private val labelColor = if (dark) 0xfff0f1f5.toInt() else 0xff202124.toInt()
    private val categories = listOf("◷" to "Recently used", "☻" to "Smileys and emotions", "♙" to "People",
        "♧" to "Animals and nature", "◉" to "Food and drink", "⚽" to "Activities",
        "▣" to "Travel and places", "♢" to "Objects", "⚑" to "Symbols",
        "▦" to "Flags", "✦" to "New emoji", ":-)" to "Emoticons", "(＾)" to "Kaomoji")

    private sealed class Row {
        data class Header(val title: String) : Row()
        data class Emojis(val entries: List<String>, val columns: Int = 8) : Row()
    }
    /** Original geometric cue, not an image asset; recycled cells reset it on every bind. */
    private inner class EmojiCell(context: Context) : TextView(context) {
        var toneMarker = false
        private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val markerPath = Path()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!toneMarker) return
            val edge = dp(4).toFloat()
            val size = dp(7).toFloat()
            val x = width - edge
            val y = height - edge
            markerPath.reset()
            markerPath.moveTo(x - size, y)
            markerPath.lineTo(x, y - size)
            markerPath.lineTo(x, y)
            markerPath.close()
            markerPaint.color = if (dark) 0xffaeb8c7.toInt() else 0xff758298.toInt()
            canvas.drawPath(markerPath, markerPaint)
        }
    }
    private var rows: List<Row> = emptyList()
    internal fun browseSectionTitles() = rows.filterIsInstance<Row.Header>().map { it.title }
    private val adapter = object : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(position: Int): Any = rows[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = when (val row = rows[position]) {
            is Row.Header -> ((convertView as? TextView) ?: TextView(context).apply {
                textSize = 12f
                letterSpacing = 0.12f
                setTextColor(labelColor)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, 0, 0)
                layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28))
            }).apply { text = row.title.uppercase() }
            is Row.Emojis -> ((convertView as? LinearLayout)?.takeIf { it.childCount == row.columns }
                ?: LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(45))
                repeat(row.columns) {
                    addView(EmojiCell(context).apply {
                        textSize = if (row.columns == 2) 16f else 27f
                        gravity = Gravity.CENTER
                    }, LayoutParams(0, dp(43), 1f))
                }
            }).apply {
                for (i in 0 until row.columns) {
                    (getChildAt(i) as EmojiCell).apply {
                        val emoji = row.entries.getOrNull(i)
                        val hasTones = emoji != null && toneGroups[toneBase(emoji)].orEmpty().size > 1
                        toneMarker = hasTones
                        invalidate()
                        text = emoji.orEmpty()
                        background = null
                        contentDescription = emoji?.let { context.getString(if (hasTones)
                            R.string.swipe_emoji_tone_hold else R.string.swipe_emoji_item_description, it) }
                        isFocusable = emoji != null
                        isClickable = emoji != null
                        setOnClickListener(if (emoji == null) null else OnClickListener { pickForTest(emoji) })
                        setOnLongClickListener(if (!hasTones || emoji == null) null
                            else OnLongClickListener { showTonePicker(this, emoji) })
                        isLongClickable = hasTones
                    }
                }
            }
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(surfaceColor)
        isFocusable = false
        browseBar.gravity = Gravity.CENTER_VERTICAL
        browseBar.addView(action("⌕  " + context.getString(R.string.swipe_emoji_browse_search), "Search emoji") {
            setSearchMode(true)
        }.apply { textSize = 16f; background = rounded(tileColor) }, LayoutParams(0, dp(42), 1.2f))
        categoryBar.isHorizontalScrollBarEnabled = false
        browseBar.addView(categoryBar, LayoutParams(0, dp(44), 1f))
        queryBar.gravity = Gravity.CENTER_VERTICAL
        queryLabel.setTextColor(labelColor)
        queryLabel.textSize = 16f
        queryLabel.gravity = Gravity.CENTER_VERTICAL
        queryLabel.setPadding(dp(12), 0, 0, 0)
        queryLabel.contentDescription = context.getString(R.string.swipe_emoji_query_description)
        queryBar.addView(queryLabel, LayoutParams(0, dp(44), 1f))
        queryBar.addView(action("⌫", context.getString(R.string.swipe_emoji_backspace)) { backspaceQuery() },
            LayoutParams(dp(48), dp(44)))
        queryBar.addView(action("×", "Browse emoji") { setSearchMode(false) }, LayoutParams(dp(48), dp(44)))
        addView(browseBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
        list.divider = null
        list.setSelector(android.R.color.transparent)
        list.isVerticalScrollBarEnabled = false
        list.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) dismissTonePicker()
            }
            override fun onScroll(view: AbsListView, firstVisibleItem: Int,
                                  visibleItemCount: Int, totalItemCount: Int) {
                syncCategoryFromScroll(firstVisibleItem)
            }
        })
        list.adapter = adapter
        content.addView(list, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        footer.addView(action("ABC", "Return to keyboard") { close() }, LayoutParams(dp(64), dp(48)))
        footer.addView(action("⌕", "Search emoji") { setSearchMode(true) }, LayoutParams(dp(60), dp(48)))
        footer.addView(View(context), LayoutParams(0, dp(48), 1f))
        footer.addView(action("⌫", context.getString(R.string.swipe_emoji_delete_editor)) {
            onEditorDelete?.invoke()
        }, LayoutParams(dp(58), dp(48)))
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        render()
    }

    fun setSearchIndex(search: OfflineEmojiSearch?) { index = search; render() }
    fun selectCategory(number: Int) {
        if (number !in categories.indices) return
        dismissTonePicker()
        if (isSearchMode) setSearchMode(false)
        val target = sectionStarts[number].takeIf { it >= 0 } ?: sectionStarts[1]
        updateSelectedCategory(if (sectionStarts[number] >= 0) number else 1)
        if (target >= 0) list.setSelectionFromTop(target, 0)
    }
    fun setSearchMode(enabled: Boolean) {
        dismissTonePicker()
        if (isSearchMode == enabled) return
        isSearchMode = enabled
        query = ""
        removeViewAt(0)
        addView(if (enabled) queryBar else browseBar, 0,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
        footer.visibility = if (enabled) GONE else VISIBLE
        content.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, if (enabled) dp(56) else 0,
            if (enabled) 0f else 1f)
        onSearchModeChanged?.invoke(enabled)
        render()
        list.setSelection(0)
    }
    fun appendQuery(text: String) {
        if (text.isEmpty()) return
        if (!isSearchMode) setSearchMode(true)
        query = (query + text.replace('\n', ' ').replace('\r', ' ')).take(64)
        render()
    }
    fun backspaceQuery() {
        if (query.isEmpty()) return
        query = query.substring(0, query.offsetByCodePoints(query.length, -1))
        render()
    }
    fun clearQuery() { query = ""; render() }
    fun close() { setSearchMode(false); clearQuery(); onClose?.invoke() }
    /** A Settings erasure may have happened while an older IME view was retained. */
    fun reloadRecents() {
        recent.clear()
        recent.addAll(savedRecent())
        renderPreservingBrowsePosition()
    }
    /** Only emoji choices, never surrounding editor text. */
    fun clearRecent() {
        if (!clearSavedRecents(context)) return
        recent.clear()
        renderPreservingBrowsePosition()
    }
    internal fun pickForTest(emoji: String) {
        if (emoji !in visibleEmoji) return
        dismissTonePicker()
        commitPicked(emoji)
    }
    private fun commitPicked(emoji: String, refresh: Boolean = true): Boolean {
        if (onEmojiPicked?.invoke(emoji) != true) return false
        if (emoji.length <= 32 && '\n' !in emoji && '\r' !in emoji) {
            recent.remove(emoji)
            recent.add(0, emoji)
            while (recent.size > 48) recent.removeAt(recent.lastIndex)
            runCatching { preferences.edit().putString("entries", recent.joinToString("\n")).apply() }
            // The editor already accepted the emoji; storage failure cannot undo that write.
        }
        if (refresh && !isSearchMode) renderPreservingBrowsePosition()
        return true
    }
    /** A temporary overlay stays inside the IME; it cannot focus or cover the host editor. */
    private fun showTonePicker(cell: TextView, emoji: String): Boolean {
        val options = toneGroups[toneBase(emoji)].orEmpty().distinct()
        if (options.size < 2) return false
        dismissTonePicker()
        val columns = minOf(6, options.size)
        val popup = LinearLayout(context).apply {
            tag = "emoji_tone_popup"
            orientation = if (options.size > 6) VERTICAL else HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(tileColor)
            elevation = dp(6).toFloat()
            isClickable = true // Empty margins never fall through to the emoji catalog.
            isFocusable = false
        }
        fun choiceView(choice: String) = TextView(context).apply {
            text = choice
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(labelColor)
            val toneLabels = intArrayOf(R.string.swipe_emoji_tone_neutral, R.string.swipe_emoji_tone_light,
                R.string.swipe_emoji_tone_medium_light, R.string.swipe_emoji_tone_medium,
                R.string.swipe_emoji_tone_medium_dark, R.string.swipe_emoji_tone_dark)
            val optionTone = toneOf(choice)
            contentDescription = if (optionTone == null)
                context.getString(R.string.swipe_emoji_item_description, choice)
            else context.getString(R.string.swipe_emoji_tone_choice, choice,
                context.getString(toneLabels[optionTone]))
            isFocusable = true
            setOnClickListener {
                dismissTonePicker()
                if (commitPicked(choice, refresh = false)) { // No state changes after a failed editor write.
                    val nextTone = toneOf(choice)
                    if (nextTone != null && runCatching {
                        appearance.edit().putInt("skin_tone", nextTone).commit()
                    }.getOrDefault(false)) {
                        chosenTone = nextTone
                    }
                    if (!isSearchMode) renderPreservingBrowsePosition()
                }
            }
        }
        if (options.size <= 6) {
            options.forEach { popup.addView(choiceView(it), LinearLayout.LayoutParams(0, dp(48), 1f)) }
        } else {
            // Mixed-person sequences can have 26 real variants. Keep all of them reachable
            // in a bounded, scrollable grid instead of silently dropping the last 20.
            val grid = LinearLayout(context).apply { orientation = VERTICAL }
            options.chunked(columns).forEach { group ->
                val row = LinearLayout(context)
                group.forEach { row.addView(choiceView(it), LinearLayout.LayoutParams(0, dp(48), 1f)) }
                repeat(columns - group.size) {
                    row.addView(View(context), LinearLayout.LayoutParams(0, dp(48), 1f))
                }
                grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
            }
            popup.addView(ScrollView(context).apply {
                isVerticalScrollBarEnabled = true
                addView(grid)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val bounds = Rect()
        cell.getDrawingRect(bounds)
        content.offsetDescendantRectToMyCoords(cell, bounds)
        val width = minOf(dp(48) * columns, (content.width - dp(8)).coerceAtLeast(dp(48)))
        val wantedHeight = if (options.size <= 6) dp(50) else dp(48) * ((options.size + columns - 1) / columns)
        val height = minOf(wantedHeight, (content.height - dp(8)).coerceAtLeast(dp(50)))
        val left = (bounds.centerX() - width / 2).coerceIn(0, (content.width - width).coerceAtLeast(0))
        content.addView(popup, FrameLayout.LayoutParams(width, height).apply {
            leftMargin = left
            topMargin = (bounds.top - height - dp(2)).coerceIn(0,
                (content.height - height).coerceAtLeast(0))
        })
        tonePopup = popup
        return true
    }
    fun dismissTonePicker() {
        tonePopup?.let { content.removeView(it) }
        tonePopup = null
    }
    override fun onDetachedFromWindow() {
        dismissTonePicker()
        super.onDetachedFromWindow()
    }
    internal fun containsEditableTextField(): Boolean {
        fun scan(view: View): Boolean = when (view) {
            is EditText -> true
            is ViewGroup -> (0 until view.childCount).any { scan(view.getChildAt(it)) }
            else -> false
        }
        return scan(this)
    }
    /** A changing recent row must not pull a distant catalog section back to the top. */
    private fun renderPreservingBrowsePosition() {
        if (isSearchMode) { render(); return }
        val first = list.firstVisiblePosition
        val top = list.getChildAt(0)?.top ?: 0
        val oldSmileys = sectionStarts[1]
        val wasInRecents = oldSmileys > 0 && first < oldSmileys
        render()
        if (rows.isEmpty()) return
        val shift = sectionStarts[1] - oldSmileys
        val target = if (wasInRecents) {
            if (sectionStarts[0] < 0) 0 else first
        } else first + shift
        list.setSelectionFromTop(target.coerceIn(0, rows.lastIndex), top)
    }
    private fun render() {
        queryLabel.text = if (query.isEmpty()) context.getString(R.string.swipe_emoji_search_hint) else query
        rows = if (isSearchMode) {
            visibleEmoji = if (query.isBlank()) recent.ifEmpty { catalog.getOrElse(1) { emptyList() } }.take(36)
                else index?.search(query, 36).orEmpty()
            visibleEmoji.chunked(8).map(Row::Emojis)
        } else {
            val sections = buildList {
                if (recent.isNotEmpty()) add(0 to ("Recently used" to recent.toList()))
                for (number in 1 until categories.size) {
                    add(number to (categories[number].second to compactCategory(emojisForCategory(number))))
                }
            }
            val allRows = ArrayList<Row>()
            val owners = ArrayList<Int>()
            val starts = IntArray(categories.size) { -1 }
            for ((number, section) in sections) {
                starts[number] = allRows.size
                allRows.add(Row.Header(section.first))
                owners.add(number)
                val columns = if (number == 12) 2 else 8
                for (chunk in section.second.chunked(columns)) {
                    allRows.add(Row.Emojis(chunk, columns))
                    owners.add(number)
                }
            }
            sectionStarts = starts
            rowCategories = owners.toIntArray()
            visibleEmoji = sections.flatMap { it.second.second }
            allRows
        }
        adapter.notifyDataSetChanged()
        if (!isSearchMode) renderCategories()
    }
    /** A shared preference affects only uniform tone families; mixed pairs stay searchable. */
    private fun toneOf(emoji: String): Int? {
        var tone = 0
        var offset = 0
        while (offset < emoji.length) {
            val point = emoji.codePointAt(offset)
            if (point in TONE_MODIFIERS) {
                val next = point - 0x1F3FA
                if (tone != 0 && tone != next) return null
                tone = next
            }
            offset += Character.charCount(point)
        }
        return tone
    }
    private fun compactCategory(source: List<String>): List<String> = source.distinctBy(::toneBase).map { emoji ->
        val base = toneBase(emoji)
        val family = toneGroups[base].orEmpty()
        family.firstOrNull { toneOf(it) == chosenTone } ?: family.firstOrNull { it == base } ?: emoji
    }
    private fun emojisForCategory(number: Int): List<String> = when (number) {
        11 -> catalog.getOrElse(11) { emptyList() }.takeWhile { item -> item.all { it.code < 128 } }
        12 -> catalog.getOrElse(11) { emptyList() }.dropWhile { item -> item.all { it.code < 128 } }
        else -> catalog.getOrElse(number) { emptyList() }
    }
    /** Called by the actual ListView listener; kept deterministic for Robolectric tests. */
    internal fun syncCategoryFromScroll(firstVisibleItem: Int) {
        if (!isSearchMode && firstVisibleItem in rowCategories.indices) {
            updateSelectedCategory(rowCategories[firstVisibleItem])
        }
    }
    private fun updateSelectedCategory(number: Int) {
        if (number !in categories.indices) return
        val tabs = categoryBar.getChildAt(0) as? LinearLayout
        if (category == number && tabs?.getChildAt(number)?.isSelected == true) return
        category = number
        if (tabs == null) return
        for (index in 0 until tabs.childCount) {
            val tab = tabs.getChildAt(index)
            tab.isSelected = index == number
            tab.background = rounded(if (index == number) selectedColor else surfaceColor)
        }
        // Keep the active icon reachable without resetting the user's horizontal rail position.
        categoryBar.post {
            val tab = tabs.getChildAt(number) ?: return@post
            if (tab.right > categoryBar.scrollX + categoryBar.width) {
                categoryBar.smoothScrollTo(tab.right - categoryBar.width, 0)
            } else if (tab.left < categoryBar.scrollX) {
                categoryBar.smoothScrollTo(tab.left, 0)
            }
        }
    }
    private fun renderCategories() {
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        categories.forEachIndexed { index, (icon, description) ->
            row.addView(action(icon, description) { selectCategory(index) }.apply {
                background = rounded(if (category == index) selectedColor else surfaceColor)
                isSelected = category == index
            }, LayoutParams(dp(43), dp(42)))
        }
        categoryBar.removeAllViews()
        categoryBar.addView(row)
    }
    private fun action(label: String, description: String, click: () -> Unit) = TextView(context).apply {
        text = label
        textSize = if (label.length > 3) 14f else 21f
        gravity = Gravity.CENTER
        setTextColor(labelColor)
        background = rounded(surfaceColor)
        contentDescription = description
        isFocusable = true
        setOnClickListener { click() }
    }
    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(8).toFloat()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun loadCatalog(context: Context): List<List<String>> {
        val lines = context.resources.openRawResource(R.raw.emojis).bufferedReader(Charsets.UTF_8)
            .use { it.readLines() }
        val bounds = lines.last().trim().split(Regex("\\s+")).map { it.toInt() }
        return listOf(emptyList<String>()) + bounds.indices.map { group ->
            lines.subList(bounds[group], bounds.getOrElse(group + 1) { lines.lastIndex })
                .filter { it.isNotBlank() }
        }
    }
}
