package com.example.retrocamera.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.retrocamera.filters.CameraShaderRenderer
import java.util.concurrent.Executors

@Composable
fun CameraShaderScreen(
    shaderRendererSetter: (CameraShaderRenderer) -> Unit,
    surfaceViewSetter: (android.view.SurfaceView) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = context as LifecycleOwner

    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        if (!hasPermission.value) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 123)
        }
    }

    if (!hasPermission.value) {
        Text("Brak dostępu do kamery", color = Color.White)
        return
    }

    val selectedFilter = rememberSaveable { mutableStateOf("Normal") }
    val cameraTextureId = remember { IntArray(1) }
    val surfaceTexture = remember { mutableStateOf<SurfaceTexture?>(null) }

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            val renderer = CameraShaderRenderer(
                context = context,
                cameraTextureId = cameraTextureId,
                surfaceTexture = surfaceTexture,
                selectedFilter = selectedFilter
            )
            shaderRendererSetter(renderer)
            surfaceViewSetter(this)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    LaunchedEffect(surfaceTexture.value) {
        surfaceTexture.value?.let { tex ->
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder()
                .setTargetResolution(Size(720, 1280))
                .build()

            val surface = Surface(tex)
            preview.setSurfaceProvider { request ->
                request.provideSurface(surface, Executors.newSingleThreadExecutor()) {}
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { glSurfaceView }, modifier = Modifier.fillMaxSize())
        ShaderFilterDropdown(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) { selectedFilter.value = it }
    }
}


@Composable
fun ShaderFilterDropdown(
    modifier: Modifier = Modifier,
    onFilterSelected: (String) -> Unit
) {
    val options = listOf("Normal", "Sepia", "Grayscale", "OldFilm", "VHS", "8mm", "Vintage", "GameBoy", "8bit")
    var expanded by remember { mutableStateOf(false) }
    var selectedLabel by rememberSaveable { mutableStateOf("Normal") }

    Box(modifier) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .width(150.dp)
                .background(Color(0xFF4A148C), shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Column {
                Text("Filtr", color = Color.White)

                Divider(
                    color = Color.White.copy(alpha = 0.6f),
                    thickness = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 6.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { expanded = true }
                ) {
                    Text(
                        text = selectedLabel,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFFEDE7F6))
        ) {
            options.forEach { label ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            color = Color(0xFF4A148C)
                        )
                    },
                    onClick = {
                        selectedLabel = label
                        expanded = false
                        onFilterSelected(label)
                    }
                )
            }
        }
    }

}
