package com.example.retrocamera.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.retrocamera.filters.CameraShaderRenderer
import com.example.retrocamera.ui.CameraShaderScreen
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CameraManager(
    private val context: Context,
    private val previewView: PreviewView? = null,
    private val lifecycleOwner: LifecycleOwner
) {
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var shaderRenderer: CameraShaderRenderer? = null

    fun startCamera() {
        if (previewView == null) return

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Błąd kamery: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun tapToFocus(x: Float, y: Float) {
        previewView?.let {
            val factory = it.meteringPointFactory
            val point = factory.createPoint(x, y)

            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            camera?.cameraControl?.startFocusAndMetering(action)
        }
    }

    fun setShaderRenderer(renderer: CameraShaderRenderer) {
        shaderRenderer = renderer
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "ShaderPhoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RetroCamera")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
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

    @Composable
    fun ShowCameraWithShader() {
        val glSurfaceRef = remember { mutableStateOf<SurfaceView?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            CameraShaderScreen(
                shaderRendererSetter = { setShaderRenderer(it) },
                surfaceViewSetter = { glSurfaceRef.value = it }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp, top = 32.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(onClick = {
                    val view = glSurfaceRef.value
                    if (view == null) {
                        Toast.makeText(context, "SurfaceView niegotowy", Toast.LENGTH_SHORT).show()
                        return@FloatingActionButton
                    }

                    val outBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    val handler = Handler(Looper.getMainLooper())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        PixelCopy.request(view, outBitmap, { result ->
                            if (result == PixelCopy.SUCCESS) {
                                saveBitmapToGallery(outBitmap)
                            } else {
                                Toast.makeText(context, "PixelCopy error: $result", Toast.LENGTH_SHORT).show()
                            }
                        }, handler)
                    } else {
                        Toast.makeText(context, "PixelCopy wymaga Androida 7.0+", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Camera, contentDescription = "Zrób zdjęcie")
                }

                FloatingActionButton(onClick = {
                    Toast.makeText(context, "Nagrywanie wideo wyłączone", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Nagrywanie wyłączone")
                }
            }
        }
    }
}
