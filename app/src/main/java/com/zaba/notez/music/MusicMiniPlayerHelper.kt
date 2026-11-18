package com.zaba.notez.music

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.material.card.MaterialCardView
import com.google.common.util.concurrent.ListenableFuture
import com.zaba.notez.R
import com.zaba.notez.UiStyle
import kotlin.math.abs

/**
 * Controller penghubung mini player mengambang (ArchiveTune design) dengan
 * [MusicPlaybackService] yang sedang aktif di latar belakang.
 */
class MusicMiniPlayerHelper(
    private val activity: AppCompatActivity,
    private val root: View,
    private val onOpenDrawer: (() -> Unit)? = null,
    private val onVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    private val handler = Handler(Looper.getMainLooper())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var playerListener: Player.Listener? = null

    private val card: MaterialCardView? = (root as? MaterialCardView)
        ?: root.findViewById(R.id.mini_player_card)
        ?: root.findViewById(R.id.home_mini_player)
        ?: root.findViewById(R.id.editor_mini_player)

    private val title: TextView? = root.findViewById(R.id.mini_player_title)
    private val artist: TextView? = root.findViewById(R.id.mini_player_artist)
    private val progress: ProgressBar? = root.findViewById(R.id.mini_player_progress)
    private val playPause: ImageButton? = root.findViewById(R.id.mini_player_play_pause)
    private val prev: ImageButton? = root.findViewById(R.id.mini_player_prev)
    private val next: ImageButton? = root.findViewById(R.id.mini_player_next)
    private val infoContainer: View? = root.findViewById(R.id.mini_player_info)

    private var isPlayerVisible = false
    private var startTouchX = 0f
    private var startTouchY = 0f

    private val progressTick = object : Runnable {
        override fun run() {
            updateProgress()
            if (controller?.isPlaying == true) {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    init {
        card?.let { c ->
            c.setCardBackgroundColor(ColorUtils.setAlphaComponent(UiStyle.surface(activity), 224))
            c.setStrokeColor(ColorUtils.setAlphaComponent(UiStyle.outline(activity), 45))
            c.strokeWidth = UiStyle.dp(activity, 1f)
            c.setRadius(UiStyle.dp(activity, 28f).toFloat())
            c.setCardElevation(UiStyle.dp(activity, 4f).toFloat())
        }

        playPause?.setOnClickListener { togglePlayPause() }
        prev?.setOnClickListener { playPrevious() }
        next?.setOnClickListener { playNext() }
        infoContainer?.setOnClickListener { onOpenDrawer?.invoke() }

        setupSwipeGesture()
        connectController()
    }

    fun refresh() {
        updateUi()
    }

    fun destroy() {
        handler.removeCallbacks(progressTick)
        playerListener?.let { controller?.removeListener(it) }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun connectController() {
        runCatching {
            val token = SessionToken(activity, ComponentName(activity, MusicPlaybackService::class.java))
            val future = MediaController.Builder(activity, token).buildAsync()
            controllerFuture = future
            future.addListener({
                controller = runCatching { future.get() }.getOrNull()
                attachPlayerListener()
                updateUi()
            }, ContextCompat.getMainExecutor(activity))
        }
    }

    private fun attachPlayerListener() {
        val c = controller ?: return
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                activity.runOnUiThread {
                    updateUi()
                    if (isPlaying) {
                        handler.removeCallbacks(progressTick)
                        handler.post(progressTick)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                activity.runOnUiThread { updateUi() }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                activity.runOnUiThread { updateUi() }
            }
        }
        c.addListener(listener)
        playerListener = listener
    }

    private fun updateUi() {
        val c = controller
        if (c == null) {
            if (isPlayerVisible) {
                isPlayerVisible = false
                root.visibility = View.GONE
                onVisibilityChanged?.invoke(false)
            }
            return
        }

        val hasTrack = c.mediaItemCount > 0 && c.currentMediaItem != null
        val shouldShow = hasTrack && (c.isPlaying || c.playbackState != Player.STATE_IDLE)

        if (shouldShow != isPlayerVisible) {
            isPlayerVisible = shouldShow
            root.visibility = if (shouldShow) View.VISIBLE else View.GONE
            onVisibilityChanged?.invoke(shouldShow)
        }

        if (!shouldShow) return

        val item = c.currentMediaItem
        val metadata = item?.mediaMetadata
        val displayTitle = metadata?.title?.toString()
            ?: item?.mediaId?.substringAfterLast('/')?.substringBeforeLast('.')
            ?: "Musik"
        val displayArtist = metadata?.artist?.toString() ?: ""

        title?.text = displayTitle
        artist?.text = displayArtist.ifBlank { "NOTEZ Audio" }
        title?.isSelected = true

        val isPlaying = c.isPlaying
        playPause?.setImageResource(
            if (isPlaying) R.drawable.ic_lucide_pause else R.drawable.ic_lucide_play
        )

        updateProgress()
        if (isPlaying) {
            handler.removeCallbacks(progressTick)
            handler.post(progressTick)
        }
    }

    private fun updateProgress() {
        val c = controller ?: return
        val currentPos = c.currentPosition
        val duration = c.duration.coerceAtLeast(1L)
        val ratio = (currentPos.toFloat() / duration).coerceIn(0f, 1f)
        progress?.progress = (ratio * 1000).toInt()
    }

    private fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) {
                c.seekTo(0L)
                c.prepare()
            }
            c.play()
        }
        updateUi()
    }

    private fun playPrevious() {
        val c = controller ?: return
        if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
        } else {
            c.seekTo(0L)
        }
        c.play()
        updateUi()
    }

    private fun playNext() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
        } else if (c.mediaItemCount > 0) {
            c.seekTo(0, 0L)
        }
        c.play()
        updateUi()
    }

    private fun setupSwipeGesture() {
        val targetCard = card ?: return
        val threshold = UiStyle.dp(activity, 42f)
        targetCard.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.x
                    startTouchY = event.y
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - startTouchX
                    val dy = event.y - startTouchY
                    if (abs(dx) > threshold && abs(dx) > abs(dy) * 1.5f) {
                        if (dx < 0) {
                            playNext()
                        } else {
                            playPrevious()
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }
}
