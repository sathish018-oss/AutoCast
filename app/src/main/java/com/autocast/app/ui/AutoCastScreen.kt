package com.autocast.app.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Item
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.autocast.app.service.ScreenCastService

class AutoCastScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val currentMode = if (ScreenCastService.activeMode == ScreenCastService.MODE_YOUTUBE) {
            "YouTube Player"
        } else {
            "Screen Mirroring"
        }

        val listBuilder = ItemList.Builder()

        // Mode Indicator & Quick Switch Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Streaming Mode: $currentMode")
                .addText("Tap to switch between YouTube Widescreen Player & Full Device Mirroring")
                .setOnClickListener(ParkedOnlyOnClickListener.create {
                    ScreenCastService.activeMode = if (ScreenCastService.activeMode == ScreenCastService.MODE_YOUTUBE) {
                        ScreenCastService.MODE_SCREEN_CAST
                    } else {
                        ScreenCastService.MODE_YOUTUBE
                    }
                    invalidate()
                })
                .build()
        )

        // YouTube Control Deck
        listBuilder.addItem(
            Row.Builder()
                .setTitle("YouTube Widescreen Player Deck")
                .addText("Play / Pause & control YouTube playback on car display")
                .setOnClickListener(ParkedOnlyOnClickListener.create {
                    ScreenCastService.activeMode = ScreenCastService.MODE_YOUTUBE
                    invalidate()
                })
                .build()
        )

        // Full Screen Mirroring Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Full Device Screen Mirroring")
                .addText("Mirror phone display & audio to car screen (Low Latency H.264)")
                .setOnClickListener(ParkedOnlyOnClickListener.create {
                    ScreenCastService.activeMode = ScreenCastService.MODE_SCREEN_CAST
                    invalidate()
                })
                .build()
        )

        // Quick Seek Controls
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Video Navigation")
                .addText("10s Rewind | 10s Fast-Forward")
                .setOnClickListener(ParkedOnlyOnClickListener.create {
                    // Action triggered
                    invalidate()
                })
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("AutoCast - Media Stream")
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
