package com.fern.vpn

import android.app.Application
import com.fern.vpn.data.SettingsRepository

class FernApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
    }
}
