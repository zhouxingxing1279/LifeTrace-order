package com.lifetrace.order

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class OrderApplication : Application(), DefaultLifecycleObserver {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        container.syncManager.onForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        container.syncManager.onBackground()
    }
}
