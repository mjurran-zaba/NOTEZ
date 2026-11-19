package com.zaba.notez

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter daftar catatan Home untuk mode List dan Grid dengan dukungan
 * Contextual Selection Mode dan indikator Semat (Pin).
 */
class NoteAdapter(
    private val onOpen: (Note) -> Unit,
    private val onDelete: (Note) -> Unit,
    private val onSelectionChanged: (selectedCount: Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.Holder>() {

    private var items: List<Note> = emptyList()
    private val selectedIds = mutableSetOf<Long>()
    private val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    var mode: NoteDisplayMode = NoteDisplayMode.LIST
        private set

    val isSelectionMode: Boolean
        get() = selectedIds.isNotEmpty()

    fun getSelectedNotes(): List<Note> = items.filter { selectedIds.contains(it.id) }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(items.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun submit(list: List<Note>) {
        items = list
        selectedIds.retainAll(items.map { it.id }.toSet())
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

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
        val isSelected = selectedIds.contains(n.id)

        UiStyle.applyGlassCard(h.card, alpha = 185, radiusDp = 16f)
        h.card.cardElevation = 0f

        if (isSelected) {
            h.card.strokeColor = UiStyle.primary(context)
            h.card.strokeWidth = UiStyle.dp(context, 1.5f)
        }

        h.title.text = n.title.ifBlank { "(tanpa judul)" }

        val previewText = n.content.lines().filter { it.isNotBlank() }.take(2).joinToString(" ").take(120)
        if (previewText.isBlank()) {
            h.preview.visibility = View.GONE
        } else {
            h.preview.visibility = View.VISIBLE
            h.preview.text = previewText
        }

        h.meta.text = "${fmt.format(Date(n.updatedAt))} • ${n.content.length} karakter"

        if (isSelectionMode) {
            h.pin.visibility = View.GONE
            h.check.visibility = View.VISIBLE
            if (isSelected) {
                h.check.setImageResource(R.drawable.ic_lucide_check_circle_2)
                h.check.imageTintList = ColorStateList.valueOf(UiStyle.primary(context))
            } else {
                h.check.setImageResource(R.drawable.ic_lucide_circle)
                h.check.imageTintList = ColorStateList.valueOf(UiStyle.outline(context))
            }
        } else {
            h.check.visibility = View.GONE
            h.pin.visibility = if (n.isPinned) View.VISIBLE else View.GONE
            h.pin.imageTintList = ColorStateList.valueOf(UiStyle.primary(context))
        }

        h.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(n.id)
            } else {
                onOpen(n)
            }
        }

        h.itemView.setOnLongClickListener {
            h.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            toggleSelection(n.id)
            true
        }
    }

    override fun getItemCount() = items.size

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val card: MaterialCardView = (v as? MaterialCardView) ?: v.findViewById(R.id.note_card)
        val title: TextView = v.findViewById(R.id.item_title)
        val preview: TextView = v.findViewById(R.id.item_preview)
        val meta: TextView = v.findViewById(R.id.item_meta)
        val pin: ImageView = v.findViewById(R.id.item_pin)
        val check: ImageView = v.findViewById(R.id.item_select_check)
    }

    private companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
    }
}
