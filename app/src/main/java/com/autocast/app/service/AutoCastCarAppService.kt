package com.autocast.app.service

import androidx.car.app.CarAppService
import androidx.car.app.HostInfo
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class AutoCastCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return object : HostValidator {
            override fun isValidHost(hostInfo: HostInfo): Boolean {
                return true
            }
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
