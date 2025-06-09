package com.example.retrocamera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.retrocamera.api.exchangeAuthCodeForAccessToken
import com.example.retrocamera.camera.CameraManager
import com.example.retrocamera.camera.CameraViewModel
import com.example.retrocamera.galeria.GalleryScreen
import com.example.retrocamera.galeria.GalleryViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AuthSession {
    var accessToken: String? = null
}
const val clientId = BuildConfig.GOOGLE_CLIENT_ID

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

        // Konfiguracja logowania Google
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"),
                Scope("https://www.googleapis.com/auth/photoslibrary.readonly")
            )
            .requestServerAuthCode(clientId)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)

        if (AuthSession.accessToken == null) {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 123)
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                val authCode = account.serverAuthCode
                if (authCode != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val token = exchangeAuthCodeForAccessToken(this@MainActivity, authCode)
                        AuthSession.accessToken = token
                    }
                } else {
                    Log.e("LOGIN", "serverAuthCode is null even after sign-in")
                }
            } else {
                Log.e("LOGIN", "Sign-in failed")
            }

        }
    }
}
