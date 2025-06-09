package com.example.retrocamera.api

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.retrocamera.AuthSession
import com.example.retrocamera.AuthSession.accessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import com.example.retrocamera.BuildConfig
//pobranie stałych z gradle.properties, tak żeby klucze były niewidoczne
val clientId = BuildConfig.GOOGLE_CLIENT_ID
val clientSecret = BuildConfig.GOOGLE_CLIENT_SECRET
class GooglePhotosApiClient(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun uploadImageToGooglePhotos(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // sprawdzenie token
            if (accessToken == null) {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                val authCode = account?.serverAuthCode
                accessToken = authCode?.let { exchangeAuthCodeForAccessToken(context, it) }
            }
            // wspólny dla całej apki
            val token = AuthSession.accessToken ?: return@withContext false

            val file = File(uri.path ?: return@withContext false)
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)

            // Krok 1: Upload w binarce
            val uploadRequest = Request.Builder()
                .url("https://photoslibrary.googleapis.com/v1/uploads")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-type", "application/octet-stream")
                .addHeader("X-Goog-Upload-File-Name", file.name)
                .addHeader("X-Goog-Upload-Protocol", "raw")
                .post(requestBody)
                .build()

            val uploadResponse = client.newCall(uploadRequest).execute()
            val uploadToken = uploadResponse.body?.string()?.trim()

            if (!uploadResponse.isSuccessful || uploadToken.isNullOrEmpty()) {
                Log.e("UPLOAD", "Upload failed: ${uploadResponse.code} | Token: $uploadToken")
                return@withContext false
            }

            // Krok 2: Dodaj zdjęcie do biblioteki
            val json = """
            {
              "newMediaItems": [
                {
                  "description": "${file.name}",
                  "simpleMediaItem": {
                    "uploadToken": "$uploadToken"
                  }
                }
              ]
            }
        """.trimIndent()

            val createRequest = Request.Builder()
                .url("https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate")
                .addHeader("Authorization", "Bearer $token")
                .post(okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), json))
                .build()

            val createResponse = client.newCall(createRequest).execute()
            val createBody = createResponse.body?.string()

            if (!createResponse.isSuccessful) {
                Log.e("UPLOAD", "Create failed: ${createResponse.code} | $createBody")
            }

            return@withContext createResponse.isSuccessful
        } catch (e: Exception) {
            Log.e("UPLOAD", "Exception: ${e.message}", e)
            false
        }
    }

}
// reguest do otrzymania tokenu do wrzucenia zdjecia,
fun exchangeAuthCodeForAccessToken(context: Context, authCode: String): String? {
    return try {

        val formBody = okhttp3.FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
            .add("code", authCode)
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(formBody)
            .build()

        val response = OkHttpClient().newCall(request).execute()
        val body = response.body?.string()

        if (response.isSuccessful && body != null) {
            val token = JSONObject(body).getString("access_token")
            Log.d("AUTH", "access_token: $token")
            token
        } else {
            Log.e("AUTH", "Token exchange failed: $body")
            null
        }
    } catch (e: Exception) {
        Log.e("AUTH", "Token exchange error: ${e.message}", e)
        null
    }
}
