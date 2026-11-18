package com.zaba.notez

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.zaba.notez.markdown.MarkdownPreviewRenderer
import com.zaba.notez.diagnostics.Breadcrumb
import com.zaba.notez.diagnostics.PerfTracker
import com.zaba.notez.diagnostics.installBreadcrumbLogcat
import android.os.SystemClock
import com.zaba.notez.music.MusicDrawerController
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Editor tanpa batas karakter + autosave 800ms.
 *
 * Dua mode:
 * - VIEW (default untuk catatan lama): judul & isi ditampilkan read-only, isi
 *   di-render sebagai Markdown lokal via WebView Reading View.
 * - EDIT (default untuk catatan baru, atau setelah tombol pensil ditekan):
 *   judul & isi jadi EditText biasa berisi teks Markdown mentah.
 */
class EditorActivity : AppCompatActivity() {

    private companion object {
        /**
         * Batas keras impor file. Dibatasi agar catatan raksasa tidak
         * menghabiskan RAM perangkat saat dibaca ke String sekaligus.
         */
        const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

        /** Identitas catatan aktif; harus bertahan saat layar diputar. */
        const val KEY_NOTE_ID = "state_note_id"
        const val KEY_EDITING = "state_editing"
        const val KEY_SCROLL_RATIO = "state_scroll_ratio"
    }

    private lateinit var dao: NoteDao
    private lateinit var drawer: DrawerLayout
    private lateinit var musicDrawer: MusicDrawerController
    private lateinit var markdownPreview: MarkdownPreviewRenderer
    private var perfTracker: PerfTracker? = null

    private lateinit var titleEdit: EditText
    private lateinit var titleView: TextView
    private lateinit var bodyEdit: EditText
    private lateinit var bodyWebView: WebView
    private lateinit var bodyEmptyView: TextView
    private lateinit var counter: TextView
    private lateinit var editToggle: ImageButton
    private var musicMiniPlayer: com.zaba.notez.music.MusicMiniPlayerHelper? = null

    private var noteId: Long = -1
    private var loaded = false
    private var isEditing = false
    private var suppressAutosave = false
    private var pendingScrollRatio: Float = 0f
    private var lastEditScrollRatio: Float = 0f

    // Nilai terakhir yang tersimpan/valid, dipakai saat render mode view.
    private var currentTitle = ""
    private var currentContent = ""

    /**
     * Snapshot isi yang benar-benar sudah ditulis ke [bodyEdit].
     *
     * Dipakai untuk melewati setText ulang pada catatan besar: menyalin 96 KB
     * yang isinya sama memaksa layout dokumen penuh untuk hasil yang identik.
     * Perbandingannya referensi/isi string, bukan text.toString(), supaya tidak
     * mengalokasikan salinan 96 KB di setiap toggle.
     */
    private var bodyTextSynced: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var saveTask: Runnable? = null
    private val saveMutex = Mutex()
    @Volatile private var saveGeneration = 0L

