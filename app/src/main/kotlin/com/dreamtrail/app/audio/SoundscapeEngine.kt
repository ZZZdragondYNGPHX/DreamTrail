package com.dreamtrail.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundscapeChannel(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    NONE("NONE", "不播放", "静谧无声"),
    MIST_RAIN("MIST_RAIN", "雾林细雨", "轻柔林间落雨与叶片沙沙"),
    TIDAL_WAVES("TIDAL_WAVES", "银滩潮声", "舒缓往复的退潮海浪"),
    HEARTH_FIRE("HEARTH_FIRE", "火塘柴火", "温暖松木微噼声"),
    STUDY_PENDULUM("STUDY_PENDULUM", "书斋钟摆", "沉稳低压节拍与纸页呼吸"),
    CLOUD_CHIMES("CLOUD_CHIMES", "云巅风铃", "空灵五声音阶微风共鸣"),
    BROWN_NOISE("BROWN_NOISE", "沉静棕噪", "浑厚包裹的低频屏蔽声");

    companion object {
        fun fromId(id: String?): SoundscapeChannel =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

/**
 * Pure procedural ambient synthesizer built on PCM 16-bit AudioTrack.
 * Zero external audio assets needed and fully offline-safe.
 */
class SoundscapeEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var fadeJob: Job? = null
    private val playbackMutex = Mutex()
    private var configuredFadeMinutes = 0

    @Volatile private var currentChannel: SoundscapeChannel = SoundscapeChannel.NONE
    @Volatile private var currentVolume: Int = 60
    @Volatile private var isMuted: Boolean = true
    @Volatile private var fadeScale: Float = 1f
    @Volatile private var currentTrack: AudioTrack? = null

    fun applyConfig(
        channel: SoundscapeChannel,
        volume: Int,
        muted: Boolean,
        fadeOutMinutes: Int
    ) {
        val safeVol = volume.coerceIn(0, 100)
        val shouldPlay = !muted && channel != SoundscapeChannel.NONE && safeVol > 0
        val channelChanged = currentChannel != channel
        val safeFadeMinutes = fadeOutMinutes.coerceIn(0, 120)
        val fadeChanged = configuredFadeMinutes != safeFadeMinutes
        configuredFadeMinutes = safeFadeMinutes
        currentVolume = safeVol
        isMuted = muted

        if (!shouldPlay) {
            currentChannel = SoundscapeChannel.NONE
            stopPlayback()
            return
        }

        if (channelChanged || playJob?.isActive != true) {
            currentChannel = channel
            fadeScale = 1f
            startPlayback(channel)
            scheduleFadeOut(safeFadeMinutes)
        } else {
            updateTrackGain()
            if (fadeChanged || (safeFadeMinutes > 0 && fadeJob?.isActive != true)) {
                fadeScale = 1f
                scheduleFadeOut(safeFadeMinutes)
            } else if (fadeOutMinutes <= 0) {
                fadeJob?.cancel()
                fadeScale = 1f
                updateTrackGain()
            }
        }
    }

    fun playCompletionChime(muted: Boolean) {
        if (muted) return
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = (sampleRate * 0.72).toInt()
                val buf = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = if (t < 0.28) 432.0 else 648.0
                    val localT = if (t < 0.28) t else (t - 0.28)
                    val env = kotlin.math.exp(-localT * 5.2) * (1.0 - kotlin.math.exp(-localT * 45.0))
                    val s = sin(2.0 * PI * freq * t) * 0.42 * env
                    buf[i] = (s * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(totalSamples * 2)
                    .build()
                try {
                    track.write(buf, 0, totalSamples)
                    track.play()
                    delay(780)
                } finally {
                    runCatching { track.stop() }
                    track.release()
                }
            }
        }
    }

    fun stopAll() {
        currentChannel = SoundscapeChannel.NONE
        stopPlayback()
    }

    fun release() {
        stopAll()
        scope.cancel()
    }

    private fun scheduleFadeOut(fadeMinutes: Int) {
        fadeJob?.cancel()
        if (fadeMinutes <= 0) {
            fadeScale = 1f
            return
        }
        fadeJob = scope.launch {
            val totalMs = fadeMinutes.coerceAtMost(120) * 60_000L
            val fadeWindowMs = minOf(30_000L, totalMs / 4L).coerceAtLeast(3_000L)
            delay((totalMs - fadeWindowMs).coerceAtLeast(0L))
            val steps = 20
            val stepDelay = fadeWindowMs / steps
            for (i in 1..steps) {
                fadeScale = (1f - i.toFloat() / steps).coerceIn(0f, 1f)
                updateTrackGain()
                delay(stepDelay)
            }
            stopPlayback()
        }
    }

    private fun startPlayback(channel: SoundscapeChannel) {
        stopPlayback()

        playJob = scope.launch {
            playbackMutex.withLock {
                ensureActive()
                val sampleRate = 22050
                val minBufBytes = runCatching {
                    AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                }.getOrElse { 4096 }.coerceAtLeast(2048)

                val track = runCatching {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(minBufBytes * 2)
                        .build()
                }.getOrNull() ?: return@withLock

                currentTrack = track
                updateTrackGain()
                runCatching { track.play() }

                val chunkSize = 2048
                val pcm = ShortArray(chunkSize)
                val rng = Random(channel.ordinal * 9173 + 17)
                var brown = 0.0
                var lp = 0.0
                var hpPrev = 0.0
                var prevRaw = 0.0
                var phase = 0L

                try {
                    while (isActive && currentChannel == channel) {
                        val gain = ((currentVolume / 100f) * fadeScale).coerceIn(0f, 1f)
                        if (isMuted || gain <= 0.005f) {
                            pcm.fill(0)
                        } else {
                            for (i in 0 until chunkSize) {
                                val t = phase.toDouble() / sampleRate
                                phase++
                                val white = rng.nextDouble() * 2.0 - 1.0
                                brown = (brown + white * 0.025).coerceIn(-1.0, 1.0)
                                lp = lp * 0.92 + white * 0.08
                                val hp = white - prevRaw + 0.85 * hpPrev
                                prevRaw = white
                                hpPrev = hp

                                val sample = when (channel) {
                                    SoundscapeChannel.NONE -> 0.0
                                    SoundscapeChannel.MIST_RAIN -> lp * 0.45 + hp * 0.06
                                    SoundscapeChannel.TIDAL_WAVES -> {
                                        val wave = 0.25 + 0.75 * ((sin(2.0 * PI * 0.11 * t) + 1.0) * 0.5)
                                        brown * 0.65 * wave + lp * 0.12 * wave
                                    }
                                    SoundscapeChannel.HEARTH_FIRE -> {
                                        val crackle = if (rng.nextInt(1400) == 0) (rng.nextDouble() - 0.5) * 0.9 else 0.0
                                        brown * 0.55 + lp * 0.15 + crackle
                                    }
                                    SoundscapeChannel.STUDY_PENDULUM -> {
                                        val beatPos = t % 1.0
                                        val tick = if (beatPos < 0.022) {
                                            sin(2.0 * PI * 680.0 * beatPos) * (1.0 - beatPos / 0.022) * 0.22
                                        } else 0.0
                                        brown * 0.35 + tick
                                    }
                                    SoundscapeChannel.CLOUD_CHIMES -> {
                                        val chord = sin(2.0 * PI * 261.6 * t) * 0.05 +
                                            sin(2.0 * PI * 329.6 * t) * 0.04 +
                                            sin(2.0 * PI * 392.0 * t) * 0.03
                                        val swell = (sin(2.0 * PI * 0.14 * t) + 1.0) * 0.5
                                        brown * 0.30 + chord * swell
                                    }
                                    SoundscapeChannel.BROWN_NOISE -> brown * 0.85
                                }
                                val scaled = (sample * Short.MAX_VALUE * 0.42).toInt()
                                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                pcm[i] = scaled.toShort()
                            }
                        }
                        val written = runCatching { track.write(pcm, 0, chunkSize) }.getOrDefault(-1)
                        if (written <= 0) break
                    }
                } finally {
                    runCatching {
                        track.pause()
                        track.flush()
                        track.stop()
                    }
                    runCatching { track.release() }
                    if (currentTrack === track) currentTrack = null
                }
            }
        }
    }

    private fun stopPlayback() {
        fadeJob?.cancel()
        playJob?.cancel()
        runCatching {
            currentTrack?.pause()
            currentTrack?.flush()
            currentTrack?.stop()
        }
    }

    private fun updateTrackGain() {
        val g = if (isMuted) 0f else ((currentVolume / 100f) * fadeScale).coerceIn(0f, 1f)
        runCatching { currentTrack?.setVolume(g) }
    }
}
