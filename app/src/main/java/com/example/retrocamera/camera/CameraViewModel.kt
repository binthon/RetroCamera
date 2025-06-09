package com.example.retrocamera.camera
import android.content.Context
import android.graphics.SurfaceTexture
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.example.retrocamera.filters.CameraShaderRenderer

class CameraViewModel : ViewModel() {
    val shaderRenderer = mutableStateOf<CameraShaderRenderer?>(null)
    val surfaceTexture = mutableStateOf<SurfaceTexture?>(null)

    val selectedFilter = mutableStateOf("Normal")

    fun initRenderer(context: Context): CameraShaderRenderer {
        val cameraTextureId = IntArray(1)
        val renderer = CameraShaderRenderer(
            context = context,
            cameraTextureId = cameraTextureId,
            surfaceTexture = surfaceTexture,
            selectedFilter = selectedFilter
        )
        shaderRenderer.value = renderer
        return renderer
    }

}