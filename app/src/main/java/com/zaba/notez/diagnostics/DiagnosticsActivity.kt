package com.zaba.notez.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zaba.notez.R
import com.zaba.notez.ThemePref
import com.zaba.notez.UiStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostics: riwayat sesi performa + breadcrumb global.
 *
 * Riwayat (bukan hanya sesi terakhir) penting karena satu sesi catatan bisa
 * diikuti sesi cache-hit kecil, dan membuka catatan lain membuat tracker baru.
 * Breadcrumb global menampilkan tap dan lifecycle yang terjadi di luar tracker.
 */
class DiagnosticsActivity : AppCompatActivity() {

    private lateinit var sessionMeta: TextView
    private lateinit var timelineContainer: LinearLayout
    private lateinit var breadcrumbLabel: TextView
    private lateinit var breadcrumbMeta: TextView
    private lateinit var breadcrumbContainer: LinearLayout
    private lateinit var emptyDiagnostics: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnClear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemePref.styleOf(ThemePref.get(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)
        applyScreenPolish()

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        sessionMeta = findViewById(R.id.session_meta)
        timelineContainer = findViewById(R.id.timeline_container)
        breadcrumbLabel = findViewById(R.id.breadcrumb_label)
        breadcrumbMeta = findViewById(R.id.breadcrumb_meta)
        breadcrumbContainer = findViewById(R.id.breadcrumb_container)
        emptyDiagnostics = findViewById(R.id.empty_diagnostics)
        btnCopy = findViewById(R.id.btn_copy)
        btnClear = findViewById(R.id.btn_clear)

        btnCopy.setOnClickListener { copyBreadcrumbToClipboard() }
        btnClear.setOnClickListener {
            PerfStore.clear()
            Breadcrumb.clear()
            renderDiagnostics()
            Toast.makeText(this, "Diagnostics dibersihkan", Toast.LENGTH_SHORT).show()
        }

        Breadcrumb.add("DIAG", "DiagnosticsActivity dibuka")
        renderDiagnostics()
    }

    private fun applyScreenPolish() {
        findViewById<View>(R.id.diagnostics_toolbar).background =
            UiStyle.roundedBackground(this, fillAlpha = 78, strokeAlpha = 52, radiusDp = 18f)
        listOf(R.id.session_meta, R.id.breadcrumb_meta).forEach { id ->
            findViewById<TextView>(id).background =
                UiStyle.roundedBackground(this, fillAlpha = 52, strokeAlpha = 42, radiusDp = 16f)
            findViewById<TextView>(id).setPadding(
                UiStyle.dp(this, 14f),
                UiStyle.dp(this, 12f),
                UiStyle.dp(this, 14f),
                UiStyle.dp(this, 12f)
            )
        }
        findViewById<TextView>(R.id.empty_diagnostics).background =
            UiStyle.roundedBackground(this, fillAlpha = 52, strokeAlpha = 42, radiusDp = 18f)
        findViewById<View>(R.id.diagnostics_actions).background =
            UiStyle.roundedBackground(this, fillAlpha = 42, strokeAlpha = 34, radiusDp = 16f)
        findViewById<View>(R.id.diagnostics_actions).setPadding(
            UiStyle.dp(this, 10f),
            UiStyle.dp(this, 6f),
            UiStyle.dp(this, 10f),
            UiStyle.dp(this, 6f)
        )
        findViewById<Button>(R.id.btn_copy).text = "SALIN"
        findViewById<Button>(R.id.btn_clear).text = "BERSIHKAN"
    }

