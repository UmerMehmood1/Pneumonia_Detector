package com.umer.pneumoniadetector

import android.app.Application
import com.google.firebase.FirebaseApp

class PneumoniaApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

}