package com.autocast.app

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.autocast.app.databinding.ActivityMainBinding
import com.autocast.app.service.ScreenCastService
import com.autocast.app.touch.TouchInputService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
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
        } else {
            Toast.makeText(this, "MediaProjection permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateUIState()
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
                startScreenCastPermissionCheck()
            }
        }

        binding.btnAccessibilityPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'AutoCast Touch Service' for touch passthrough", Toast.LENGTH_LONG).show()
        }
    }

    private fun startScreenCastPermissionCheck() {
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
