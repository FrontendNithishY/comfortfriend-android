package com.comfortfriend.app

import android.app.Application

class ComfortFriendApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
