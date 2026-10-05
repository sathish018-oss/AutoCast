package com.autocast.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autocast.app.databinding.ActivityMainBinding
import com.autocast.app.service.ScreenCastService
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchMediaProjectionPermission()
        } else {
            Toast.makeText(this, "Notification permission required for casting", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                val serviceIntent = Intent(this, ScreenCastService::class.java).apply {
                    action = ScreenCastService.ACTION_START
                    putExtra(ScreenCastService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenCastService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                isServiceRunning = true
                updateUIState()
                Toast.makeText(this, "Screen Casting Active - Video Streaming to Car", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error starting cast service: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "MediaProjection permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupYouTubeWebView()
        setupListeners()
        updateUIState()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupYouTubeWebView() {
        binding.youtubeWebView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    request?.url?.toString()?.let { url ->
                        ScreenCastService.loadUrlInPresentation(url)
                    }
                    return false
                }
            }
            loadUrl("https://m.youtube.com")
        }
    }

    private fun setupListeners() {
        binding.toggleModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnYouTubeMode -> {
                        binding.tvActiveMode.text = "Mode: YouTube Widescreen"
                        binding.youtubeBarLayout.visibility = View.VISIBLE
                        binding.youtubeWebView.visibility = View.VISIBLE
                        binding.tvScreenCastPlaceholder.visibility = View.GONE
                        ScreenCastService.activeMode = ScreenCastService.MODE_YOUTUBE
                    }
                    R.id.btnScreenCastMode -> {
                        binding.tvActiveMode.text = "Mode: Full Screen Mirroring"
                        binding.youtubeBarLayout.visibility = View.GONE
                        binding.youtubeWebView.visibility = View.GONE
                        binding.tvScreenCastPlaceholder.visibility = View.VISIBLE
                        ScreenCastService.activeMode = ScreenCastService.MODE_SCREEN_CAST
                    }
                }
            }
        }

        binding.btnToggleService.setOnClickListener {
            if (isServiceRunning) {
                stopScreenCastService()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.btnSearch.setOnClickListener {
            performYouTubeSearch()
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performYouTubeSearch()
                true
            } else {
                false
            }
        }

        binding.btnHome.setOnClickListener {
            val targetUrl = "https://m.youtube.com"
            binding.youtubeWebView.loadUrl(targetUrl)
            ScreenCastService.loadUrlInPresentation(targetUrl)
        }

        binding.btnOverlayPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    Toast.makeText(this, "Enable 'Display over other apps' for AutoCast", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAccessibilityPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'AutoCast Touch Service' for car touch passthrough", Toast.LENGTH_LONG).show()
        }
    }

    private fun performYouTubeSearch() {
        val query = binding.etSearchQuery.text.toString().trim()
        val targetUrl = if (query.isNotEmpty()) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            "https://m.youtube.com/results?search_query=$encodedQuery"
        } else {
            "https://m.youtube.com"
        }
        binding.youtubeWebView.loadUrl(targetUrl)
        ScreenCastService.loadUrlInPresentation(targetUrl)
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Please grant 'Display over other apps' to stream video to your car", Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        launchMediaProjectionPermission()
    }

    private fun launchMediaProjectionPermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun stopScreenCastService() {
        val serviceIntent = Intent(this, ScreenCastService::class.java).apply {
            action = ScreenCastService.ACTION_STOP
        }
        startService(serviceIntent)
        isServiceRunning = false
        updateUIState()
        Toast.makeText(this, "Screen Casting Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIState() {
        if (isServiceRunning) {
            binding.tvStatus.text = "Status: Streaming Active"
            binding.btnToggleService.text = getString(R.string.stop_mirroring)
        } else {
            binding.tvStatus.text = "Status: Ready to connect"
            binding.btnToggleService.text = getString(R.string.start_mirroring)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.youtubeWebView.visibility == View.VISIBLE && binding.youtubeWebView.canGoBack()) {
            binding.youtubeWebView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
