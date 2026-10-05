package com.autocast.app.service

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.autocast.app.R
import com.autocast.app.car.CarSurfaceRenderer

class ScreenCastService : Service() {

    companion object {
        const val ACTION_START = "com.autocast.app.action.START"
        const val ACTION_STOP = "com.autocast.app.action.STOP"
        const val EXTRA_RESULT_CODE = "com.autocast.app.extra.RESULT_CODE"
        const val EXTRA_RESULT_DATA = "com.autocast.app.extra.RESULT_DATA"

        const val CHANNEL_ID = "AutoCastStreamChannel"
        const val NOTIFICATION_ID = 1001

        const val MODE_YOUTUBE = 0
        const val MODE_SCREEN_CAST = 1

        var activeMode: Int = MODE_YOUTUBE

        /** Kept for compatibility; video now goes to the car via CarSurfaceRenderer. */
        fun loadUrlInPresentation(url: String) {}

        private const val SAMPLE_RATE = 44100
        private const val FRAME_INTERVAL_MS = 66L // ~15 fps, low quality is fine
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var frameBitmap: Bitmap? = null
    private var lastFrameTime = 0L

    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    @Volatile private var audioRunning = false
    private var audioThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    startForegroundServiceWithNotification()
                    try {
                        startScreenCapture(resultCode, resultData)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            ACTION_STOP -> {
                stopScreenCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoCast Active")
            .setContentText("Mirroring phone screen and audio to the car")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, resultData)
        mediaProjection = projection

        captureThread = HandlerThread("AutoCastCapture").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)

        // Android 14+ requires a callback registered before createVirtualDisplay.
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopScreenCapture()
            }
        }, captureHandler)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        // Downscale to roughly 640px wide for low latency.
        val scale = 640f / metrics.widthPixels
        val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(2)
        val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(2)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { reader ->
            reader.setOnImageAvailableListener({ r ->
                val image = try { r.acquireLatestImage() } catch (e: Exception) { null } ?: return@setOnImageAvailableListener
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameTime < FRAME_INTERVAL_MS) return@setOnImageAvailableListener
                    lastFrameTime = now

                    val plane = image.planes[0]
                    val pixelStride = plane.pixelStride
                    val rowPadding = plane.rowStride - pixelStride * width
                    val fullWidth = width + rowPadding / pixelStride
                    var bmp = frameBitmap
                    if (bmp == null || bmp.width != fullWidth || bmp.height != height) {
                        bmp = Bitmap.createBitmap(fullWidth, height, Bitmap.Config.ARGB_8888)
                        frameBitmap = bmp
                    }
                    plane.buffer.rewind()
                    bmp!!.copyPixelsFromBuffer(plane.buffer)
                    val cropped = if (fullWidth != width) Bitmap.createBitmap(bmp, 0, 0, width, height) else bmp
                    CarSurfaceRenderer.drawFrame(cropped)
                } finally {
                    image.close()
                }
            }, captureHandler)
        }

        virtualDisplay = projection.createVirtualDisplay(
            "AutoCastStream",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        startAudioCapture(projection)
    }

    private fun startAudioCapture(projection: MediaProjection) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = minBuf * 2

            val record = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufSize)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioRecord = record
            audioTrack = track
            audioRunning = true
            record.startRecording()
            track.play()

            audioThread = Thread {
                val buffer = ByteArray(bufSize)
                while (audioRunning) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) track.write(buffer, 0, read)
                }
            }.also { it.start() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopScreenCapture() {
        try {
            audioRunning = false
            audioThread?.join(300)
            audioThread = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            captureThread?.quitSafely()
            captureThread = null
            CarSurfaceRenderer.drawMessage("Mirroring stopped")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopScreenCapture()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoCast Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
