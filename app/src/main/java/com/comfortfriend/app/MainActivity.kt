package com.comfortfriend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comfortfriend.app.data.prefs.ThemeChoice
import com.comfortfriend.app.ui.AppNav
import com.comfortfriend.app.ui.MainViewModel
import com.comfortfriend.app.ui.VMFactory
import com.comfortfriend.app.ui.theme.ComfortFriendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ComfortFriendApp).container
        setContent {
            val vm: MainViewModel = viewModel(factory = VMFactory(container))
            val settings by vm.settings.collectAsState()
            ComfortFriendTheme(settings?.theme ?: ThemeChoice.SYSTEM) {
                AppNav(container, vm)
            }
        }
    }
}