    private fun renderDiagnostics() {
        val sessions = PerfStore.getSessions()
        val breadcrumbs = Breadcrumb.snapshot()
        timelineContainer.removeAllViews()
        breadcrumbContainer.removeAllViews()

        if (sessions.isEmpty() && breadcrumbs.isEmpty()) {
            sessionMeta.visibility = View.GONE
            timelineContainer.visibility = View.GONE
            breadcrumbLabel.visibility = View.GONE
            breadcrumbMeta.visibility = View.GONE
            breadcrumbContainer.visibility = View.GONE
            emptyDiagnostics.visibility = View.VISIBLE
            btnCopy.isEnabled = false
            btnClear.isEnabled = false
            return
        }

        emptyDiagnostics.visibility = View.GONE
        btnCopy.isEnabled = true
        btnClear.isEnabled = true

        if (sessions.isEmpty()) {
            sessionMeta.visibility = View.GONE
            timelineContainer.visibility = View.GONE
        } else {
            sessionMeta.visibility = View.VISIBLE
            timelineContainer.visibility = View.VISIBLE
            sessionMeta.text = buildString {
                append("${sessions.size} sesi tersimpan, terbaru di atas")
                if (sessions.size > MAX_RENDERED_SESSIONS) {
                    append(" • hanya $MAX_RENDERED_SESSIONS terbaru ditampilkan")
                }
            }
            sessions.take(MAX_RENDERED_SESSIONS).forEachIndexed { index, session ->
                addSessionHeader(session, index + 1)
                session.events.forEachIndexed { eventIndex, event ->
                    val previous = session.events.getOrNull(eventIndex - 1)?.elapsedMs ?: 0L
                    addRow(
                        container = timelineContainer,
                        title = "● ${event.name}",
                        subtitle = buildString {
                            append("+${event.elapsedMs} ms")
                            append(" · Δ ${event.elapsedMs - previous} ms")
                            if (event.thread != "main") append(" · ${event.thread}")
                            if (event.detail.isNotBlank()) append(" · ${event.detail}")
                        },
                        titleColor = getColor(ThemePref.optionOf(ThemePref.get(this)).textColorRes)
                    )
                }
                if (session.droppedEvents > 0) {
                    addInfoLine(timelineContainer, "${session.droppedEvents} event dipotong (batas per sesi)")
                }
                if (session.events.isEmpty()) {
                    addInfoLine(timelineContainer, "sesi kosong")
                }
            }
        }

        if (breadcrumbs.isEmpty()) {
            breadcrumbLabel.visibility = View.GONE
            breadcrumbMeta.visibility = View.GONE
            breadcrumbContainer.visibility = View.GONE
        } else {
            breadcrumbLabel.visibility = View.VISIBLE
            breadcrumbMeta.visibility = View.VISIBLE
            breadcrumbContainer.visibility = View.VISIBLE
            breadcrumbMeta.text = buildString {
                append("${breadcrumbs.size} kejadian terakhir, urut lama → baru")
                val dropped = Breadcrumb.droppedCount()
                if (dropped > 0) append(" • $dropped lama dibuang")
            }
            breadcrumbs.takeLast(MAX_RENDERED_BREADCRUMBS).forEach { entry ->
                addRow(
                    container = breadcrumbContainer,
                    title = "${clock(entry.wallClockMs)} · ${entry.area}",
                    subtitle = entry.message + if (entry.thread == "main") "" else " · ${entry.thread}",
                    titleColor = UiStyle.primary(this)
                )
            }
        }
    }

