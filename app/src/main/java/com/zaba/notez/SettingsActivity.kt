package com.zaba.notez

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zaba.notez.markdown.RemoteImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var dao: NoteDao
    private var pendingExport: String? = null
    private var appliedTheme: Int = ThemePref.GITHUB_DARK

    private val createDoc = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val data = pendingExport ?: return@registerForActivityResult
        pendingExport = null
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
            launch(Dispatchers.Main) { toast("Diekspor") }
        }
    }

    private val openDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            val raw = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            val notes = try { BackupHelper.fromJson(raw.orEmpty()) } catch (_: Exception) { null }
            if (notes == null) {
                launch(Dispatchers.Main) { toast("File backup tidak valid") }
                return@launch
            }
            for (note in notes) dao.upsert(note)
            launch(Dispatchers.Main) { toast("${notes.size} catatan dipulihkan") }
        }
    }

    private val openTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        BackupHelper.setTree(this, uri)
        toast("Folder backup otomatis aktif")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedTheme = ThemePref.get(this)
        setTheme(ThemePref.styleOf(appliedTheme))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        dao = AppDatabase.get(this).noteDao()
        applyScreenPolish()

        findViewById<ImageButton>(R.id.settings_back).setOnClickListener { finish() }
        findViewById<View>(R.id.settings_row_theme).setOnClickListener {
            startActivity(Intent(this, ThemePickerActivity::class.java))
        }
        findViewById<View>(R.id.settings_row_export_json).setOnClickListener { exportJson() }
        findViewById<View>(R.id.settings_row_import_json).setOnClickListener {
            openDoc.launch(arrayOf("application/json"))
        }
        findViewById<View>(R.id.settings_row_export_txt).setOnClickListener { exportTxt() }
        findViewById<View>(R.id.settings_row_auto_backup_folder).setOnClickListener {
            openTree.launch(null)
        }
        findViewById<View>(R.id.settings_row_about_notez).setOnClickListener {
            startActivity(Intent(this, AboutNotezActivity::class.java))
        }
        findViewById<View>(R.id.settings_row_privacy).setOnClickListener { showPrivacyDialog() }
        findViewById<View>(R.id.settings_row_clear_remote_image_cache).setOnClickListener { confirmClearRemoteImageCache() }

        setupExpandableSections()
        updateSummaries()
    }

    override fun onResume() {
        super.onResume()
        if (ThemePref.get(this) != appliedTheme) {
            recreate()
            return
        }
        updateSummaries()
    }


    private fun applyScreenPolish() {
        val headers = listOf(
            R.id.section_header_appearance,
            R.id.section_header_data,
            R.id.section_header_app
        )
        headers.forEach { id ->
            val header = findViewById<View>(id)
            header.background = selectableItemBackground()
            (header.layoutParams as? LinearLayout.LayoutParams)?.apply {
                setMargins(0, dp(2), 0, dp(2))
            }
        }

        val contentIds = listOf(
            R.id.section_content_appearance,
            R.id.section_content_data,
            R.id.section_content_app
        )
        contentIds.forEach { id ->
            val content = findViewById<View>(id)
            content.background = UiStyle.roundedBackground(this, fillAlpha = 34, strokeAlpha = 34, radiusDp = 16f)
            (content.layoutParams as? LinearLayout.LayoutParams)?.apply {
                setMargins(dp(2), dp(4), dp(2), dp(6))
            }
        }
    }

    private fun setupExpandableSections() {
        val headerApp = findViewById<View>(R.id.section_header_appearance)
        val contentApp = findViewById<View>(R.id.section_content_appearance)
        val indicatorApp = findViewById<ImageView>(R.id.section_indicator_appearance)

        val headerData = findViewById<View>(R.id.section_header_data)
        val contentData = findViewById<View>(R.id.section_content_data)
        val indicatorData = findViewById<ImageView>(R.id.section_indicator_data)

        val headerMain = findViewById<View>(R.id.section_header_app)
        val contentMain = findViewById<View>(R.id.section_content_app)
        val indicatorMain = findViewById<ImageView>(R.id.section_indicator_app)

        headerApp.setOnClickListener {
            toggleSection(contentApp, indicatorApp)
        }
        headerData.setOnClickListener {
            toggleSection(contentData, indicatorData)
        }
        headerMain.setOnClickListener {
            toggleSection(contentMain, indicatorMain)
        }
    }

    private fun toggleSection(contentView: View, indicatorView: ImageView) {
        val willShow = contentView.visibility != View.VISIBLE
        contentView.visibility = if (willShow) View.VISIBLE else View.GONE
        indicatorView.animate()
            .rotation(if (willShow) 180f else 0f)
            .setDuration(200L)
            .start()
    }

    private fun updateSummaries() {
        findViewById<TextView>(R.id.settings_theme_value).text = ThemePref.nameOf(ThemePref.get(this))
        findViewById<TextView>(R.id.settings_version_value).text = versionName()
    }

    private fun selectableItemBackground() = android.util.TypedValue().let { outValue ->
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        getDrawable(outValue.resourceId)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun exportJson() {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = BackupHelper.toJson(dao.getAllNow())
            launch(Dispatchers.Main) {
                pendingExport = data
                createDoc.launch(BackupHelper.fileName("json"))
            }
        }
    }

    private fun exportTxt() {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = BackupHelper.toTxt(dao.getAllNow())
            launch(Dispatchers.Main) {
                pendingExport = data
                createDoc.launch(BackupHelper.fileName("txt"))
            }
        }
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Privacy / Offline")
            .setMessage(
                "NOTEZ tetap offline-first. Semua fitur inti tetap jalan tanpa internet.\n\n" +
                    "• INTERNET hanya dipakai saat kamu tap Load & cache pada gambar online.\n" +
                    "• Remote image tidak auto-load.\n" +
                    "• Gambar yang berhasil dimuat disimpan lokal untuk dibaca offline.\n" +
                    "• Markdown renderer memakai asset lokal dari APK.\n" +
                    "• Remote script/style/font/iframe tetap tidak dimuat.\n" +
                    "• Link eksternal dibuka lewat aplikasi/browser luar saat user tap."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmClearRemoteImageCache() {
        val cache = RemoteImageCache(this)
        AlertDialog.Builder(this)
            .setTitle("Hapus cache gambar online?")
            .setMessage(
                "Cache sekarang: ${cache.formattedSize()}\n\n" +
                    "Gambar online yang pernah di-load akan dihapus dari penyimpanan lokal. " +
                    "Catatan tetap aman; nanti gambar bisa di-load lagi kalau dibutuhkan."
            )
            .setPositiveButton("Hapus") { _, _ ->
                val result = cache.clear()
                toast("Cache gambar dihapus (${result.files} file)")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun versionName(): String {
        val fallback = getString(R.string.notez_version_name)
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
                ?.takeIf { it.isNotBlank() && it != "unknown" }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}