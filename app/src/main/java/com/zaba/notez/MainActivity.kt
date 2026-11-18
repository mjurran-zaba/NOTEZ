package com.zaba.notez

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.zaba.notez.diagnostics.Breadcrumb
import com.zaba.notez.diagnostics.installBreadcrumbLogcat
import com.zaba.notez.music.MusicMiniPlayerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val GRID_SPACING_DP = 8
private const val SEARCH_SLIDE_MS = 180L

class MainActivity : AppCompatActivity() {

    private lateinit var dao: NoteDao
    private lateinit var adapter: NoteAdapter
    private lateinit var drawer: DrawerLayout
    private var musicMiniPlayer: MusicMiniPlayerHelper? = null

    private var collectJob: Job? = null
    private var notePresenceJob: Job? = null
    private var query = ""
    private var pendingDrawerAction: (() -> Unit)? = null
    private var searchOpen = false
    private var speedDialOpen = false
    private var totalNotes: Int? = null
    private var displayMode = NoteDisplayMode.LIST
    private var spacingDecoration: GridSpacingItemDecoration? = null
    private var scrollRestoreToken = 0
    private lateinit var listView: RecyclerView
    private var appliedTheme = ThemePref.GITHUB_DARK

    private lateinit var fab: FloatingActionButton
    private lateinit var fabScrim: View
    private lateinit var fabSpeedDial: View
    private lateinit var btnSpeedNewNote: MaterialCardView
    private lateinit var btnSpeedOpenFile: MaterialCardView
    private lateinit var btnDrawer: ImageButton
    private lateinit var searchToggle: ImageButton
    private lateinit var searchPill: MaterialCardView
    private lateinit var searchInput: EditText
    private lateinit var homeHeaderInfo: View
    private lateinit var homeSubtitle: TextView

    private var startupSplashAnimator: AnimatorSet? = null
    private var startupSplashStatusBarColor: Int? = null
    private var startupSplashNavigationBarColor: Int? = null
    private var startupSplashSystemUiVisibility: Int? = null

