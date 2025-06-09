package com.example.retrocamera.galeria

import android.net.Uri
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryElement(
    uri: Uri,
    onClose: () -> Unit,
    onDeleteClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .align(Alignment.TopStart)
        ) {
            TopAppBar(
                title = { Text("Podgląd zdjęcia", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 48.dp else 64.dp)
            )
        }

        val bottomPadding = if (isLandscape) 16.dp else 64.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding)
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onSyncClick) {
                Icon(Icons.Default.Sync, contentDescription = "Synchronizuj")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synchronizuj")
            }
            Button(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń zdjęcie")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usuń zdjęcie")
            }
        }

    }
}