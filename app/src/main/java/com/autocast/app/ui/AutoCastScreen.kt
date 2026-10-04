package com.autocast.app.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.autocast.app.service.ScreenCastService

class AutoCastScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val currentMode = if (ScreenCastService.activeMode == ScreenCastService.MODE_YOUTUBE) {
            "YouTube Player UI"
        } else {
            "Full Device Mirroring"
        }

        val listBuilder = ItemList.Builder()

        // Mode Switcher Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Active Mode: $currentMode")
                .addText("Tap to toggle YouTube Widescreen Player / Screen Mirroring")
                .setOnClickListener {
                    ScreenCastService.activeMode = if (ScreenCastService.activeMode == ScreenCastService.MODE_YOUTUBE) {
                        ScreenCastService.MODE_SCREEN_CAST
                    } else {
                        ScreenCastService.MODE_YOUTUBE
                    }
                    invalidate()
                }
                .build()
        )

        // YouTube Widescreen Player Deck
        listBuilder.addItem(
            Row.Builder()
                .setTitle("YouTube Widescreen Player")
                .addText("Tap to launch YouTube HTML5 Player on car display")
                .setOnClickListener {
                    ScreenCastService.activeMode = ScreenCastService.MODE_YOUTUBE
                    invalidate()
                }
                .build()
        )

        // Full Device Screen Mirroring Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Full Device Screen Mirroring")
                .addText("Tap to mirror phone screen & audio in real-time")
                .setOnClickListener {
                    ScreenCastService.activeMode = ScreenCastService.MODE_SCREEN_CAST
                    invalidate()
                }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("AutoCast Pro")
            .setSingleList(listBuilder.build())
            .build()
    }
}
