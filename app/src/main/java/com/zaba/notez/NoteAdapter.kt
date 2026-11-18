package com.zaba.notez

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter daftar catatan Home untuk mode List dan Grid.
 *
 * Menggunakan kartu berestetika lempengan kaca gelap (slate glass) dengan
 * stroke outline yang tenang, tipografi berjenjang, dan bebas dari tombol
 * sampah inline demi kenyamanan sentuhan jempol pengguna.
 */
class NoteAdapter(
    private val onOpen: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.Holder>() {

    private var items: List<Note> = emptyList()
    private val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    var mode: NoteDisplayMode = NoteDisplayMode.LIST
        private set

    fun submit(list: List<Note>) {
        items = list
        notifyDataSetChanged()
    }

    /** Ganti presentasi tanpa menyentuh dataset. */
    fun setMode(newMode: NoteDisplayMode) {
        if (mode == newMode) return
        mode = newMode
        notifyDataSetChanged()
    }

    fun noteAt(position: Int): Note? = items.getOrNull(position)

    fun indexOfNote(noteId: Long): Int = items.indexOfFirst { it.id == noteId }

    override fun getItemViewType(position: Int): Int =
        if (mode == NoteDisplayMode.GRID) VIEW_TYPE_GRID else VIEW_TYPE_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = if (viewType == VIEW_TYPE_GRID) {
            R.layout.item_note_grid
        } else {
            R.layout.item_note
        }
        return Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val n = items[position]
        val context = h.itemView.context
        h.card.setCardBackgroundColor(ColorUtils.setAlphaComponent(UiStyle.surface(context), 224))
        h.card.setStrokeColor(ColorUtils.setAlphaComponent(UiStyle.outline(context), 45))
        h.card.setRadius(UiStyle.dp(context, 16f).toFloat())
        h.card.setCardElevation(UiStyle.dp(context, 1.5f).toFloat())

        h.title.text = n.title.ifBlank { "(tanpa judul)" }

        val previewText = n.content.lines().filter { it.isNotBlank() }.take(2).joinToString(" ").take(120)
        if (previewText.isBlank()) {
            h.preview.visibility = View.GONE
        } else {
            h.preview.visibility = View.VISIBLE
            h.preview.text = previewText
        }

        h.meta.text = "${fmt.format(Date(n.updatedAt))} • ${n.content.length} karakter"
        h.itemView.setOnClickListener { onOpen(n) }
        h.itemView.setOnLongClickListener {
            onDelete(n)
            true
        }
    }

    override fun getItemCount() = items.size

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = v.findViewById(R.id.note_card)
        val title: TextView = v.findViewById(R.id.item_title)
        val preview: TextView = v.findViewById(R.id.item_preview)
        val meta: TextView = v.findViewById(R.id.item_meta)
    }

    private companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
    }
}
