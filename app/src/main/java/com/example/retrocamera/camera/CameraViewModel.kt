package com.example.retrocamera.camera
import android.content.Context
import android.graphics.SurfaceTexture
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.example.retrocamera.filters.CameraShaderRenderer

//logika viewModel, przechowyanie stanu shdera, przechowywanie tekstury
class CameraViewModel : ViewModel() {
    val shaderRenderer = mutableStateOf<CameraShaderRenderer?>(null)
    val surfaceTexture = mutableStateOf<SurfaceTexture?>(null)

    //domyślny filtr
    val selectedFilter = mutableStateOf("Normal")


    //Tworzy nową teksturę kamery (cameraTextureId)
    //Tworzy nowy CameraShaderRenderer i przekazuje mu: context ID tekstury
    //surfaceTexture – obiekt, do którego będzie rysować
    //selectedFilter – filtr, który będzie stosowany
    //Przechowuje renderer w stanie ViewModelu
    //Zwraca go do GLSurfaceView do rysowania
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