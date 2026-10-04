package com.autocast.app.service

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.autocast.app.ui.AutoCastScreen

class AutoCastCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allows connection from any host (including Android Auto Desktop Head Unit and Developer Mode hosts)
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: android.content.Intent): androidx.car.app.Screen {
                return AutoCastScreen(carContext)
            }
        }
    }
}
