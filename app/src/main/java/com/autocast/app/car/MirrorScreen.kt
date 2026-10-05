package com.autocast.app.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate

/** Navigation-category screen: the car host gives us a drawable surface behind it. */
class MirrorScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("AutoCast")
                    .setOnClickListener { CarSurfaceRenderer.drawMessage("Mirroring phone screen...") }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(strip)
            .setBackgroundColor(CarColor.createCustom(0xFF000000.toInt(), 0xFF000000.toInt()))
            .build()
    }
}