    private val openExternalFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        importExternalFile(uri)
    }

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("NOTEZ_CRASH", "FATAL EXCEPTION in thread ${thread.name}", throwable)
            Breadcrumb.add("CRASH", "${throwable.javaClass.simpleName}: ${throwable.message}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
        appliedTheme = ThemePref.get(this)
        setTheme(ThemePref.styleOf(appliedTheme))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        installBreadcrumbLogcat()
        Breadcrumb.add(
            "HOME",
            "onCreate theme=$appliedTheme rotation=${savedInstanceState != null}"
        )
        applyHomeSystemBars()
        playStartupSplash(savedInstanceState)
        dao = AppDatabase.get(this).noteDao()

        drawer = findViewById(R.id.drawer)
        UiStyle.applyDrawerGlass(findViewById(R.id.drawer_panel))
        setupDrawer()

        initViews()
        setupTopBar()
        setupSpeedDial()
        displayMode = HomeDisplayPref.get(this)
        setupRecyclerView()
        applyDisplayMode(displayMode, restoreScroll = false)
        setupMiniPlayer()
        applyHorseThemeAccent()

        observeNotePresence()
    }

    private fun initViews() {
        btnDrawer = findViewById(R.id.btn_drawer)
        searchToggle = findViewById(R.id.search_toggle)
        searchPill = findViewById(R.id.search_pill)
        searchInput = findViewById(R.id.search_input)
        homeHeaderInfo = findViewById(R.id.home_header_info)
        homeSubtitle = findViewById(R.id.home_subtitle)

        UiStyle.applyGlassCard(searchPill, alpha = 200, radiusDp = 20f)

        fab = findViewById(R.id.fab)
        fabScrim = findViewById(R.id.fab_scrim)
        fabSpeedDial = findViewById(R.id.fab_speed_dial)
        btnSpeedNewNote = findViewById(R.id.btn_speed_new_note)
        btnSpeedOpenFile = findViewById(R.id.btn_speed_open_file)

        UiStyle.applyGlassCard(btnSpeedNewNote, alpha = 224, radiusDp = 22f)
        UiStyle.applyGlassCard(btnSpeedOpenFile, alpha = 224, radiusDp = 22f)

        findViewById<View>(R.id.empty_about_link)?.setOnClickListener {
            startActivity(Intent(this, AboutNotezActivity::class.java))
        }
    }

    private fun setupTopBar() {
        btnDrawer.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            it.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60L).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).setInterpolator(OvershootInterpolator()).start()
            }.start()
            if (drawer.isDrawerVisible(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            } else {
                drawer.openDrawer(GravityCompat.START)
            }
        }

        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                btnDrawer.rotation = slideOffset * 180f
            }

            override fun onDrawerClosed(drawerView: View) {
                Breadcrumb.add("HOME", "DRAWER_CLOSED")
                btnDrawer.rotation = 0f
                val action = pendingDrawerAction ?: return
                pendingDrawerAction = null
                action()
            }
        })

        searchToggle.setOnClickListener {
            if (searchOpen) {
                Breadcrumb.add("HOME", "TAP_SEARCH_CLOSE")
                closeSearch()
            } else {
                Breadcrumb.add("HOME", "TAP_SEARCH_OPEN")
                openSearch()
            }
        }

        searchInput.doAfterTextChanged {
            query = it?.toString().orEmpty()
            Breadcrumb.add("HOME", "SEARCH_QUERY chars=${query.length} q='$query'")
            observe()
        }
    }

    private fun setupSpeedDial() {
        fab.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            toggleSpeedDial()
        }
        fabScrim.setOnClickListener { closeSpeedDial() }

        btnSpeedNewNote.setOnClickListener {
            closeSpeedDial()
            Breadcrumb.add("HOME", "TAP_SPEED_NEW_NOTE")
            lifecycleScope.launch(Dispatchers.IO) {
                val id = dao.upsert(Note(title = "Catatan baru"))
                Breadcrumb.add("IO", "NEW_NOTE_CREATED id=$id")
                launch(Dispatchers.Main) { openEditor(id, isNew = true) }
            }
        }

        btnSpeedOpenFile.setOnClickListener {
            closeSpeedDial()
            Breadcrumb.add("HOME", "TAP_SPEED_OPEN_FILE")
            openExternalFileLauncher.launch(arrayOf("text/*", "application/octet-stream"))
        }
    }

    private fun toggleSpeedDial() {
        if (speedDialOpen) closeSpeedDial() else openSpeedDial()
    }

    private fun openSpeedDial() {
        speedDialOpen = true
        fab.animate().rotation(45f).setDuration(180L).setInterpolator(DecelerateInterpolator()).start()
        fabScrim.visibility = View.VISIBLE
        fabScrim.alpha = 0f
        fabScrim.animate().alpha(0.38f).setDuration(180L).start()

        fabSpeedDial.visibility = View.VISIBLE
        animateSpeedDialItem(btnSpeedNewNote, delayMs = 0L)
        animateSpeedDialItem(btnSpeedOpenFile, delayMs = 35L)
    }

    private fun closeSpeedDial() {
        if (!speedDialOpen) return
        speedDialOpen = false
        fab.animate().rotation(0f).setDuration(180L).setInterpolator(DecelerateInterpolator()).start()
        fabScrim.animate().alpha(0f).setDuration(180L).withEndAction { fabScrim.visibility = View.GONE }.start()

        fabSpeedDial.animate()
            .alpha(0f)
            .translationY(UiStyle.dp(this, 12f).toFloat())
            .setDuration(140L)
            .withEndAction {
                fabSpeedDial.visibility = View.GONE
                fabSpeedDial.alpha = 1f
                fabSpeedDial.translationY = 0f
            }.start()
    }

    private fun animateSpeedDialItem(view: View, delayMs: Long) {
        view.alpha = 0f
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        view.translationY = UiStyle.dp(this, 18f).toFloat()
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(180L)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onOpen = { note ->
                Breadcrumb.add("HOME", "TAP_NOTE id=${note.id} title='${note.title}'")
                openEditor(note.id)
            },
            onDelete = { note ->
                Breadcrumb.add("HOME", "TAP_DELETE id=${note.id} title='${note.title}'")
                deleteWithUndo(note)
            }
        )
        displayMode = HomeDisplayPref.get(this)
        listView = findViewById(R.id.list)
        applyDisplayMode(displayMode, restoreScroll = false)
        listView.adapter = adapter
        setupDisplayModeDrawer()
        setupSwipeToDelete()

        listView.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (displayMode == NoteDisplayMode.GRID && right - left != oldRight - oldLeft) {
                updateGridSpanCount()
            }
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val deletePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E02424")
            }
            private val trashIcon: Drawable? = AppCompatResources.getDrawable(this@MainActivity, R.drawable.ic_lucide_trash)?.apply {
                setTint(Color.WHITE)
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (displayMode == NoteDisplayMode.LIST) super.getSwipeDirs(recyclerView, viewHolder) else 0
            }

            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = adapter.noteAt(position) ?: return
                deleteWithUndo(note)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = viewHolder.itemView
                    val radius = UiStyle.dp(this@MainActivity, 16f).toFloat()
                    val margin = UiStyle.dp(this@MainActivity, 6f).toFloat()
                    val background = RectF(
                        itemView.right + dX - margin,
                        itemView.top + margin,
                        itemView.right - margin,
                        itemView.bottom - margin
                    )
                    c.drawRoundRect(background, radius, radius, deletePaint)

                    val icon = trashIcon
                    if (icon != null) {
                        val iconSize = UiStyle.dp(this@MainActivity, 22f)
                        val iconMargin = UiStyle.dp(this@MainActivity, 20f)
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        val iconLeft = itemView.right - iconMargin - iconSize
                        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        swipeHelper.attachToRecyclerView(listView)
    }

    private fun setupMiniPlayer() {
        val miniPlayerView = findViewById<View>(R.id.home_mini_player) ?: return
        runCatching {
            musicMiniPlayer = MusicMiniPlayerHelper(
                activity = this,
                root = miniPlayerView,
                onOpenDrawer = null,
                onVisibilityChanged = { isVisible ->
                    val offset = if (isVisible) -UiStyle.dp(this, 68f).toFloat() else 0f
                    fab.animate().translationY(offset).setDuration(180L).start()
                    fabSpeedDial.animate().translationY(offset).setDuration(180L).start()
                }
            )
        }.onFailure { err ->
            Breadcrumb.add("HOME", "MINI_PLAYER_INIT_FAIL: ${err.message}")
        }
    }

    private fun applyHorseThemeAccent() {
        val accentColor = getColor(ThemePref.optionOf(appliedTheme).accentColorRes)
        findViewById<ImageView>(R.id.splash_accent)?.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.splash_glow)?.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.empty_notez_accent)?.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.empty_notez_glow)?.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.drawer_footer_horse)?.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
    }

    private fun openSearch() {
        searchOpen = true
        homeHeaderInfo.visibility = View.GONE
        searchPill.visibility = View.VISIBLE
        searchPill.alpha = 0f
        searchPill.animate().alpha(1f).setDuration(SEARCH_SLIDE_MS).start()

        searchToggle.setImageResource(R.drawable.ic_lucide_x)
        searchToggle.contentDescription = getString(R.string.cd_close_search)
        searchInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        searchOpen = false
        searchInput.setText("")
        searchPill.visibility = View.GONE
        homeHeaderInfo.visibility = View.VISIBLE
        homeHeaderInfo.alpha = 0f
        homeHeaderInfo.animate().alpha(1f).setDuration(SEARCH_SLIDE_MS).start()

        searchToggle.setImageResource(R.drawable.ic_lucide_search)
        searchToggle.contentDescription = getString(R.string.cd_open_search)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }
        searchToggle.contentDescription = getString(R.string.cd_open_search)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    private fun observeNotePresence() {
        notePresenceJob?.cancel()
        notePresenceJob = lifecycleScope.launch {
            dao.observeAll().collectLatest { notes ->
                totalNotes = notes.size
                homeSubtitle.text = "${notes.size} catatan"
                renderSearchChrome(searchToggle, searchInput)
            }
        }
    }

    private fun renderSearchChrome(toggle: ImageButton, input: EditText) {
        val show = HomeSearchPolicy.toolbarVisible(totalNotes)
        if (!show && searchOpen) closeSearch()
        findViewById<View>(R.id.home_toolbar).visibility = if (show) View.VISIBLE else View.GONE
        toggle.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun importExternalFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val fileName = queryDisplayName(uri) ?: "Catatan Impor"
            val title = fileName.substringBeforeLast('.')
            val content = runCatching {
                contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
            }.getOrNull().orEmpty()

            val newId = dao.upsert(Note(title = title, content = content))
            withContext(Dispatchers.Main) {
                openEditor(newId)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }

    private fun setupDisplayModeDrawer() {
        val group = findViewById<RadioGroup>(R.id.drawer_display_group) ?: return
        group.check(
            if (displayMode == NoteDisplayMode.GRID) R.id.drawer_display_grid
            else R.id.drawer_display_list
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            val selected = if (checkedId == R.id.drawer_display_grid) {
                NoteDisplayMode.GRID
            } else {
                NoteDisplayMode.LIST
            }
            if (selected == displayMode) return@setOnCheckedChangeListener
            Breadcrumb.add("HOME", "DISPLAY_MODE_CHANGE $displayMode -> $selected")
            HomeDisplayPref.set(this, selected)
            applyDisplayMode(selected, restoreScroll = true)
        }
    }

    private fun applyDisplayMode(mode: NoteDisplayMode, restoreScroll: Boolean) {
        val anchor = if (restoreScroll) captureScrollAnchor() else null
        displayMode = mode
        adapter.setMode(mode)
        spacingDecoration?.let { listView.removeItemDecoration(it) }
        spacingDecoration = null
        if (mode == NoteDisplayMode.GRID) {
            listView.layoutManager = GridLayoutManager(this, gridSpanCount())
            val decoration = GridSpacingItemDecoration(UiStyle.dp(this, GRID_SPACING_DP.toFloat()))
            spacingDecoration = decoration
            listView.addItemDecoration(decoration)
        } else {
            listView.layoutManager = LinearLayoutManager(this)
        }
        if (anchor != null) {
            restoreScrollAnchor(anchor, ++scrollRestoreToken)
        }
    }

    private fun gridSpanCount(): Int {
        val measured = listView.width - listView.paddingStart - listView.paddingEnd
        val available = if (measured > 0) {
            measured
        } else {
            resources.displayMetrics.widthPixels - listView.paddingStart - listView.paddingEnd
        }
        return HomeDisplayPolicy.spanCount(
            availableWidthPx = available,
            minCardWidthPx = UiStyle.dp(this, HomeDisplayPolicy.MIN_CARD_WIDTH_DP.toFloat()),
            spacingPx = UiStyle.dp(this, GRID_SPACING_DP.toFloat())
        )
    }

    private fun updateGridSpanCount() {
        val manager = listView.layoutManager as? GridLayoutManager ?: return
        val span = gridSpanCount()
        if (span != manager.spanCount) manager.spanCount = span
    }

    private data class ScrollAnchor(val noteId: Long, val position: Int, val offset: Int)

    private fun captureScrollAnchor(): ScrollAnchor? {
        val manager = listView.layoutManager as? LinearLayoutManager ?: return null
        val position = manager.findFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return null
        val note = adapter.noteAt(position) ?: return null
        val view = manager.findViewByPosition(position) ?: return null
        return ScrollAnchor(note.id, position, view.top - listView.paddingTop)
    }

    private fun restoreScrollAnchor(anchor: ScrollAnchor, token: Int) {
        listView.post {
            if (token != scrollRestoreToken) return@post
            val manager = listView.layoutManager as? LinearLayoutManager ?: return@post
            val target = HomeDisplayPolicy.anchorIndex(
                exactIndex = adapter.indexOfNote(anchor.noteId),
                fallbackPosition = anchor.position,
                itemCount = adapter.itemCount
            )
            if (target < 0) return@post
            manager.scrollToPositionWithOffset(target, anchor.offset)
        }
    }

    private fun setupDrawer() {
        val versionName = runCatching { getString(R.string.notez_version_name) }.getOrDefault("0.1.7")
        findViewById<TextView>(R.id.drawer_footer_version)?.text = "NOTEZ v$versionName"

        findViewById<View>(R.id.menu_markdown_guide)?.setOnClickListener {
            Breadcrumb.add("HOME", "TAP_MENU_MARKDOWN_GUIDE")
            closeDrawerThen { startActivity(Intent(this, MarkdownGuideActivity::class.java)) }
        }
        findViewById<View>(R.id.drawer_settings)?.setOnClickListener {
            Breadcrumb.add("HOME", "TAP_MENU_SETTINGS")
            closeDrawerThen { startActivity(Intent(this, SettingsActivity::class.java)) }
        }
        findViewById<View>(R.id.drawer_diagnostics)?.setOnClickListener {
            Breadcrumb.add("HOME", "TAP_MENU_DIAGNOSTICS")
            closeDrawerThen { startActivity(Intent(this, com.zaba.notez.diagnostics.DiagnosticsActivity::class.java)) }
        }
    }

    private fun closeDrawerThen(action: () -> Unit) {
        if (drawer.isDrawerVisible(GravityCompat.START)) {
            pendingDrawerAction = action
            drawer.closeDrawer(GravityCompat.START)
        } else {
            action()
        }
    }

    private fun deleteWithUndo(note: Note) {
        lifecycleScope.launch(Dispatchers.IO) {
            dao.delete(note)
            Breadcrumb.add("IO", "DB_DELETE id=${note.id}")
            launch(Dispatchers.Main) {
                Snackbar.make(listView, "Catatan dihapus", Snackbar.LENGTH_LONG)
                    .setAction("Batal") {
                        lifecycleScope.launch(Dispatchers.IO) {
                            dao.upsert(note)
                            Breadcrumb.add("IO", "DB_RESTORE id=${note.id}")
                        }
                    }
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        }
    }

    private fun observe() {
        collectJob?.cancel()
        val flow = if (query.isBlank()) dao.observeAll() else dao.search(query)
        collectJob = lifecycleScope.launch {
            flow.collectLatest {
                adapter.submit(it)
                val empty = it.isEmpty()
                val searching = query.isNotBlank()
                findViewById<RecyclerView>(R.id.list).visibility =
                    if (empty) View.GONE else View.VISIBLE
                findViewById<View>(R.id.empty_welcome).visibility =
                    if (empty && !searching) View.VISIBLE else View.GONE
                findViewById<TextView>(R.id.empty_search).visibility =
                    if (empty && searching) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openEditor(id: Long, isNew: Boolean = false) {
        Breadcrumb.add("HOME", "OPEN_EDITOR note=$id isNew=$isNew")
        startActivity(
            Intent(this, EditorActivity::class.java)
                .putExtra("note_id", id)
                .putExtra("is_new", isNew)
                .putExtra("open_started_at", android.os.SystemClock.uptimeMillis())
        )
    }

    override fun onBackPressed() {
        Breadcrumb.add(
            "HOME",
            "BACK drawer=${drawer.isDrawerVisible(GravityCompat.START)} search=$searchOpen speedDial=$speedDialOpen"
        )
        if (drawer.isDrawerVisible(GravityCompat.START)) {
            pendingDrawerAction = null
            drawer.closeDrawer(GravityCompat.START)
        } else if (speedDialOpen) {
            closeSpeedDial()
        } else if (searchOpen) {
            closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        val currentTheme = ThemePref.get(this)
        Breadcrumb.add("HOME", "onResume theme=$currentTheme applied=$appliedTheme")
        if (currentTheme != appliedTheme) {
            Breadcrumb.add("HOME", "RECREATE_FOR_THEME")
            recreate()
            return
        }
        observe()
        musicMiniPlayer?.refresh()
        lifecycleScope.launch(Dispatchers.IO) {
            if (BackupHelper.autoBackupIfDue(this@MainActivity, dao)) {
                Breadcrumb.add("IO", "AUTO_BACKUP_SAVED")
                launch(Dispatchers.Main) { toast("Backup otomatis tersimpan") }
            }
        }
    }

    override fun onDestroy() {
        startupSplashAnimator?.cancel()
        startupSplashAnimator = null
        notePresenceJob?.cancel()
        notePresenceJob = null
        musicMiniPlayer?.destroy()
        musicMiniPlayer = null
        super.onDestroy()
    }

    private fun playStartupSplash(savedInstanceState: Bundle?) {
        val overlay = findViewById<View>(R.id.splash_overlay)
        if (savedInstanceState != null || animatorDurationScale() == 0f) {
            overlay.visibility = View.GONE
            return
        }

        prepareStartupSplashSystemBars()
        overlay.visibility = View.VISIBLE
        overlay.alpha = 1f
        overlay.isClickable = true
        overlay.bringToFront()

        val glow = findViewById<ImageView>(R.id.splash_glow)
        val horse = findViewById<ImageView>(R.id.splash_horse)
        val accent = findViewById<ImageView>(R.id.splash_accent)

        overlay.post {
            if (isFinishing || isDestroyed) {
                hideStartupSplash(overlay)
                return@post
            }

            glow.alpha = 0f
            glow.scaleX = 1.02f
            glow.scaleY = 1.02f
            horse.alpha = 0f
            horse.scaleX = 0.96f
            horse.scaleY = 0.96f
            horse.translationY = 10f
            accent.alpha = 0f
            accent.scaleX = 0.72f
            accent.scaleY = 0.72f
            accent.translationY = 14f
            accent.pivotX = accent.width * 0.34f
            accent.pivotY = accent.height * 0.78f

            startupSplashAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(horse, View.ALPHA, 0f, 1f).timed(260L, 80L),
                    ObjectAnimator.ofFloat(horse, View.SCALE_X, 0.96f, 1f).timed(380L, 80L),
                    ObjectAnimator.ofFloat(horse, View.SCALE_Y, 0.96f, 1f).timed(380L, 80L),
                    ObjectAnimator.ofFloat(horse, View.TRANSLATION_Y, 10f, 0f).timed(380L, 80L),
                    ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 0.82f, 0.46f).timed(680L, 210L),
                    ObjectAnimator.ofFloat(glow, View.SCALE_X, 1.02f, 1.12f).timed(680L, 210L),
                    ObjectAnimator.ofFloat(glow, View.SCALE_Y, 1.02f, 1.12f).timed(680L, 210L),
                    ObjectAnimator.ofFloat(accent, View.ALPHA, 0f, 1f, 0.9f).timed(430L, 420L),
                    ObjectAnimator.ofFloat(accent, View.SCALE_X, 0.72f, 1f).timed(430L, 420L),
                    ObjectAnimator.ofFloat(accent, View.SCALE_Y, 0.72f, 1f).timed(430L, 420L),
                    ObjectAnimator.ofFloat(accent, View.TRANSLATION_Y, 14f, 0f).timed(430L, 420L),
                    ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f).timed(180L, 1040L)
                )
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = hideStartupSplash(overlay)
                    override fun onAnimationCancel(animation: Animator) = hideStartupSplash(overlay)
                })
                start()
            }
        }
    }

    private fun ObjectAnimator.timed(durationMs: Long, delayMs: Long): ObjectAnimator = apply {
        duration = durationMs
        startDelay = delayMs
        interpolator = DecelerateInterpolator()
    }

    private fun applyHomeSystemBars() {
        val background = getColor(ThemePref.optionOf(appliedTheme).backgroundColorRes)
        window.statusBarColor = background
        window.navigationBarColor = background
        val lightBars = isLightColor(background)
        window.decorView.systemUiVisibility = if (lightBars) {
            window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
    }

    private fun isLightColor(color: Int): Boolean {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return luminance > 0.62
    }

    private fun prepareStartupSplashSystemBars() {
        if (startupSplashStatusBarColor == null) {
            startupSplashStatusBarColor = window.statusBarColor
            startupSplashNavigationBarColor = window.navigationBarColor
            startupSplashSystemUiVisibility = window.decorView.systemUiVisibility
        }
        window.statusBarColor = getColor(R.color.github_bg)
        window.navigationBarColor = getColor(R.color.github_bg)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
    }

    private fun restoreStartupSplashSystemBars() {
        startupSplashStatusBarColor?.let { window.statusBarColor = it }
        startupSplashNavigationBarColor?.let { window.navigationBarColor = it }
        startupSplashSystemUiVisibility?.let { window.decorView.systemUiVisibility = it }
        startupSplashStatusBarColor = null
        startupSplashNavigationBarColor = null
        startupSplashSystemUiVisibility = null
    }

    private fun hideStartupSplash(overlay: View) {
        overlay.visibility = View.GONE
        overlay.alpha = 0f
        overlay.isClickable = false
        startupSplashAnimator = null
        restoreStartupSplashSystemBars()
    }

    private fun animatorDurationScale(): Float = try {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    } catch (_: Exception) {
        1f
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
