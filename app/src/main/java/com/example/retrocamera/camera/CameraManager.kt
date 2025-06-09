package com.example.retrocamera.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

//warstwa miedzy logiką a wartwą UI
class CameraManager(
    private val context: Context,
    private val viewModel: CameraViewModel
) {

    // funkcja do zapisu zdjęcia w galerii
    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "ShaderPhoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        // dane do zapisu
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RetroCamera")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        // zapis bitmapy jako jpeg
        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                // po przekazaniu zapisu do backgroudy tu dostaje odp o sukcesie lub nie odnośnie zapisu
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        if (success) "Zapisano zdjęcie w galerii" else "Błąd zapisu zdjęcia",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    // główna funckcja programu która pokazuje kamere z filtrem, ikony
    @Composable
    fun ShowCameraWithShader(onGalleryClick: () -> Unit) {
        val glSurfaceRef = remember { mutableStateOf<SurfaceView?>(null) }
        val configuration = LocalContext.current.resources.configuration
        val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

        Box(modifier = Modifier.fillMaxSize()) {
            CameraShaderScreen(
                // render camerashader na viewmodel
                shaderRendererSetter = { viewModel.shaderRenderer.value = it },
                //referencja przez surfaceview do glsurfaceref
                surfaceViewSetter = { glSurfaceRef.value = it }
            )

            // Prawy dolny róg — przyciski zdjęcie
            Column(
                modifier = if (isPortrait) {
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 50.dp, bottom = 50.dp)

                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)

                },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FloatingActionButton(
                    onClick = {
                        val view = glSurfaceRef.value
                        if (view == null) {
                            Toast.makeText(context, "SurfaceView niegotowy", Toast.LENGTH_SHORT).show()
                            return@FloatingActionButton
                        }

                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        val handler = Handler(Looper.getMainLooper())

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // uzycie pixelcopy do przekazania bitmay z obrazu
                            PixelCopy.request(view, bitmap, { result ->
                                if (result == PixelCopy.SUCCESS) {
                                    saveBitmapToGallery(bitmap)
                                }
                            }, handler)
                        } else {
                            Toast.makeText(context, "Wymaga Androida 7.0+", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(if (isPortrait) 56.dp else 80.dp)
                ) {
                    Icon(
                        Icons.Default.Camera,
                        contentDescription = "Zrób zdjęcie",
                        modifier = Modifier.size(if (isPortrait) 48.dp else 64.dp) // ⬅
                    )
                }


            }

            //Lewy dolny róg — przycisk galerii
            FloatingActionButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .padding(bottom = 50.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria")
            }
        }

    }
}
