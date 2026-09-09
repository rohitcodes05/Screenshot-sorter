package com.screensort.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.screensort.app.ui.screens.MainScreen
import com.screensort.app.ui.theme.ScreenSortTheme
import com.screensort.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as ScreenSortApp
        MainViewModel.Factory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScreenSortTheme {
                // Storage permission launcher
                val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.scanDeviceScreenshots()
                    } else {
                        Toast.makeText(
                            this,
                            "Permission needed to scan device screenshots",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // File/image picker launcher for manual imports
                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetMultipleContents()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        viewModel.importImages(uris)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onTriggerScan = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                this,
                                storagePermission
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                viewModel.scanDeviceScreenshots()
                            } else {
                                permissionLauncher.launch(storagePermission)
                            }
                        },
                        onTriggerImport = {
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
}
