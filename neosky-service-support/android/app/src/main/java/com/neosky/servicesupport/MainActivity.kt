package com.neosky.servicesupport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neosky.servicesupport.presentation.navigation.NeoSkyNavHost
import com.neosky.servicesupport.presentation.theme.NeoSkyServiceSupportTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeoSkyServiceSupportTheme {
                val currentUser by mainViewModel.currentUser.collectAsState()
                val isOffline by mainViewModel.isOffline.collectAsState()
                NeoSkyNavHost(
                    isLoggedIn = currentUser != null,
                    isOffline = isOffline,
                )
            }
        }
    }
}
