package com.example.retrocamera.galeria

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryElement(
    images: List<Uri>, // lista uri zdjęc w galerii
    startIndex: Int, // indeks pierwszego zdjecia
    onClose: () -> Unit, // funkcja przy zamknęciu podglądu zdj
    onDeleteClick: (Uri) -> Unit, // funckja przy usuwaniu zdjecia
    onSyncClick: (Uri) -> Unit // funckcja wywołania API
) {

    //zapamietanie indexu zdjecia
    val pagerState = rememberPagerState(initialPage = startIndex)
    //layout na fullscreen
    Box(modifier = Modifier.fillMaxSize()) {
        // możliwośc przewijania galerii
        HorizontalPager(
            count = images.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val uri = images[page]
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        // górny topappbar do powórtu do galerii
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
                .height(56.dp)
                .align(Alignment.TopStart)
        )
        // zmienne odnośnie orientacji ekranu
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        // dolny appbar do schronizacji zdjecia z google photos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (isPortrait) 64.dp else 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentUri = images[pagerState.currentPage]
            Button(onClick = { onSyncClick(currentUri) }) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synchronizuj")
            }
            Button(onClick = { onDeleteClick(currentUri) }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usuń zdjęcie")
            }
        }
    }
}
