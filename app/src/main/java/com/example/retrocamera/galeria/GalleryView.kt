package com.example.retrocamera.galeria

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.io.File

class GalleryViewModel(context: Context) : ViewModel() {
    val images = mutableStateListOf<Uri>()

    init {
        loadImages(context)
    }

    private fun loadImages(context: Context) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "RetroCamera")
        if (dir.exists()) {
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.forEach {
                images.add(Uri.fromFile(it))
            }
        }
    }
}
