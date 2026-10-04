package com.autocast.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autocast.app.databinding.ActivityMainBinding
import com.autocast.app.service.ScreenCastService

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
                Toast.makeText(this, "Screen Casting Started for Android Auto", Toast.LENGTH_SHORT).show()
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

        checkInstallerPackage()
        setupListeners()
        updateUIState()
    }

    private fun checkInstallerPackage() {
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }

        if (installer != "com.android.vending") {
            binding.tvStatus.text = "Notice: Installed directly"
            binding.tvActiveMode.text = "Tip: Sideload via KingInstaller or ADB with -i com.android.vending"
        }
    }

    private fun setupListeners() {
        binding.toggleModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnYouTubeMode -> {
                        binding.tvActiveMode.text = "Active Mode: YouTube Player UI"
                        ScreenCastService.activeMode = ScreenCastService.MODE_YOUTUBE
                    }
                    R.id.btnScreenCastMode -> {
                        binding.tvActiveMode.text = "Active Mode: Full Screen Mirroring"
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

        binding.btnAccessibilityPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'AutoCast Touch Service' for touch passthrough", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionsAndStart() {
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
}
