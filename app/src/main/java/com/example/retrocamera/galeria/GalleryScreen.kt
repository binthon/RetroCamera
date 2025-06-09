package com.example.retrocamera.galeria

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.rememberAsyncImagePainter
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: GalleryViewModel, onBackToCamera: () -> Unit) {
    var selectedImage by rememberSaveable { mutableStateOf<Uri?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedImage == null) "Twoja Galeria" else "Podgląd zdjęcia"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedImage != null) {
                            // Wracamy do widoku galerii
                            selectedImage = null
                        } else {
                            // Wracamy do kamery
                            onBackToCamera()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        selectedImage?.let {
            GalleryElement(
                uri = it,
                onClose = { selectedImage = null },
                onDeleteClick = {
                    viewModel.deleteImage(it)
                    selectedImage = null
                },
                onSyncClick = {
                    viewModel.uploadToGooglePhotos(it)
                }
            )

            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.images) { uri ->
                val painter: Painter = rememberAsyncImagePainter(model = uri)
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clickable { selectedImage = uri }
                )
            }
        }
    }
}