    private fun addSessionHeader(session: PerfSession, index: Int) {
        val view = TextView(this)
        view.text = buildString {
            append("#$index · ${session.label.uppercase(Locale.US)}")
            append(" · ${clock(session.startedAtWallClock)}")
            append(" · note ${session.noteId ?: "N/A"}")
            append("\n${session.durationMs} ms · ${session.events.size} event")
            if (session.droppedEvents > 0) append(" (+${session.droppedEvents} dipotong)")
        }
        view.setTextColor(UiStyle.primary(this))
        view.textSize = 14f
        view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
        view.background = UiStyle.roundedBackground(this, fillAlpha = 64, strokeAlpha = 46, radiusDp = 16f)
        view.setPadding(
            UiStyle.dp(this, 14f),
            UiStyle.dp(this, 10f),
            UiStyle.dp(this, 14f),
            UiStyle.dp(this, 10f)
        )
        timelineContainer.addView(
            view,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = UiStyle.dp(this@DiagnosticsActivity, if (index == 1) 4f else 20f)
                bottomMargin = UiStyle.dp(this@DiagnosticsActivity, 8f)
            }
        )
    }

    private fun addRow(
        container: LinearLayout,
        title: String,
        subtitle: String,
        titleColor: Int
    ) {
        val itemView = layoutInflater.inflate(
            android.R.layout.simple_list_item_2,
            container,
            false
        )
        val text1 = itemView.findViewById<TextView>(android.R.id.text1)
        val text2 = itemView.findViewById<TextView>(android.R.id.text2)

        text1.text = title
        text1.setTextColor(titleColor)
        text1.textSize = 15f

        text2.text = subtitle
        text2.setTextColor(UiStyle.primary(this))
        text2.textSize = 12f
        text2.setTextIsSelectable(true)

        itemView.background = UiStyle.roundedBackground(
            this,
            fillAlpha = 42,
            strokeAlpha = 34,
            radiusDp = 14f
        )
        itemView.setPadding(
            UiStyle.dp(this, 14f),
            UiStyle.dp(this, 8f),
            UiStyle.dp(this, 14f),
            UiStyle.dp(this, 8f)
        )
        container.addView(
            itemView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = UiStyle.dp(this@DiagnosticsActivity, 6f) }
        )
    }

    private fun addInfoLine(container: LinearLayout, text: String) {
        val view = TextView(this)
        view.text = text
        view.setTextColor(getColor(ThemePref.optionOf(ThemePref.get(this)).secondaryColorRes))
        view.textSize = 12f
        view.setPadding(
            UiStyle.dp(this, 14f),
            UiStyle.dp(this, 2f),
            UiStyle.dp(this, 14f),
            UiStyle.dp(this, 6f)
        )
        container.addView(view)
    }

    private fun copyBreadcrumbToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NOTEZ Diagnostics", buildPlainText())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Diagnostics tersalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    /** Format teks yang sama dengan tampilan, supaya bisa ditempel ke chat. */
    private fun buildPlainText(): String {
        val sb = StringBuilder()
        sb.appendLine("NOTEZ Diagnostics")
        sb.appendLine()

        PerfStore.getSessions().forEachIndexed { index, session ->
            sb.appendLine(
                "#${index + 1} ${session.label.uppercase(Locale.US)} · " +
                    "${clock(session.startedAtWallClock)} · " +
                    "note ${session.noteId ?: "N/A"} · " +
                    "${session.durationMs}ms · ${session.events.size} event" +
                    (if (session.droppedEvents > 0) " (+${session.droppedEvents} dipotong)" else "")
            )
            session.events.forEachIndexed { eventIndex, event ->
                val previous = session.events.getOrNull(eventIndex - 1)?.elapsedMs ?: 0L
                sb.appendLine(
                    buildString {
                        append("${event.name} +${event.elapsedMs}ms")
                        append(" Δ${event.elapsedMs - previous}")
                        if (event.thread != "main") append(" [${event.thread}]")
                        if (event.detail.isNotBlank()) append(" · ${event.detail}")
                    }
                )
            }
            sb.appendLine()
        }

        val breadcrumbs = Breadcrumb.snapshot()
        if (breadcrumbs.isNotEmpty()) {
            sb.appendLine("Breadcrumb Global (${breadcrumbs.size} kejadian)")
            breadcrumbs.forEach { entry ->
                sb.appendLine(
                    "${clock(entry.wallClockMs)} [${entry.area}] ${entry.message}" +
                        (if (entry.thread == "main") "" else " (${entry.thread})")
                )
            }
        }
        return sb.toString()
    }

    private fun clock(wallClockMs: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(wallClockMs))

    private companion object {
        const val MAX_RENDERED_SESSIONS = 15
        const val MAX_RENDERED_BREADCRUMBS = 300
    }
}
