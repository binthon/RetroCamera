package com.example.retrocamera.video

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class VideoRecorder(private val context: Context) {

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoSurface: Surface? = null
    private var trackIndex = -1
    private var isRecording = false
    private var muxerStarted = false
    private var outputFilePath: String? = null

    fun getInputSurface(): Surface? = videoSurface

    fun startRecording(width: Int = 1280, height: Int = 720, bitrate: Int = 5_000_000) {
        if (isRecording) return

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoSurface = createInputSurface()
            start()
        }

        outputFilePath = File(context.cacheDir, "temp_shader_video.mp4").absolutePath
        mediaMuxer = MediaMuxer(outputFilePath!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        isRecording = true
        muxerStarted = false

        Thread {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isRecording) {
                val outputBufferId = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                if (outputBufferId >= 0) {
                    val encodedData = mediaCodec!!.getOutputBuffer(outputBufferId) ?: continue
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        if (!muxerStarted) {
                            trackIndex = mediaMuxer!!.addTrack(mediaCodec!!.outputFormat)
                            mediaMuxer!!.start()
                            muxerStarted = true
                        }
                        mediaMuxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    mediaCodec!!.releaseOutputBuffer(outputBufferId, false)
                }
            }

            try {
                videoSurface?.release()
                mediaCodec?.signalEndOfInputStream()
                mediaCodec?.stop()
                mediaCodec?.release()
                mediaCodec = null

                if (muxerStarted) {
                    mediaMuxer?.stop()
                    mediaMuxer?.release()
                }
                mediaMuxer = null
                saveToGallery()
            } catch (e: Exception) {
                Log.e("VideoRecorder", "Błąd przy zatrzymywaniu nagrywania", e)
            }
        }.start()
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
    }

    private fun saveToGallery() {
        try {
            val filename = "ShaderVideo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/RetroCamera")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { outputStream ->
                File(outputFilePath!!).inputStream().copyTo(outputStream)
            }

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            Log.d("VideoRecorder", "Zapisano video: $uri")
        } catch (e: Exception) {
            Log.e("VideoRecorder", "Błąd zapisu video do galerii", e)
        }
    }

    fun setSurface(surface: Surface?) {
        videoSurface = surface
    }
}

fun com.example.retrocamera.filters.CameraShaderRenderer.setVideoSurface(surface: Surface?) {
    this.setVideoSurface(surface)
}
