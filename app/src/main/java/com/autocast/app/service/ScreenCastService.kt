package com.autocast.app.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.app.NotificationCompat
import com.autocast.app.R
import com.autocast.app.presentation.AutoCastPresentation

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
        var activePresentation: AutoCastPresentation? = null

        fun loadUrlInPresentation(url: String) {
            activePresentation?.loadUrl(url)
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var displayManager: DisplayManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            checkAndShowPresentation()
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (activePresentation?.display?.displayId == displayId) {
                dismissPresentation()
            }
        }

        override fun onDisplayChanged(displayId: Int) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, mainHandler)
    }

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
                    checkAndShowPresentation()
                }
            }
            ACTION_STOP -> {
                stopScreenCapture()
                dismissPresentation()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoCast Active")
            .setContentText("Streaming YouTube & device content to Android Auto")
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
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopScreenCapture()
                }
            }, null)
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "AutoCastStream",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun checkAndShowPresentation() {
        val targetDisplay = virtualDisplay?.display ?: run {
            displayManager.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        }

        if (targetDisplay != null) {
            mainHandler.post {
                try {
                    activePresentation?.dismiss()
                    val presentation = AutoCastPresentation(this, targetDisplay)
                    presentation.show()
                    activePresentation = presentation
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun dismissPresentation() {
        mainHandler.post {
            try {
                activePresentation?.dismiss()
                activePresentation = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopScreenCapture() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(displayListener)
        dismissPresentation()
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
