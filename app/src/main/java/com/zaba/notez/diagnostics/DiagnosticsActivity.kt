package com.zaba.notez.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.zaba.notez.R
import com.zaba.notez.ThemePref
import com.zaba.notez.UiStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostics Console: Terminal ADB Logcat style emulator with syntax highlighting,
 * performance trace summaries, multi-tag filtering, and ANR-safe windowing.
 */
class DiagnosticsActivity : AppCompatActivity() {

    enum class LogFilter(val label: String) {
        ALL("SEMUA"),
        PERF("PERF"),
        HOME("HOME"),
        EDITOR("EDITOR"),
        MUSIC("MUSIC"),
        CRASH("CRASH")
    }

    private lateinit var filterContainer: LinearLayout
    private lateinit var terminalConsoleText: TextView
    private lateinit var terminalPrompt: TextView
    private lateinit var btnLoadMore: TextView
    private lateinit var emptyDiagnostics: TextView
    private lateinit var logCountMeta: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnShare: Button
    private lateinit var btnClear: Button

    private var currentFilter = LogFilter.ALL
    private var renderWindowSize = RENDER_WINDOW_DEFAULT
    private var cachedFullText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemePref.styleOf(ThemePref.get(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener { finish() }

        filterContainer = findViewById(R.id.filter_container)
        terminalConsoleText = findViewById(R.id.terminal_console_text)
        terminalPrompt = findViewById(R.id.terminal_prompt)
        btnLoadMore = findViewById(R.id.btn_load_more)
        emptyDiagnostics = findViewById(R.id.empty_diagnostics)
        logCountMeta = findViewById(R.id.log_count_meta)
        btnCopy = findViewById(R.id.btn_copy)
        btnShare = findViewById(R.id.btn_share)
        btnClear = findViewById(R.id.btn_clear)

        applyScreenPolish()
        setupFilterChips()

        btnLoadMore.setOnClickListener {
            renderWindowSize += RENDER_WINDOW_DEFAULT
            renderDiagnostics()
        }

        btnCopy.setOnClickListener { copyToClipboard() }
        btnShare.setOnClickListener { shareDiagnostics() }
        btnClear.setOnClickListener {
            PerfStore.clear()
            Breadcrumb.clear()
            renderDiagnostics()
            Toast.makeText(this, "Diagnostics dibersihkan", Toast.LENGTH_SHORT).show()
        }

        Breadcrumb.add("DIAG", "DiagnosticsActivity dibuka (Logcat Console)")
        renderDiagnostics()
    }

    private fun applyScreenPolish() {
        findViewById<View>(R.id.diagnostics_toolbar)?.background =
            UiStyle.roundedBackground(this, fillAlpha = 78, strokeAlpha = 52, radiusDp = 18f)

        val terminalCard = findViewById<MaterialCardView>(R.id.terminal_card)
        if (terminalCard != null) {
            terminalCard.setCardBackgroundColor(Color.parseColor("#080C12"))
            val primary = UiStyle.primary(this)
            val outline = UiStyle.outline(this)
            terminalCard.strokeColor = ColorUtils.compositeColors(
                ColorUtils.setAlphaComponent(primary, 60),
                ColorUtils.setAlphaComponent(outline, 80)
            )
            terminalCard.strokeWidth = UiStyle.dp(this, 1f)
        }

        findViewById<View>(R.id.diagnostics_actions)?.let { actions ->
            actions.background = UiStyle.roundedBackground(this, fillAlpha = 42, strokeAlpha = 34, radiusDp = 16f)
            actions.setPadding(
                UiStyle.dp(this, 12f),
                UiStyle.dp(this, 8f),
                UiStyle.dp(this, 12f),
                UiStyle.dp(this, 8f)
            )
        }

        findViewById<View>(R.id.btn_load_more)?.background =
            UiStyle.roundedBackground(this, fillAlpha = 40, strokeAlpha = 45, radiusDp = 8f)
    }

    private fun setupFilterChips() {
        filterContainer.removeAllViews()
        LogFilter.values().forEach { filter ->
            val chip = TextView(this).apply {
                text = filter.label
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setPadding(
                    UiStyle.dp(this@DiagnosticsActivity, 14f),
                    UiStyle.dp(this@DiagnosticsActivity, 6f),
                    UiStyle.dp(this@DiagnosticsActivity, 14f),
                    UiStyle.dp(this@DiagnosticsActivity, 6f)
                )
                val isSelected = filter == currentFilter
                if (isSelected) {
                    val primaryColor = UiStyle.primary(this@DiagnosticsActivity)
                    val onPrimaryColor = UiStyle.resolveColor(
                        this@DiagnosticsActivity,
                        com.google.android.material.R.attr.colorOnPrimary,
                        Color.WHITE
                    )
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = UiStyle.dp(this@DiagnosticsActivity, 14f).toFloat()
                        setColor(primaryColor)
                    }
                    setTextColor(onPrimaryColor)
                    setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
                } else {
                    background = UiStyle.roundedBackground(
                        this@DiagnosticsActivity,
                        fillAlpha = 40,
                        strokeAlpha = 50,
                        radiusDp = 14f
                    )
                    setTextColor(
                        UiStyle.resolveColor(
                            this@DiagnosticsActivity,
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            Color.LTGRAY
                        )
                    )
                    setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                setOnClickListener {
                    currentFilter = filter
                    renderWindowSize = RENDER_WINDOW_DEFAULT
                    setupFilterChips()
                    renderDiagnostics()
                }
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = UiStyle.dp(this@DiagnosticsActivity, 8f) }
            filterContainer.addView(chip, lp)
        }
    }

    private fun renderDiagnostics() {
        val sessions = PerfStore.getSessions()
        val allBreadcrumbs = Breadcrumb.snapshot()

        // Filter breadcrumbs
        val filteredBreadcrumbs = when (currentFilter) {
            LogFilter.ALL -> allBreadcrumbs
            LogFilter.PERF -> allBreadcrumbs.filter { it.area.contains("PERF", ignoreCase = true) }
            LogFilter.HOME -> allBreadcrumbs.filter { it.area.contains("HOME", ignoreCase = true) }
            LogFilter.EDITOR -> allBreadcrumbs.filter { it.area.contains("EDITOR", ignoreCase = true) }
            LogFilter.MUSIC -> allBreadcrumbs.filter { it.area.contains("MUSIC", ignoreCase = true) }
            LogFilter.CRASH -> allBreadcrumbs.filter {
                val upper = it.message.uppercase(Locale.US)
                upper.contains("CRASH") || upper.contains("FATAL") || upper.contains("ERROR") ||
                    upper.contains("FAIL") || upper.contains("EXCEPTION") || upper.contains("THROWABLE")
            }
        }

        val showTrace = (currentFilter == LogFilter.ALL || currentFilter == LogFilter.PERF) && sessions.isNotEmpty()
        val totalCount = filteredBreadcrumbs.size + (if (showTrace) sessions.size else 0)

        if (totalCount == 0) {
            terminalConsoleText.visibility = View.GONE
            emptyDiagnostics.visibility = View.VISIBLE
            emptyDiagnostics.text = "(tidak ada rekaman log untuk filter '${currentFilter.label}')"
            btnLoadMore.visibility = View.GONE
            logCountMeta.text = "0 events"
            btnCopy.isEnabled = false
            btnShare.isEnabled = false
            cachedFullText = ""
            return
        }

        terminalConsoleText.visibility = View.VISIBLE
        emptyDiagnostics.visibility = View.GONE
        btnCopy.isEnabled = true
        btnShare.isEnabled = true

        val displayedBreadcrumbs = filteredBreadcrumbs.takeLast(renderWindowSize)
        val hasMore = filteredBreadcrumbs.size > displayedBreadcrumbs.size

        if (hasMore) {
            btnLoadMore.visibility = View.VISIBLE
            btnLoadMore.text = "▲ [ADB BUFFER: Menampilkan ${displayedBreadcrumbs.size} dari ${filteredBreadcrumbs.size} baris — Ketuk untuk muat $RENDER_WINDOW_DEFAULT lebih lama]"
        } else {
            btnLoadMore.visibility = View.GONE
        }

        logCountMeta.text = buildString {
            append("${displayedBreadcrumbs.size} / ${filteredBreadcrumbs.size} events")
            val dropped = Breadcrumb.droppedCount()
            if (dropped > 0) append(" ($dropped dropped)")
        }

        // Build master spannable output for logcat window
        val spanBuilder = SpannableStringBuilder()
        val plainBuilder = StringBuilder()

        plainBuilder.appendLine("--------- beginning of NOTEZ logcat (${currentFilter.label})")

        // 1. Render Systrace Summary if applicable
        if (showTrace) {
            val traceText = buildTraceSummary(sessions)
            appendColoredLine(spanBuilder, traceText, Color.parseColor("#38BDF8"))
            plainBuilder.appendLine(traceText)
        }

        // 2. Render Logcat lines
        displayedBreadcrumbs.forEach { entry ->
            val level = determineLogLevel(entry.area, entry.message)
            val levelColor = colorForLevel(level, entry.area)
            val timeStr = clock(entry.wallClockMs)
            val tagStr = "$level/NOTEZ_${entry.area.uppercase(Locale.US)}"
            val threadStr = if (entry.thread == "main") "" else " [${entry.thread}]"

            // Plain text line for clipboard/share
            plainBuilder.appendLine("$timeStr $tagStr: ${entry.message}$threadStr")

            // Spannable formatted line
            val lineStart = spanBuilder.length

            // Timestamp (gray/teal)
            spanBuilder.append(timeStr)
            spanBuilder.setSpan(
                ForegroundColorSpan(Color.parseColor("#88AAB5")),
                lineStart,
                spanBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spanBuilder.append(" ")

            // Level and Tag (color coded)
            val tagStart = spanBuilder.length
            spanBuilder.append(tagStr)
            spanBuilder.setSpan(
                ForegroundColorSpan(levelColor),
                tagStart,
                spanBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spanBuilder.append(": ")

            // Message body (light frost)
            val msgStart = spanBuilder.length
            spanBuilder.append(entry.message)
            spanBuilder.setSpan(
                ForegroundColorSpan(Color.parseColor("#E0E6ED")),
                msgStart,
                spanBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Thread info if not main
            if (threadStr.isNotEmpty()) {
                val threadStart = spanBuilder.length
                spanBuilder.append(threadStr)
                spanBuilder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#667788")),
                    threadStart,
                    spanBuilder.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            spanBuilder.append("\n")
        }

        cachedFullText = plainBuilder.toString()
        terminalConsoleText.text = spanBuilder
    }

    private fun buildTraceSummary(sessions: List<PerfSession>): String = buildString {
        appendLine("================ PERFORMANCE TRACE SUMMARY ================")
        sessions.take(MAX_SESSIONS_SUMMARY).forEachIndexed { index, session ->
            appendLine(
                "#${index + 1} ${session.label.uppercase(Locale.US)} · " +
                    "${clock(session.startedAtWallClock)} · " +
                    "note ${session.noteId ?: "N/A"} · " +
                    "${session.durationMs}ms · ${session.events.size} event" +
                    (if (session.droppedEvents > 0) " (+${session.droppedEvents} dropped)" else "")
            )
            session.events.forEachIndexed { eventIndex, event ->
                val prev = session.events.getOrNull(eventIndex - 1)?.elapsedMs ?: 0L
                val isLast = eventIndex == session.events.lastIndex
                val treePrefix = if (isLast) "   └─ " else "   ├─ "
                appendLine(
                    String.format(
                        Locale.US,
                        "%s%-24s +%4dms (Δ%4dms)%s%s",
                        treePrefix,
                        event.name,
                        event.elapsedMs,
                        event.elapsedMs - prev,
                        if (event.thread != "main") " [${event.thread}]" else "",
                        if (event.detail.isNotBlank()) " · ${event.detail}" else ""
                    )
                )
            }
            if (index < sessions.take(MAX_SESSIONS_SUMMARY).lastIndex) {
                appendLine("-----------------------------------------------------------")
            }
        }
        appendLine("===========================================================")
    }

    private fun determineLogLevel(area: String, message: String): String {
        val upper = message.uppercase(Locale.US)
        return when {
            upper.contains("CRASH") || upper.contains("FATAL") || upper.contains("ERROR") ||
                upper.contains("FAIL") || upper.contains("EXCEPTION") || upper.contains("THROWABLE") -> "E"
            upper.contains("WARN") || upper.contains("SLOW") || upper.contains("DROP") ||
                upper.contains("ANR") -> "W"
            upper.contains("PAINT_REAL") || upper.contains("READY") || upper.contains("SUCCESS") ||
                upper.contains("INIT") -> "I"
            area.equals("MUSIC", ignoreCase = true) -> "D"
            else -> "V"
        }
    }

    private fun colorForLevel(level: String, area: String): Int = when (level) {
        "E" -> Color.parseColor("#FF6B6B") // Coral Red
        "W" -> Color.parseColor("#FFC600") // Amber
        "I" -> Color.parseColor("#39FF14") // Neon Green
        "D" -> if (area.equals("MUSIC", ignoreCase = true)) Color.parseColor("#38BDF8") else Color.parseColor("#7AA2F7")
        else -> Color.parseColor("#A8E6CF") // Mint
    }

    private fun appendColoredLine(builder: SpannableStringBuilder, text: String, color: Int) {
        val start = builder.length
        builder.append(text)
        if (!text.endsWith("\n")) builder.append("\n")
        builder.setSpan(
            ForegroundColorSpan(color),
            start,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NOTEZ Logcat Diagnostics", cachedFullText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Logcat diagnostics disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareDiagnostics() {
        if (cachedFullText.isBlank()) return
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, cachedFullText)
            putExtra(Intent.EXTRA_TITLE, "NOTEZ Logcat Diagnostics")
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Bagikan NOTEZ Diagnostics")
        startActivity(chooser)
    }

    private fun clock(wallClockMs: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(wallClockMs))

    private companion object {
        const val RENDER_WINDOW_DEFAULT = 500
        const val MAX_SESSIONS_SUMMARY = 10
    }
}
