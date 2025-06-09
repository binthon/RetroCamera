package com.example.retrocamera.galeria

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrocamera.api.GooglePhotosApiClient
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(private val context: Context) : ViewModel()
{
    val images = mutableStateListOf<Uri>()

    init {
        loadImages(context)
    }
    fun deleteImage(uri: Uri) {
        val file = File(uri.path ?: return)
        if (file.exists()) {
            file.delete()
            images.remove(uri)
        }
    }

    fun uploadToGooglePhotos(uri: Uri) {
        viewModelScope.launch {
            val uploader = GooglePhotosApiClient(context)
            val success = uploader.uploadImageToGooglePhotos(uri)
            Log.d("Upload", if (success) "Upload OK" else "Upload FAILED")

        }
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
