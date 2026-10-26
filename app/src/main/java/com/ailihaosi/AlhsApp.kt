package com.ailihaosi

import android.app.Application
import com.ailihaosi.data.api.NetworkModule

class AlhsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)
    }
}
