# RetroCamera 📸

RetroCamera to aplikacja na Androida inspirowana klasycznymi aparatami fotograficznymi. Umożliwia robienie zdjęć z efektami w stylu retro, filtrowanie ich oraz przeglądanie wykonanych zdjęć w galerii.

## Funkcje
- Logowanie do konta Google 
- Obsługa aparatu w czasie rzeczywistym (CameraX)
- Nakładanie filtrów w stylu retro tryby:  thermal, grayscale, oldfilm, vhs, 8mm, vintage, gameboy, 8bit, sobel, glitch, neon
- Zapisywanie zdjęć do pamięci urządzenia
- Przeglądanie wykonanych zdjęć w galerii
- Usuwanie wybranego zdjęcia
- Sychronizacja zdjęcia do Google Photos

## Główne komponenty aplikacji
 # MainActivity.kt
- Uruchamia aplikację.
- Konfiguruje nawigację między ekranem kamery a galerią.
- Obsługuje logowanie przez Google (OAuth2) i zapisuje token dostępu.
- Przechowuje token globalnie w AuthSession.accessToken.

  # CameraManager.kt
- Wyświetla kamerę z aktywnym shaderem (filtr graficzny).
- Obsługuje robienie zdjęcia (przez PixelCopy) i zapisuje je w galerii (MediaStore).
- Tworzy przyciski UI do robienia zdjęć i otwierania galerii.

 # CameraScreen.kt
- Łączy kamerę z widokiem OpenGL (GLSurfaceView) i nakłada wybrany filtr.
- Renderuje CameraShaderRenderer w czasie rzeczywistym.
- Obsługuje wybór filtra z dropdown menu (ShaderFilterDropdown).

 # CameraViewModel.kt
- Przechowuje aktualny stan renderera, tekstury oraz wybrany filtr.
- Ułatwia komunikację między UI a logiką renderowania kamery.

 # Shader.kt
- Główna klasa CameraShaderRenderer odpowiada za rysowanie obrazu kamery z nałożonym shaderem (OpenGL).
Obsługuje:
- Tworzenie tekstury i SurfaceTexture.
- Aktualizację filtra.
- Rysowanie obrazu z shaderem w czasie rzeczywistym.
- Robienie zdjęć (kopiowanie obrazu z GPU do bitmapy).
- Zawiera implementacje działania filtrów
 # GalleryScreen.kt
- Wyświetla ekran galerii z miniaturami w siatce (3 kolumny).
- Obsługuje przełączanie się między trybem przeglądania a szczegółowym widokiem zdjęcia.
- Integruje z GalleryViewModel do operacji na zdjęciach.

 # GalleryElement.kt
Widok szczegółowy zdjęcia. Umożliwia:
- Synchronizację zdjęcia z Google Photos.
- Usunięcie zdjęcia.
- Umożliwia przewijanie zdjęć w galerii.

 # GalleryView.kt
Przechowuje listę zdjęć.
- Ładuje zdjęcia z folderu Pictures/RetroCamera.
Obsługuje:
- Usuwanie zdjęć.
- Wysyłanie zdjęć do Google Photos przez GooglePhotosApiClient.

# API i autoryzacja do Google
- Klient API do Google Photos (GooglePhotosApiClient).
- Funkcja uploadImageToGooglePhotos(uri):
- Wysyła zdjęcie jako binarkę.
- Tworzy element w bibliotece Google Photos.

Funkcja exchangeAuthCodeForAccessToken():
- Wymienia authCode na token OAuth2 (dostępowy).
- Dane uwierzytelniające (clientId, clientSecret) pobierane z BuildConfig.
  W pliku gradle.properties należy uzupełnić pola:
  GOOGLE_CLIENT_ID=""
  GOOGLE_CLIENT_SECRET=""
  Może użyć do tego Google Cloud Console

# Współdzielone dane
- AuthSession (w MainActivity.kt)
- Globalna zmienna przechowująca token dostępu OAuth2 (accessToken).
