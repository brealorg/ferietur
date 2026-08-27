package app.ferietur.ui

internal data class FlowChromeProgress(
    val current: Int,
    val total: Int,
) {
    val label: String get() = "Steg $current av $total"
    val fraction: Float get() = if (total <= 0) 0f else (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

internal fun flowChromeProgress(stepLabel: String): FlowChromeProgress? {
    val match = Regex("^(\\d+) av (\\d+)$").matchEntire(stepLabel.trim()) ?: return null
    val current = match.groupValues[1].toIntOrNull() ?: return null
    val total = match.groupValues[2].toIntOrNull() ?: return null
    if (current < 1 || total < 1 || current > total) return null
    return FlowChromeProgress(current = current, total = total)
}
