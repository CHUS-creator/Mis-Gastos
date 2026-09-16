package com.misgastos.app

import android.app.Application
import com.misgastos.app.viewmodel.misGastosApplication

class MisGastosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        misGastosApplication = this
    }
}
