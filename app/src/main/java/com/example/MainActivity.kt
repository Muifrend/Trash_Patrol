package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Fused Location Service Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // System Back gesture mapping to return to reporting screen
                val currentScreen by viewModel.currentScreen.collectAsState()
                BackHandler(enabled = currentScreen != AppScreen.Report) {
                    viewModel.navigateTo(AppScreen.Report)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF0D0D0D)
                ) {
                    MainAppContainer(
                        viewModel = viewModel,
                        onGpsRequest = { requestDeviceGpsLocation() }
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestDeviceGpsLocation() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (hasFine || hasCoarse) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        viewModel.updateLocation(location)
                    }
                }.addOnFailureListener {
                    // Fail gracefully
                }
            } catch (e: SecurityException) {
                // Safeguard against missing permissions
            }
        } else {
            // No location permission granted, submit without coordinates
            viewModel.updateLocation(null)
        }
    }
}
