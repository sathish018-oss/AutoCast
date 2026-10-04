package com.autocast.app.service

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class AutoCastCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return try {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist)
                .build()
        } catch (e: Exception) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        }
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: android.content.Intent): androidx.car.app.Screen {
                return com.autocast.app.ui.AutoCastScreen(carContext)
            }
        }
    }
}
