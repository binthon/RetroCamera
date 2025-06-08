package com.example.retrocamera.camera
import android.content.Context
import android.graphics.SurfaceTexture
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.example.retrocamera.filters.CameraShaderRenderer

class CameraViewModel : ViewModel() {
    val shaderRenderer = mutableStateOf<CameraShaderRenderer?>(null)
    val surfaceTexture = mutableStateOf<SurfaceTexture?>(null)

    fun initRenderer(context: Context): CameraShaderRenderer {
        val cameraTextureId = IntArray(1)
        val renderer = CameraShaderRenderer(
            context = context,
            cameraTextureId = cameraTextureId,
            surfaceTexture = surfaceTexture,
            selectedFilter = mutableStateOf("Normal")
        )
        shaderRenderer.value = renderer
        return renderer
    }
}