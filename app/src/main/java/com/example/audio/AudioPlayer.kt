package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var trackerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun loadAudio(filePath: String) {
        if (filePath == currentFilePath && mediaPlayer != null) return

        release()
        currentFilePath = filePath

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            _durationMs.value = 0L
            return
        }

        try {
            val player = MediaPlayer()
            player.setDataSource(filePath)
            player.prepare()
            _durationMs.value = player.duration.toLong().coerceAtLeast(0L)
            _currentPositionMs.value = 0L
            _isPlaying.value = false

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = _durationMs.value
                trackerJob?.cancel()
            }

            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
            _durationMs.value = 0L
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = PlaybackParams()
                params.speed = _playbackSpeed.value
                player.playbackParams = params
            }
            player.start()
            _isPlaying.value = true
            startPositionTracker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            _isPlaying.value = false
            trackerJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            val clamped = positionMs.coerceIn(0L, _durationMs.value)
            player.seekTo(clamped.toInt())
            _currentPositionMs.value = clamped
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val player = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (player.isPlaying) {
                    val params = PlaybackParams()
                    params.speed = speed
                    player.playbackParams = params
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPositionTracker() {
        trackerJob?.cancel()
        trackerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition.toLong()
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                delay(100)
            }
        }
    }

    fun release() {
        trackerJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        currentFilePath = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
    }
}
