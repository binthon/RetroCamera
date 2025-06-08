package com.example.retrocamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retrocamera.camera.CameraManager
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.retrocamera.camera.CameraViewModel
import com.example.retrocamera.galeria.GalleryScreen
import com.example.retrocamera.galeria.GalleryViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") {
                    val cameraViewModel: CameraViewModel = viewModel()
                    val cameraManager = CameraManager(
                        context = this@MainActivity,
                        viewModel = cameraViewModel
                    )
                    cameraManager.ShowCameraWithShader(
                        onGalleryClick = { navController.navigate("gallery") }
                    )
                }

                composable("gallery") {
                    GalleryScreen(
                        viewModel = GalleryViewModel(this@MainActivity),
                        onBackToCamera = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}