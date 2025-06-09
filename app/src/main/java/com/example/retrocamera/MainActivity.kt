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

// zapis logowania do google
object AuthSession {
    var accessToken: String? = null
}

// clientID pobrany z gradle.properties
const val clientId = BuildConfig.GOOGLE_CLIENT_ID



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // nawigacja nav
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") {
                    //viewmodel przechowuje filtr i sufaceTexture
                    val cameraViewModel: CameraViewModel = viewModel()

                    //logika i działanie kamery
                    val cameraManager = CameraManager(
                        context = this@MainActivity,
                        viewModel = cameraViewModel
                    )

                    // ui kamery z shaderem
                    cameraManager.ShowCameraWithShader(
                        onGalleryClick = { navController.navigate("gallery") }
                    )
                }

                // ekran galeruu
                composable("gallery") {
                    GalleryScreen(
                        viewModel = GalleryViewModel(this@MainActivity),
                        //powrót do kamery
                        onBackToCamera = { navController.popBackStack() }
                    )
                }
            }
        }

        // Konfiguracja logowania Google
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                // Potrzebne do odczytu/zapisu zdjęć z Google Photos
                Scope("https://www.googleapis.com/auth/photoslibrary.appendonly"),
                Scope("https://www.googleapis.com/auth/photoslibrary.readonly")
            )
            .requestServerAuthCode(clientId)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
        //jak nie ma tokena to logowanie
        if (AuthSession.accessToken == null) {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 123)
        }


    }

    // powrót z logowania
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                val authCode = account.serverAuthCode
                // jeśli jest authcode to wymiana na token
                if (authCode != null) {
                    //wątek współbieżny, kamera działa w tle
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

// czyli w mainacivity jest autoryazcja authCode wysyłany do exchangeAuthCodeForAccessToken gdzie po poprawny pobraniu
// access_token funkcja moze wrzucić plik do google photos