    private val openMusic = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (::musicDrawer.isInitialized) musicDrawer.onMusicPicked(uris)
    }

    private val saveAsLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        NoteFileTargetStore.setTargetUri(this, noteId, uri)
        writeToExternalUri(uri)
    }

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) importFromUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val openTime = intent.getLongExtra("open_started_at", SystemClock.uptimeMillis())
        noteId = intent.getLongExtra("note_id", -1)
        perfTracker = PerfTracker(noteId, openTime)
        setTheme(ThemePref.styleOf(ThemePref.get(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        installBreadcrumbLogcat()
        perfTracker?.mark(
            "ACTIVITY_CREATED",
            "note=$noteId editing=$isEditing rotation=${savedInstanceState != null}"
        )
        Breadcrumb.add(
            "EDITOR",
            "onCreate note=$noteId editing=$isEditing rotation=${savedInstanceState != null}"
        )
        dao = AppDatabase.get(this).noteDao()
        drawer = findViewById(R.id.drawer)
        UiStyle.applyDrawerGlass(findViewById(R.id.drawer_panel))
        drawer.post { perfTracker?.mark("LAYOUT_READY") }
        musicDrawer = MusicDrawerController(
            activity = this,
            root = drawer,
            onAddMusicRequested = { openMusic.launch(arrayOf("audio/*")) }
        )

        findViewById<View>(R.id.btn_back)?.setOnClickListener {
            onBackPressed()
        }
        val btnEditorDrawer = findViewById<ImageButton>(R.id.btn_editor_drawer)
        btnEditorDrawer?.setOnClickListener {
            if (drawer.isDrawerVisible(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            } else {
                drawer.openDrawer(GravityCompat.START)
            }
        }
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                btnEditorDrawer?.rotation = slideOffset * 180f
            }
            override fun onDrawerClosed(drawerView: View) {
                btnEditorDrawer?.rotation = 0f
            }
        })

        val miniPlayerView = findViewById<View>(R.id.editor_mini_player)
        if (miniPlayerView != null) {
            musicMiniPlayer = com.zaba.notez.music.MusicMiniPlayerHelper(
                activity = this,
                root = miniPlayerView,
                onOpenDrawer = { drawer.openDrawer(GravityCompat.START) }
            )
        }

        findViewById<View>(R.id.menu_file_save)?.setOnClickListener {
            perfTracker?.mark("TAP_MENU_SAVE_FILE")
            drawer.closeDrawer(GravityCompat.START)
            handleSaveFile()
        }
        findViewById<View>(R.id.menu_file_save_as)?.setOnClickListener {
            perfTracker?.mark("TAP_MENU_SAVE_AS")
            drawer.closeDrawer(GravityCompat.START)
            handleSaveAs()
        }
        noteId = intent.getLongExtra("note_id", -1)
        isEditing = intent.getBooleanExtra("is_new", false)
        // Setelah impor "Buka file", noteId bukan lagi milik intent: tanpa
        // restore ini, rotasi layar mengembalikan editor ke catatan lama.
        savedInstanceState?.let { state ->
            noteId = state.getLong(KEY_NOTE_ID, noteId)
            isEditing = state.getBoolean(KEY_EDITING, isEditing)
            val restoredRatio = state.getFloat(KEY_SCROLL_RATIO, 0f)
            if (restoredRatio > 0f) {
                pendingScrollRatio = restoredRatio
            }
        }

        titleEdit = findViewById(R.id.edit_title)
        titleView = findViewById(R.id.view_title)
        bodyEdit = findViewById(R.id.edit_body)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bodyEdit.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
            bodyEdit.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bodyEdit.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        (bodyEdit as? androidx.appcompat.widget.AppCompatEditText)?.isEmojiCompatEnabled = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bodyEdit.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                val layout = bodyEdit.layout
                if (layout != null) {
                    val visibleHeight = (bodyEdit.height - bodyEdit.totalPaddingTop - bodyEdit.totalPaddingBottom).coerceAtLeast(1)
                    val maxEditScroll = (layout.height - visibleHeight).coerceAtLeast(1)
                    lastEditScrollRatio = (scrollY.toFloat() / maxEditScroll).coerceIn(0f, 1f)
                }
            }
        }
        bodyWebView = findViewById(R.id.view_body_web)
        bodyEmptyView = findViewById(R.id.view_body_empty)
        counter = findViewById(R.id.counter)
        editToggle = findViewById(R.id.edit_toggle)
        markdownPreview = MarkdownPreviewRenderer(this, bodyWebView)
        perfTracker?.mark("WEBVIEW_CREATED")
        markdownPreview.perfTracker = perfTracker
        markdownPreview.onPaintReal = {
            if (!isEditing && pendingScrollRatio > 0f) {
                val ratio = pendingScrollRatio
                pendingScrollRatio = 0f
                markdownPreview.scrollToRatio(ratio)
            }
            if (!isEditing && bodyTextSynced != currentContent && currentContent.isNotEmpty()) {
                Looper.myQueue().addIdleHandler {
                    if (!isEditing && bodyTextSynced != currentContent && currentContent.isNotEmpty()) {
                        Breadcrumb.add("PERF", "PREWARM_EDIT_START chars=${currentContent.length}")
                        syncBodyText()
                        Breadcrumb.add("PERF", "PREWARM_EDIT_DONE chars=${currentContent.length}")
                    }
                    false
                }
            }
        }

        lifecycleScope.launch {
            dao.getById(noteId)?.let {
                currentTitle = it.title
                currentContent = it.content
                perfTracker?.mark("DB_QUERY_DONE", "chars=${it.content.length}")
                titleEdit.setText(it.title)
                // Batch 1: tunda setText body saat mode view; hanya diisi saat edit.
                // Body belum di-set di sini, jadi marker ini hanya mengukur judul.
                perfTracker?.mark("TITLE_TEXT_SET")
                updateCounter(it.content.length, "")
                perfTracker?.mark("COUNTER_DONE")
            }
            perfTracker?.mark("NOTE_LOADED", "chars=${currentContent.length} editing=$isEditing")
            loaded = true
            applyMode(isEditing)
            if (isEditing && pendingScrollRatio > 0f) {
                val ratio = pendingScrollRatio
                pendingScrollRatio = 0f
                bodyEdit.post {
                    val layout = bodyEdit.layout
                    if (layout != null && ratio > 0f) {
                        val targetLine = ((layout.lineCount - 1) * ratio).toInt().coerceIn(0, (layout.lineCount - 1).coerceAtLeast(0))
                        val lineTop = layout.getLineTop(targetLine)
                        val targetChar = layout.getLineStart(targetLine).coerceIn(0, bodyEdit.text?.length ?: 0)
                        bodyEdit.scrollTo(0, lineTop)
                        bodyEdit.setSelection(targetChar)
                    }
                }
            }
        }

        val schedule = {
            currentTitle = titleEdit.text.toString()
            currentContent = bodyEdit.text.toString()
            updateCounter(bodyEdit.text.length, " • menyimpan...")
            saveTask?.let(handler::removeCallbacks)
            saveTask = Runnable { save(currentTitle, currentContent) }
            handler.postDelayed(saveTask!!, 800)
        }
        titleEdit.doAfterTextChanged { if (loaded && !suppressAutosave) schedule() }
        bodyEdit.doAfterTextChanged { if (loaded && !suppressAutosave) schedule() }

        editToggle.setOnClickListener {
            if (isEditing) {
                perfTracker?.mark("EDIT_CLICK", "-> view chars=${bodyEdit.text.length}")
                switchToView()
            } else {
                // Sesi baru: marker EDIT_* tidak lagi hilang setelah PAINT_REAL.
                perfTracker?.beginInteractionSession("EDIT_CLICK")
                switchToEdit()
            }
        }
    }

    private fun switchToView() {
        saveTask?.let(handler::removeCallbacks)
        saveTask = null
        currentTitle = titleEdit.text.toString()
        currentContent = bodyEdit.text.toString()
        // Teks yang sama masih ada di EditText; catat supaya switchToEdit tidak
        // menyalin ulang isi yang identik.
        bodyTextSynced = currentContent

        val layout = bodyEdit.layout
        val ratioFromView = if (layout != null) {
            val visibleHeight = (bodyEdit.height - bodyEdit.totalPaddingTop - bodyEdit.totalPaddingBottom).coerceAtLeast(1)
            val maxEditScroll = (layout.height - visibleHeight).coerceAtLeast(1)
            (bodyEdit.scrollY.toFloat() / maxEditScroll).coerceIn(0f, 1f)
        } else 0f
        val ratioToRestore = if (ratioFromView > 0f) ratioFromView else lastEditScrollRatio
        pendingScrollRatio = ratioToRestore

        save(currentTitle, currentContent)
        isEditing = false
        titleEdit.clearFocus()
        bodyEdit.clearFocus()
        hideKeyboard()
        applyMode(false)
        perfTracker?.mark("VIEW_MODE_APPLIED", "chars=${currentContent.length}")
    }

    private fun switchToEdit() {
        isEditing = true
        suppressAutosave = true

        // Ambil rasio posisi scroll dari Reading View sebelum disembunyikan
        val scrollRatio = markdownPreview.currentScrollRatio()

        // Data perangkat: dari EDIT_CLICK ke EDIT_TEXT_SET ada 2,5–6,3 s tanpa
        // satu pun marker, jadi tidak jelas biayanya di setText judul, di
        // perbandingan isi, atau di setText body. Tiap langkah dipisah sekarang.
        perfTracker?.mark("EDIT_TITLE_SET_START", "chars=${currentTitle.length}")
        titleEdit.setText(currentTitle)
        perfTracker?.mark("EDIT_TITLE_SET_DONE")
        // Body hanya ditulis ulang kalau isinya memang berbeda (catatan besar:
        // setText = salinan + layout dokumen penuh).
        perfTracker?.mark("EDIT_SYNC_START", "syncedChars=${bodyTextSynced?.length ?: -1}")
        syncBodyText()
        perfTracker?.mark("EDIT_SYNC_DONE")
        titleEdit.setSelection(titleEdit.text?.length ?: 0)
        suppressAutosave = false
        applyMode(true)
        perfTracker?.mark("EDIT_MODE_APPLIED", "chars=${currentContent.length} titleChars=${currentTitle.length}")
        perfTracker?.mark("EDIT_VISIBLE")
        // Kursor, fokus, dan keyboard dikerjakan setelah view terlihat.
        // Sinkronkan ke posisi scroll terakhir dari Reading View; bila di paling atas,
        // pasang kursor di 0 tanpa scroll.
        bodyEdit.post {
            val layout = bodyEdit.layout
            if (layout != null && scrollRatio > 0f) {
                val targetLine = ((layout.lineCount - 1) * scrollRatio).toInt().coerceIn(0, (layout.lineCount - 1).coerceAtLeast(0))
                val lineTop = layout.getLineTop(targetLine)
                val targetChar = layout.getLineStart(targetLine).coerceIn(0, bodyEdit.text?.length ?: 0)
                bodyEdit.scrollTo(0, lineTop)
                bodyEdit.setSelection(targetChar)
            } else {
                bodyEdit.scrollTo(0, 0)
                bodyEdit.setSelection(0)
            }
            bodyEdit.requestFocus()
            showKeyboard(bodyEdit)
            perfTracker?.mark("KEYBOARD_REQUESTED")
            // Satu frame setelah keyboard diminta, supaya yang terukur adalah
            // layout dokumen, bukan antrean pesan main thread.
            bodyEdit.post {
                perfTracker?.mark("EDIT_LAYOUT_DONE")
                // Tutup sesi edit di sini; sebelumnya marker EDIT_* tidak pernah
                // tersimpan karena sesi render sudah selesai di PAINT_REAL.
                perfTracker?.finishSession()
            }
        }
    }

    /**
     * Samakan isi [bodyEdit] dengan [currentContent] hanya bila memang berbeda.
     *
     * `==` pada String yang sama referensinya langsung true, dan pada isi yang
     * berbeda hanya membandingkan karakter — tidak mengalokasikan salinan teks
     * seperti `bodyEdit.text.toString()`.
     */
    private fun syncBodyText() {
        if (bodyTextSynced == currentContent) {
            perfTracker?.mark("EDIT_TEXT_SKIP", "chars=${currentContent.length}")
            return
        }
        val prevSuppress = suppressAutosave
        suppressAutosave = true
        try {
            // START→END memisahkan biaya setText itu sendiri dari biaya langkah
            // sebelumnya (judul, perbandingan isi) di dalam sesi edit.
            perfTracker?.mark("EDIT_SETTEXT_START", "chars=${currentContent.length}")
            bodyEdit.setText(currentContent)
            bodyTextSynced = currentContent
            perfTracker?.mark("EDIT_SETTEXT_END", "chars=${currentContent.length}")
            perfTracker?.mark("EDIT_TEXT_SET", "chars=${currentContent.length}")
        } finally {
            suppressAutosave = prevSuppress
        }
    }

    /** Tampilkan set view yang sesuai mode, dan render Markdown saat masuk mode view. */
    private fun applyMode(editing: Boolean) {
        val hasContent = currentContent.isNotBlank()
        titleEdit.visibility = if (editing) View.VISIBLE else View.GONE
        titleView.visibility = if (editing) View.GONE else View.VISIBLE
        bodyEdit.visibility = if (editing) View.VISIBLE else View.GONE
        bodyWebView.visibility = if (!editing && hasContent) View.VISIBLE else View.GONE
        bodyEmptyView.visibility = if (!editing && !hasContent) View.VISIBLE else View.GONE
        counter.visibility = if (editing) View.VISIBLE else View.GONE
        editToggle.setImageResource(if (editing) R.drawable.ic_lucide_check else R.drawable.ic_lucide_pencil)
        editToggle.contentDescription =
            getString(if (editing) R.string.cd_done_editing else R.string.cd_edit_note)

        val miniPlayer = findViewById<View>(R.id.editor_mini_player)
        if (editing) {
            miniPlayer?.visibility = View.GONE
            // Rotasi bisa memulihkan isEditing=true sementara bodyEdit masih kosong
            // (setText body sengaja ditunda sampai mode edit). Tanpa isi ulang di
            // sini, onPause berikutnya menyimpan string kosong dan catatan hilang.
            // Hanya set kalau beda, supaya kursor tidak reset saat sudah sinkron.
            syncBodyText()
        }

        if (!editing) {
            musicMiniPlayer?.refresh()
            titleEdit.clearFocus()
            bodyEdit.clearFocus()
            hideKeyboard()
            titleView.text = currentTitle.ifBlank { getString(R.string.untitled_note) }
            if (hasContent) {
                perfTracker?.mark("RENDER_FROM_MODE", "chars=${currentContent.length}")
                markdownPreview.render(currentContent)
            } else {
                perfTracker?.mark("RENDER_SKIPPED_EMPTY")
            }
        }
    }

    private fun updateCounter(bodyLength: Int, suffix: String) {
        counter.text = "$bodyLength karakter$suffix"
    }

    @Synchronized
    private fun nextSaveGeneration(): Long {
        saveGeneration += 1
        return saveGeneration
    }

    private fun save(title: String, body: String) {
        val generation = nextSaveGeneration()
        perfTracker?.mark("SAVE_DISPATCH", "gen=$generation chars=${body.length}")
        // Tulis DB di IO thread — jangan block UI (penyebab scroll tersendat).
        // Generasi + mutex mencegah save lama menimpa snapshot yang lebih baru.
        lifecycleScope.launch(Dispatchers.IO) {
            saveSnapshot(title, body, generation, updateUi = true)
        }
    }

    private fun saveBlocking(title: String, body: String) {
        val generation = nextSaveGeneration()
        // onPause adalah batas persistence terakhir Activity. Save ini sengaja
        // menunggu DB write selesai agar snapshot terakhir tidak ikut tercancel
        // bersama lifecycleScope saat Activity dihancurkan.
        runBlocking(Dispatchers.IO) {
            saveSnapshot(title, body, generation, updateUi = false)
        }
    }

    private suspend fun saveSnapshot(
        title: String,
        body: String,
        generation: Long,
        updateUi: Boolean
    ) {
        var saved = false
        saveMutex.lock()
        try {
            if (generation == saveGeneration) {
                val existing = dao.getById(noteId)
                if (existing != null && generation == saveGeneration) {
                    dao.update(existing.copy(title = title, content = body, updatedAt = System.currentTimeMillis()))
                    saved = true
                }
            }
        } finally {
            saveMutex.unlock()
        }
        if (saved) {
            Breadcrumb.add("IO", "DB_WRITE gen=$generation chars=${body.length}")
        } else {
            Breadcrumb.add("IO", "DB_WRITE_DILEWATI gen=$generation latest=$saveGeneration")
        }
        if (saved && updateUi && generation == saveGeneration) {
            withContext(Dispatchers.Main) {
                if (isEditing && generation == saveGeneration) updateCounter(body.length, " • tersimpan")
            }
        }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val token = currentFocus?.windowToken ?: window.decorView.windowToken
        imm.hideSoftInputFromWindow(token, 0)
    }

    private fun handleSaveFile() {
        val targetUri = NoteFileTargetStore.getTargetUri(this, noteId)
        Breadcrumb.add("EDITOR", "SAVE_FILE target=${targetUri != null} note=$noteId")
        if (targetUri != null) {
            writeToExternalUri(targetUri)
        } else {
            handleSaveAs()
        }
    }

    private fun handleSaveAs() {
        val title = if (isEditing) titleEdit.text.toString() else currentTitle
        val body = if (isEditing) bodyEdit.text.toString() else currentContent
        val suggestedName = NoteFileTargetStore.suggestFilename(title, body)
        perfTracker?.mark("SAVE_AS_LAUNCH", "name=$suggestedName chars=${body.length}")
        saveAsLauncher.launch(suggestedName)
    }

    /** Hasil bacaan satu file yang akan diimpor menjadi catatan baru. */
    private sealed interface ImportResult {
        data class Success(val title: String, val content: String) : ImportResult
        object TooLarge : ImportResult
        object Failed : ImportResult
    }

    private fun handleOpenFile() {
        // "text/*" menangkap .md/.txt dari SAF; octet-stream menangkap provider
        // yang tidak memberi MIME jelas (mis. beberapa penyimpanan awan).
        perfTracker?.mark("OPEN_FILE_LAUNCH")
        openFileLauncher.launch(arrayOf("text/*", "application/octet-stream"))
    }

    /**
     * Baca file eksternal menjadi catatan baru, lalu ikat catatan itu ke URI
     * sumbernya sehingga "Simpan file" menulis balik ke file yang sama.
     */
    private fun importFromUri(uri: Uri) {
        perfTracker?.mark("IMPORT_PICKED", uri.toString())
        // Jangan buang edit yang belum tersimpan di catatan yang sedang terbuka:
        // noteId akan berganti, jadi simpan sekarang sebelum konteks berpindah.
        saveTask?.let(handler::removeCallbacks)
        saveTask = null
        saveBlocking(currentTitle, currentContent)

        val fallbackTitle = getString(R.string.untitled_note)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { readMarkdown(uri, fallbackTitle) }
                .getOrDefault(ImportResult.Failed)
            perfTracker?.mark(
                "IMPORT_READ_DONE",
                when (result) {
                    is ImportResult.Success -> "chars=${result.content.length} title=${result.title}"
                    ImportResult.TooLarge -> "too_large"
                    ImportResult.Failed -> "failed"
                }
            )
            when (result) {
                is ImportResult.Success -> {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    val newId = runCatching {
                        dao.upsert(
                            Note(
                                title = result.title,
                                content = result.content,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }.getOrElse {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@EditorActivity,
                                R.string.open_file_failed,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }
                    NoteFileTargetStore.setTargetUri(this@EditorActivity, newId, uri)
                    perfTracker?.mark("IMPORT_NOTE_CREATED", "note=$newId chars=${result.content.length}")
                    withContext(Dispatchers.Main) { bindImportedNote(newId, result) }
                }

                ImportResult.TooLarge -> withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditorActivity,
                        R.string.open_file_too_large,
                        Toast.LENGTH_LONG
                    ).show()
                }

                ImportResult.Failed -> withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditorActivity,
                        R.string.open_file_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Baca byte file lalu ubah jadi teks. Ukuran dibatasi [MAX_IMPORT_BYTES]. */
    private fun readMarkdown(uri: Uri, fallbackTitle: String): ImportResult {
        val displayName = contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            .orEmpty()

        val bytes = contentResolver.openInputStream(uri)?.use { stream ->
            val out = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                if (out.size() + read > MAX_IMPORT_BYTES) return ImportResult.TooLarge
                out.write(chunk, 0, read)
            }
            out.toByteArray()
        } ?: return ImportResult.Failed

        val title = displayName.substringBeforeLast('.').trim().ifBlank { fallbackTitle }
        return ImportResult.Success(title, decodeText(bytes))
    }

    /** UTF-8 ketat; file lama yang bukan UTF-8 tetap ditampilkan, bukan gagal total. */
    private fun decodeText(bytes: ByteArray): String = try {
        Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        String(bytes, Charsets.ISO_8859_1)
    }

    /** Pasang catatan hasil impor ke UI. Catatan tetap bisa diedit seperti biasa. */
    private fun bindImportedNote(newId: Long, imported: ImportResult.Success) {
        noteId = newId
        isEditing = false
        pendingScrollRatio = 0f
        lastEditScrollRatio = 0f
        markdownPreview.scrollToRatio(0f)
        currentTitle = imported.title
        currentContent = imported.content
        suppressAutosave = true
        titleEdit.setText(imported.title)
        // Batch 1: body diisi saat pindah ke edit (switchToEdit).
        suppressAutosave = false
        updateCounter(imported.content.length, "")
        loaded = true
        perfTracker?.mark("IMPORT_BOUND", "note=$newId chars=${imported.content.length}")
        applyMode(false)
        Toast.makeText(this, R.string.open_file_done, Toast.LENGTH_SHORT).show()
    }

    private fun writeToExternalUri(uri: Uri) {
        val title = if (isEditing) titleEdit.text.toString() else currentTitle
        val body = if (isEditing) bodyEdit.text.toString() else currentContent
        perfTracker?.mark("FILE_WRITE_START", "chars=${body.length} uri=$uri")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }
                perfTracker?.mark("FILE_WRITE_DONE", "chars=${body.length}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditorActivity, "File tersimpan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                perfTracker?.mark("FILE_WRITE_FAIL", e.localizedMessage.orEmpty())
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditorActivity, "Gagal menyimpan file: " + e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::drawer.isInitialized && drawer.isDrawerVisible(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        perfTracker?.mark("ACTIVITY_PAUSE", "editing=$isEditing note=$noteId")
        saveTask?.let(handler::removeCallbacks)
        saveTask = null
        val title = if (isEditing) titleEdit.text.toString() else currentTitle
        val body = if (isEditing) bodyEdit.text.toString() else currentContent
        currentTitle = title
        currentContent = body
        saveBlocking(title, body)
        perfTracker?.mark("SAVE_BLOCKING_DONE", "chars=${body.length}")
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_NOTE_ID, noteId)
        outState.putBoolean(KEY_EDITING, isEditing)
        val currentRatio = if (isEditing) {
            val layout = bodyEdit.layout
            if (layout != null) {
                val visibleHeight = (bodyEdit.height - bodyEdit.totalPaddingTop - bodyEdit.totalPaddingBottom).coerceAtLeast(1)
                val maxEditScroll = (layout.height - visibleHeight).coerceAtLeast(1)
                (bodyEdit.scrollY.toFloat() / maxEditScroll).coerceIn(0f, 1f)
            } else lastEditScrollRatio
        } else {
            markdownPreview.currentScrollRatio()
        }
        outState.putFloat(KEY_SCROLL_RATIO, currentRatio)
        Breadcrumb.add("EDITOR", "onSaveInstanceState note=$noteId editing=$isEditing ratio=$currentRatio")
    }

    override fun onDestroy() {
        perfTracker?.mark("ACTIVITY_DESTROY", "note=$noteId editing=$isEditing")
        // Sesi yang belum selesai (render/impor berhenti sebelum PAINT_REAL, atau
        // sesi edit yang post()-nya belum jalan) harus tetap disimpan. Tanpa ini
        // timeline-nya hilang tanpa jejak: di data perangkat, impor 100 KB yang
        // ditinggal sebelum PAINT_REAL tidak pernah muncul di Diagnostics.
        perfTracker?.finishSession()
        Breadcrumb.add("EDITOR", "onDestroy note=$noteId")
        musicMiniPlayer?.destroy()
        musicMiniPlayer = null
        if (::musicDrawer.isInitialized) musicDrawer.destroy()
        if (::markdownPreview.isInitialized) markdownPreview.destroy()
        super.onDestroy()
    }
}
