package com.ioniq.companion.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.ioniq.companion.auto.screens.AutoDrivingScreen
import com.ioniq.companion.util.AppLogger

class IoniqCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        AppLogger.i("ANDROID_AUTO", "onCreateScreen: Rendering in-car AutoDrivingScreen")
        return AutoDrivingScreen(carContext)
    }
}
