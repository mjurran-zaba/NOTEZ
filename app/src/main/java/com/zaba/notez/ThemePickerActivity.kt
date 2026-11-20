package com.zaba.notez

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ThemePickerActivity : AppCompatActivity() {

    private sealed class ThemePickerItem {
        data class Header(val title: String) : ThemePickerItem()
        data class Card(val option: ThemePref.ThemeOption) : ThemePickerItem()
    }

    private data class ThemeGroup(val title: String, val values: List<Int>)

    private val themeGroups = listOf(
        ThemeGroup(
            title = "DAY & PAPER (MODE TERANG)",
            values = listOf(
                ThemePref.MATCHA_CREAM_PAPER,
                ThemePref.PARCHMENT_AMBER,
                ThemePref.ROSE_PINE_DAWN,
                ThemePref.NORD_POLAR_MIST
            )
        ),
        ThemeGroup(
            title = "IMMERSIVE & GLASS",
            values = listOf(
                ThemePref.AURORA_BOREALIS,
                ThemePref.CYBER_OBSIDIAN,
                ThemePref.SUNSET_MIRAGE
            )
        ),
        ThemeGroup(
            title = "NOTEZ SIGNATURE",
            values = listOf(ThemePref.OLED, ThemePref.NOTEZ_YOU_DARK, ThemePref.COBALT2, ThemePref.NOTEZ_YOU_WARM)
        ),
        ThemeGroup(
            title = "GLOOMY SERIES",
            values = listOf(
                ThemePref.GLOOMY_SAKURA_NIGHT,
                ThemePref.GLOOMY_LAVENDER,
                ThemePref.GLOOME_DARK_SUNSET,
                ThemePref.RASPBERRY_NIGHT
            )
        ),
        ThemeGroup(
            title = "COZY EARTH",
            values = listOf(ThemePref.DARK_FOREST, ThemePref.FADE_CHOCO_MATCHA, ThemePref.KAWAII_CATPUCINN)
        ),
        ThemeGroup(
            title = "CODER NIGHT",
            values = listOf(ThemePref.GITHUB_DARK, ThemePref.TOKYO_NIGHT, ThemePref.BLUE_MOON_CHEESE)
        )
    )

    private var currentTheme: Int = ThemePref.GITHUB_DARK
    private lateinit var adapter: ThemeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        currentTheme = ThemePref.get(this)
        setTheme(ThemePref.styleOf(currentTheme))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_picker)

        findViewById<ImageButton>(R.id.theme_picker_back).setOnClickListener {
            finish()
        }

        val totalThemes = ThemePref.OPTIONS.size
        findViewById<TextView>(R.id.theme_count_badge).text = "$totalThemes tema"

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val items = mutableListOf<ThemePickerItem>()
        themeGroups.forEach { group ->
            items.add(ThemePickerItem.Header(group.title))
            group.values.forEach { themeVal ->
                items.add(ThemePickerItem.Card(ThemePref.optionOf(themeVal)))
            }
        }

        val recycler = findViewById<RecyclerView>(R.id.theme_recycler)
        val gridLayoutManager = GridLayoutManager(this, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (items[position]) {
                    is ThemePickerItem.Header -> 2
                    is ThemePickerItem.Card -> 1
                }
            }
        }
        recycler.layoutManager = gridLayoutManager

        adapter = ThemeAdapter(items, currentTheme) { option, view ->
            onThemeSelected(option, view)
        }
        recycler.adapter = adapter
    }

    private fun onThemeSelected(option: ThemePref.ThemeOption, view: View) {
        if (option.value == currentTheme) return

        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        view.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(70L)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100L).start()
            }
            .start()

        ThemePref.set(this, option.value)
        currentTheme = option.value

        // Recreate activity with new theme styling
        recreate()
    }

    private class ThemeAdapter(
        private val items: List<ThemePickerItem>,
        private val activeTheme: Int,
        private val onThemeClick: (ThemePref.ThemeOption, View) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_CARD = 1
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is ThemePickerItem.Header -> TYPE_HEADER
                is ThemePickerItem.Card -> TYPE_CARD
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                val v = inflater.inflate(R.layout.item_theme_category, parent, false)
                HeaderViewHolder(v)
            } else {
                val v = inflater.inflate(R.layout.item_theme_card, parent, false)
                CardViewHolder(v)
            }
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is ThemePickerItem.Header -> (holder as HeaderViewHolder).bind(item.title)
                is ThemePickerItem.Card -> (holder as CardViewHolder).bind(item.option, activeTheme, onThemeClick)
            }
        }

        class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val titleView = view.findViewById<TextView>(R.id.category_title)
            fun bind(title: String) {
                titleView.text = title
            }
        }

        class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val cardRoot = view.findViewById<MaterialCardView>(R.id.card_theme_root)
            private val thumbnail = view.findViewById<ThemeThumbnailView>(R.id.theme_thumbnail)
            private val nameView = view.findViewById<TextView>(R.id.theme_name)

            fun bind(
                option: ThemePref.ThemeOption,
                activeTheme: Int,
                onThemeClick: (ThemePref.ThemeOption, View) -> Unit
            ) {
                val isActive = option.value == activeTheme
                thumbnail.bind(option, isActive)
                nameView.text = option.name

                if (isActive) {
                    nameView.setTextColor(itemView.context.getColor(option.accentColorRes))
                    cardRoot.strokeColor = itemView.context.getColor(option.accentColorRes)
                    cardRoot.strokeWidth = (1.5f * itemView.resources.displayMetrics.density).toInt()
                } else {
                    nameView.setTextColor(UiStyle.resolveColor(
                        itemView.context,
                        com.google.android.material.R.attr.colorOnSurface,
                        android.graphics.Color.WHITE
                    ))
                    cardRoot.strokeColor = UiStyle.resolveColor(
                        itemView.context,
                        com.google.android.material.R.attr.colorOutline,
                        android.graphics.Color.DKGRAY
                    )
                    cardRoot.strokeWidth = (1f * itemView.resources.displayMetrics.density).toInt()
                }

                UiStyle.applyGlassCard(cardRoot, alpha = 80, radiusDp = 16f, seed = option.value)

                cardRoot.setOnClickListener {
                    onThemeClick(option, cardRoot)
                }
            }
        }
    }
}
