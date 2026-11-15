package tribixbite.cleverkeys.estonianui

/** This geometry is the sole owner of draw, touch, accessibility and swipe coordinates. */
enum class KeyboardLanguage { ESTONIAN, ENGLISH }
enum class KeyKind { LETTER, NUMBER, SYMBOL, SHIFT, BACKSPACE, SYMBOLS, COMMA, EMOJI, LANGUAGE, SPACE, PERIOD, ENTER, SPACER }
enum class NumericLayout { PIN, INTEGER, DECIMAL, SIGNED, SIGNED_DECIMAL, PHONE, DATE, TIME, DATETIME }

data class KeyBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
}

data class SurfaceKey(val label: String, val kind: KeyKind, val bounds: KeyBounds, val row: Int) {
    /** Original alternate glyphs, displayed only on the alphabetic page. */
    val secondarySymbol: String? get() = if (kind != KeyKind.LETTER) null else when (label) {
        "q" -> "%"; "w" -> "\\"; "e" -> "|"; "r" -> "="; "t" -> "["
        "y" -> "]"; "u" -> "<"; "i" -> ">"; "o" -> "{"; "p" -> "}"; "ü" -> "°"
        "a" -> "@"; "s" -> "#"; "d" -> "€"; "f" -> "_"; "g" -> "&"
        "h" -> "-"; "j" -> "+"; "k" -> "("; "l" -> ")"; "ö" -> "/"; "ä" -> "~"
        "z" -> "*"; "x" -> "\""; "c" -> "'"; "v" -> ":"; "b" -> ";"
        "n" -> "!"; "m" -> "?"; "õ" -> "\\"
        else -> null
    }
    val accessibilityLabel: String get() = when (kind) {
        KeyKind.SHIFT -> "Shift"
        KeyKind.BACKSPACE -> "Backspace"
        KeyKind.SYMBOLS -> if (label == "ABC") "Letters" else "Symbols"
        KeyKind.EMOJI -> "Emoji"
        KeyKind.LANGUAGE -> "Switch language"
        KeyKind.SPACE -> "Space, $label"
        KeyKind.ENTER -> "Enter"
        else -> label
    }
}

