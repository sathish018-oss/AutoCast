package com.autocast.app.service

import android.content.Intent
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.validation.HostValidator
import com.autocast.app.car.CarSurfaceRenderer
import com.autocast.app.car.MirrorScreen

class AutoCastCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                carContext.getCarService(AppManager::class.java)
                    .setSurfaceCallback(object : SurfaceCallback {
                        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
                            CarSurfaceRenderer.setSurface(
                                surfaceContainer.surface,
                                surfaceContainer.width,
                                surfaceContainer.height
                            )
                        }

                        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
                            CarSurfaceRenderer.setSurface(null, 0, 0)
                        }
                    })
                return MirrorScreen(carContext)
            }
        }
    }
}
