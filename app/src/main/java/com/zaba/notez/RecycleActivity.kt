package com.zaba.notez

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.zaba.notez.diagnostics.Breadcrumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecycleActivity : AppCompatActivity() {

    private lateinit var dao: NoteDao
    private lateinit var list: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var btnClearAll: TextView
    private val adapter = RecycleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemePref.styleOf(ThemePref.get(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycle)

        dao = AppDatabase.get(this).noteDao()

        findViewById<ImageButton>(R.id.btn_recycle_back).setOnClickListener { finish() }
        list = findViewById(R.id.recycle_list)
        emptyView = findViewById(R.id.recycle_empty)
        btnClearAll = findViewById(R.id.btn_recycle_clear_all)

        val infoCard = findViewById<MaterialCardView>(R.id.recycle_info_card)
        UiStyle.applyGlassCard(infoCard, alpha = 160, radiusDp = 14f)
        val infoText = findViewById<TextView>(R.id.recycle_info_text)
        if (ThemePref.isLightTheme(ThemePref.get(this))) {
            infoText?.setTextColor(Color.parseColor("#2E3A31"))
        }

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        adapter.onRestore = { note ->
            lifecycleScope.launch(Dispatchers.IO) {
                dao.restoreFromRecycle(listOf(note.id))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RecycleActivity, "\"${note.title.ifBlank { "Catatan" }}\" dipulihkan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        adapter.onDeletePermanently = { note ->
            AlertDialog.Builder(this)
                .setTitle("Hapus permanen?")
                .setMessage("Catatan \"${note.title.ifBlank { "Catatan" }}\" akan dihapus selamanya dan tidak dapat dikembalikan.")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.deletePermanently(listOf(note.id))
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        btnClearAll.setOnClickListener {
            if (adapter.itemCount == 0) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Kosongkan Recycle?")
                .setMessage("Seluruh catatan di Recycle akan dihapus permanen.")
                .setPositiveButton("Kosongkan") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.clearRecycle()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        lifecycleScope.launch {
            dao.observeRecycle().collect { notes ->
                adapter.submit(notes)
                emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                btnClearAll.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private class RecycleAdapter : RecyclerView.Adapter<RecycleAdapter.Holder>() {
        private var items = listOf<Note>()
        private val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        var onRestore: ((Note) -> Unit)? = null
        var onDeletePermanently: ((Note) -> Unit)? = null

        fun submit(list: List<Note>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recycle_note, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(h: Holder, position: Int) {
            val note = items[position]
            UiStyle.applyGlassCard(h.card, alpha = 160, radiusDp = 16f, seed = note.id.hashCode())
            h.card.cardElevation = 0f

            val isLight = ThemePref.isLightTheme(ThemePref.get(h.itemView.context))
            if (isLight) {
                h.title.setTextColor(Color.parseColor("#1C241E"))
                h.date.setTextColor(Color.parseColor("#4A5568"))
            } else {
                h.title.setTextColor(UiStyle.resolveColor(h.itemView.context, com.google.android.material.R.attr.colorOnSurface, Color.WHITE))
                h.date.setTextColor(UiStyle.resolveColor(h.itemView.context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.LTGRAY))
            }

            h.title.text = note.title.ifBlank { "(tanpa judul)" }
            val deletedDate = note.deletedAt?.let { Date(it) } ?: Date(note.updatedAt)
            h.date.text = "Dihapus: ${fmt.format(deletedDate)}"

            h.btnRestore.setOnClickListener { onRestore?.invoke(note) }
            h.btnDelete.setOnClickListener { onDeletePermanently?.invoke(note) }
        }

        override fun getItemCount(): Int = items.size

        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val card: MaterialCardView = (v as? MaterialCardView) ?: v.findViewById(R.id.note_card)
            val title: TextView = v.findViewById(R.id.recycle_item_title)
            val date: TextView = v.findViewById(R.id.recycle_item_date)
            val btnRestore: ImageButton = v.findViewById(R.id.btn_recycle_restore)
            val btnDelete: ImageButton = v.findViewById(R.id.btn_recycle_delete)
        }
    }
}