class KeyboardGeometry(val width: Int, val height: Int, val language: KeyboardLanguage,
                       val symbolMode: Boolean = false, val numericLayout: NumericLayout? = null) {
    init { require(width > 0 && height > 0) }
    private data class Spec(val label: String, val kind: KeyKind, val weight: Int = 1)
    private fun letters(text: String) = text.map { Spec(it.toString(), KeyKind.LETTER) }
    private fun symbols(text: String) = text.map { Spec(it.toString(), KeyKind.SYMBOL) }
    private fun optionalSymbol(label: String?) = if (label == null) Spec("", KeyKind.SPACER)
        else Spec(label, KeyKind.SYMBOL)
    private val definitions: List<List<Spec>> = if (numericLayout == NumericLayout.PIN) listOf(
        listOf("1", "2", "3").map { Spec(it, KeyKind.NUMBER) },
        listOf("4", "5", "6").map { Spec(it, KeyKind.NUMBER) },
        listOf("7", "8", "9").map { Spec(it, KeyKind.NUMBER) },
        listOf(Spec("⌫", KeyKind.BACKSPACE), Spec("0", KeyKind.NUMBER), Spec("↵", KeyKind.ENTER))
    ) else if (numericLayout in setOf(NumericLayout.PHONE, NumericLayout.DATE,
            NumericLayout.TIME, NumericLayout.DATETIME)) listOf(
        listOf(Spec("1", KeyKind.NUMBER), Spec("2", KeyKind.NUMBER), Spec("3", KeyKind.NUMBER),
            optionalSymbol(if (numericLayout == NumericLayout.PHONE || numericLayout == NumericLayout.DATE ||
                numericLayout == NumericLayout.DATETIME) "-" else null)),
        listOf("4", "5", "6").map { Spec(it, KeyKind.NUMBER) } +
            optionalSymbol(if (numericLayout == NumericLayout.PHONE) "+" else null),
        listOf("7", "8", "9").map { Spec(it, KeyKind.NUMBER) } + Spec("⌫", KeyKind.BACKSPACE),
        listOf(optionalSymbol(when (numericLayout) {
            NumericLayout.PHONE -> "*"; NumericLayout.DATE, NumericLayout.DATETIME -> "/"
            NumericLayout.TIME -> ":"; else -> null
        }), Spec("0", KeyKind.NUMBER), optionalSymbol(when (numericLayout) {
            NumericLayout.PHONE -> "#"; NumericLayout.DATETIME -> ":"; else -> null
        }), Spec("↵", KeyKind.ENTER))
    ) else if (numericLayout != null) listOf(
        listOf(Spec("1", KeyKind.NUMBER), Spec("2", KeyKind.NUMBER), Spec("3", KeyKind.NUMBER),
            if (numericLayout == NumericLayout.SIGNED || numericLayout == NumericLayout.SIGNED_DECIMAL)
                Spec("-", KeyKind.SYMBOL) else Spec("", KeyKind.SPACER)),
        listOf("4", "5", "6").map { Spec(it, KeyKind.NUMBER) } + Spec("", KeyKind.SPACER),
        listOf("7", "8", "9").map { Spec(it, KeyKind.NUMBER) } + Spec("⌫", KeyKind.BACKSPACE),
        listOf(Spec("", KeyKind.SPACER), Spec("0", KeyKind.NUMBER),
            if (numericLayout == NumericLayout.DECIMAL || numericLayout == NumericLayout.SIGNED_DECIMAL)
                Spec(".", KeyKind.SYMBOL) else Spec("", KeyKind.SPACER),
            Spec("↵", KeyKind.ENTER))
    ) else listOf(
        "1234567890".map { Spec(it.toString(), KeyKind.NUMBER) },
        if (symbolMode) symbols("@#€%&*-+()") else letters(if (language == KeyboardLanguage.ESTONIAN) "qwertyuiopü" else "qwertyuiop"),
        if (symbolMode) symbols(":;/\\'\"=<>!?") else letters(if (language == KeyboardLanguage.ESTONIAN) "asdfghjklöä" else "asdfghjkl"),
        if (symbolMode) symbols("$£¥_[]{}~^") + Spec("⌫", KeyKind.BACKSPACE, 2)
        else listOf(Spec("⇧", KeyKind.SHIFT, 2)) + letters(if (language == KeyboardLanguage.ESTONIAN) "zxcvbnmõ" else "zxcvbnm") + Spec("⌫", KeyKind.BACKSPACE, 2),
        listOf(Spec(if (symbolMode) "ABC" else "?123", KeyKind.SYMBOLS, 3), Spec(",", KeyKind.COMMA, 2),
            Spec("☺", KeyKind.EMOJI, 2),
            Spec(if (language == KeyboardLanguage.ESTONIAN) " Eesti " else " English ", KeyKind.SPACE, 9),
            Spec(".", KeyKind.PERIOD, 2), Spec("↵", KeyKind.ENTER, 3))
    )
    val rows: List<List<SurfaceKey>> = definitions.mapIndexed { row, specs ->
        val top = row * height / definitions.size
        val bottom = (row + 1) * height / definitions.size
        val total = specs.sumOf { it.weight }
        var used = 0
        specs.map { spec ->
            val left = used * width / total
            used += spec.weight
            SurfaceKey(spec.label, spec.kind, KeyBounds(left, top, used * width / total, bottom), row)
        }
    }
    val keys: List<SurfaceKey> = rows.flatten()
    fun hit(x: Float, y: Float): SurfaceKey? {
        if (!x.isFinite() || !y.isFinite() || x < 0 || y < 0 || x >= width || y >= height) return null
        return rows.firstOrNull { row -> y >= row[0].bounds.top && y < row[0].bounds.bottom }
            ?.firstOrNull { it.kind != KeyKind.SPACER && it.bounds.contains(x, y) }
    }
}
