package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
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
import java.io.IOException

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    COMPLETED
}

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var pausedTimeAccumulator = 0L
    private var pauseStartTime = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudesHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudesHistory: StateFlow<List<Float>> = _amplitudesHistory.asStateFlow()

    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    fun startRecording(): File? {
        if (_recordingState.value == RecordingState.RECORDING) return currentOutputFile

        requestAudioFocus()

        val outputDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val outputFile = File(outputDir, "rec_${System.currentTimeMillis()}.m4a")
        currentOutputFile = outputFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setAudioChannels(1) // Mono for speech
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            pausedTimeAccumulator = 0L
            _recordingState.value = RecordingState.RECORDING
            _amplitudesHistory.value = emptyList()

            startTicker()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            return null
        }
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            try {
                mediaRecorder?.pause()
                pauseStartTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.PAUSED
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED) {
            try {
                mediaRecorder?.resume()
                pausedTimeAccumulator += (System.currentTimeMillis() - pauseStartTime)
                _recordingState.value = RecordingState.RECORDING
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording(): File? {
        if (_recordingState.value == RecordingState.IDLE) return null

        val resultFile = currentOutputFile
        try {
            tickerJob?.cancel()
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    // Stop failed, e.g. immediate stop after start
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            abandonAudioFocus()
            _recordingState.value = RecordingState.COMPLETED
            _currentAmplitude.value = 0f
        }

        return resultFile
    }

    fun cancelRecording() {
        tickerJob?.cancel()
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    // Ignore
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            abandonAudioFocus()
            currentOutputFile?.delete()
            currentOutputFile = null
            _recordingState.value = RecordingState.IDLE
            _elapsedTimeMs.value = 0L
            _currentAmplitude.value = 0f
            _amplitudesHistory.value = emptyList()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && (_recordingState.value == RecordingState.RECORDING || _recordingState.value == RecordingState.PAUSED)) {
                if (_recordingState.value == RecordingState.RECORDING) {
                    val currentElapsed = System.currentTimeMillis() - recordingStartTime - pausedTimeAccumulator
                    _elapsedTimeMs.value = currentElapsed

                    val rawAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalizedAmp = (rawAmp / 32767f).coerceIn(0.05f, 1f)
                    _currentAmplitude.value = normalizedAmp

                    val currentList = _amplitudesHistory.value.toMutableList()
                    currentList.add(normalizedAmp)
                    if (currentList.size > 80) {
                        currentList.removeAt(0)
                    }
                    _amplitudesHistory.value = currentList
                }
                delay(60)
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .build()
            focusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun cleanup() {
        tickerJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        _recordingState.value = RecordingState.IDLE
        abandonAudioFocus()
    }
}
