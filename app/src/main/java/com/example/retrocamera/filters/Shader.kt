package com.example.retrocamera.filters

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.graphics.Matrix
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//CameraX → SurfaceTexture → CameraShaderRenderer (OpenGL) → ekran GLSurfaceView
//                             ↑
//                  wybrany shader (GLSL) z ViewModelu


class CameraShaderRenderer(
    private val context: android.content.Context,
    private val cameraTextureId: IntArray,
    private val surfaceTexture: MutableState<SurfaceTexture?>,
    private val selectedFilter: State<String>
) : GLSurfaceView.Renderer {

    private var programHandle: Int = 0
    private var lastAppliedFilter: String = selectedFilter.value
    private var width: Int = 0
    private var height: Int = 0

    private var captureRequested: Boolean = false
    private var captureCallback: ((Bitmap) -> Unit)? = null

    // tworzenie tekstury GL z obrazem z kamery, kompilacja shaderu
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glGenTextures(1, cameraTextureId, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        surfaceTexture.value = SurfaceTexture(cameraTextureId[0]).apply {
            setDefaultBufferSize(1920, 1080)
        }

        programHandle = compileShaderProgram(selectedFilter.value)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        this.width = width
        this.height = height
    }
    // wykonyanie co klatke nakładania filtru
    override fun onDrawFrame(gl: GL10?) {
        // aktualizacja obrazu z tekstu, jakby pobieranie tego co na kamerze
        surfaceTexture.value?.updateTexImage()

        //zmiana shadera jeśli wybrano inny filtr
        if (lastAppliedFilter != selectedFilter.value) {
            GLES20.glDeleteProgram(programHandle)
            programHandle = compileShaderProgram(selectedFilter.value)
            lastAppliedFilter = selectedFilter.value
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(programHandle)
        //wierzchołki pozycji nakładanego filtru
        val vertexCoords = floatArrayOf(
            -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f
        )

        val texCoords = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )

        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexCoords)
                position(0)
            }

        val texBuffer = java.nio.ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(texCoords)
                position(0)
            }
        //aPosition – pozycje wierzchołków (x, y)
        //aTexCoord – UV tekstury (mapowanie obrazu)
        //uTexture – tekstura kamery
        val aPosition = GLES20.glGetAttribLocation(programHandle, "aPosition")
        val aTexCoord = GLES20.glGetAttribLocation(programHandle, "aTexCoord")
        val uTexture = GLES20.glGetUniformLocation(programHandle, "uTexture")


        //przekazywanie bufowa filtry do GPU
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        //aktywowanie tekstury filtru
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId[0])
        GLES20.glUniform1i(uTexture, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)


        //kopiowanie obrazu z buforwa OpenGL do bitmapy
        if (captureRequested) {
            val buffer = IntArray(width * height)
            val intBuffer = IntBuffer.wrap(buffer)
            intBuffer.position(0)

            GLES20.glFinish()
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(buffer, 0, width, 0, 0, width, height)

            val flippedBitmap = Matrix().let { matrix ->
                matrix.preScale(1f, -1f)
                Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
            }

            captureCallback?.invoke(flippedBitmap)
            captureRequested = false
        }
    }

    private fun compileShaderProgram(filter: String): Int {
        //próba wyłapania orientacji telefonu
        val rotation = (context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        // rózne ustawienia wierzchołków dla pionu i poziomy telefonu w celu dostoswania filtrów do obrazu z kamery

        //shadery działają na podstawie obliczania luminacji kazdego pixel i zmieny tej wartości
        val vertexShaderCode = if (rotation == Surface.ROTATION_0) {
            """
                attribute vec4 aPosition;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main() {
                    gl_Position = aPosition;
                    vTexCoord = vec2(aTexCoord.y, 1.0 - aTexCoord.x);
                }
            """
        } else {
            """
                attribute vec4 aPosition;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main() {
                    gl_Position = aPosition;
                    vTexCoord = aTexCoord;
                }
            """
        }.trimIndent()
        // trimIndent pozwala na usunęcie zbędnych wcięć 


        val fragmentShaderCode = when (filter) {
            "Thermal" -> """
                    #extension GL_OES_EGL_image_external : require 
                    precision mediump float;
                    uniform samplerExternalOES uTexture;
                    varying vec2 vTexCoord;
                
                    void main() {
                        vec4 color = texture2D(uTexture, vTexCoord);
                        float intensity = dot(color.rgb, vec3(0.299, 0.587, 0.114)); // luminancja
                
                        vec3 mapped;
                        if (intensity < 0.2) {
                            mapped = vec3(0.0, 0.0, intensity * 5.0);
                        } else if (intensity < 0.4) {
                            mapped = vec3(0.0, (intensity - 0.2) * 5.0, 1.0);
                        } else if (intensity < 0.6) {
                            mapped = vec3((intensity - 0.4) * 5.0, 1.0, 1.0 - (intensity - 0.4) * 5.0);
                        } else if (intensity < 0.8) {
                            mapped = vec3(1.0, 1.0 - (intensity - 0.6) * 5.0, 0.0);
                        } else {
                            mapped = vec3(1.0 - (intensity - 0.8) * 5.0, 0.0, 0.0);
                        }
                
                        gl_FragColor = vec4(mapped, 1.0);
                    }
                """.trimIndent()


            "Grayscale" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
                void main() {
                    vec4 c = texture2D(uTexture, vTexCoord);
                    float gray = (c.r + c.g + c.b) / 3.0;
                    gl_FragColor = vec4(gray, gray, gray, c.a);
                }
            """.trimIndent()

            "OldFilm" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
            
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                float rand(vec2 co) {
                    return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
                }
            
                void main() {
                    vec4 color = texture2D(uTexture, vTexCoord);
            
                    // Sepia tone (mocniejszy)
                    float r = color.r;
                    float g = color.g;
                    float b = color.b;
                    color.r = min(dot(vec3(r, g, b), vec3(0.6, 0.45, 0.2)), 1.0);
                    color.g = min(dot(vec3(r, g, b), vec3(0.4, 0.35, 0.2)), 1.0);
                    color.b = min(dot(vec3(r, g, b), vec3(0.3, 0.3, 0.15)), 1.0);
            
                    // Flicker (widoczne migotanie)
                    float flicker = 0.05 * sin(gl_FragCoord.y * 10.0 + mod(gl_FragCoord.x, 10.0));
                    color.rgb += flicker;
            
                    // Stronger grain
                    float grain = rand(gl_FragCoord.xy) * 0.2;
                    color.rgb += vec3(grain);
            
                    // Vertical scratches (dużo gęstsze)
                    float scratch = step(0.97, rand(vec2(vTexCoord.x * 200.0, mod(vTexCoord.y * 200.0, 10.0))));
                    color.rgb += vec3(scratch * 0.3);
            
                    // Clamp to 0-1
                    gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), 1.0);
                }
            """.trimIndent()

            "VHS" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
            
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                float rand(vec2 co) {
                    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
                }
            
                void main() {
                    vec2 uv = vTexCoord;
            
                    // Subtle horizontal shake (jitter)
                    uv.x += sin(uv.y * 1200.0 + fract(uv.y * 0.5) * 20.0) * 0.002;
            
                    // RGB split
                    float offset = 0.0015;
                    vec4 colR = texture2D(uTexture, uv + vec2(offset, 0.0));
                    vec4 colG = texture2D(uTexture, uv);
                    vec4 colB = texture2D(uTexture, uv - vec2(offset, 0.0));
                    vec4 color = vec4(colR.r, colG.g, colB.b, 1.0);
            
                    // Add scanlines
                    float scanline = 0.04 * sin(uv.y * 800.0);
                    color.rgb -= scanline;
            
                    // Add grain/noise
                    float noise = rand(gl_FragCoord.xy * fract(uv.y * 1000.0)) * 0.05;
                    color.rgb += noise;
            
                    // Slight desaturation
                    float gray = dot(color.rgb, vec3(0.3, 0.59, 0.11));
                    color.rgb = mix(vec3(gray), color.rgb, 0.85);
            
                    gl_FragColor = color;
                }
            """.trimIndent()

            "8mm" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
            
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                float rand(vec2 co) {
                    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
                }
            
                void main() {
                    vec2 uv = vTexCoord;
            
                    // Subtle distortion for frame jitter
                    uv.y += sin(uv.x * 20.0 + rand(uv) * 5.0) * 0.003;
            
                    // Sample color
                    vec4 color = texture2D(uTexture, uv);
            
                    // Warm color tone
                    color.rgb *= vec3(1.1, 0.95, 0.85);
            
                    // Vignette
                    float vignette = smoothstep(0.8, 0.3, distance(uv, vec2(0.5)));
                    color.rgb *= vignette;
            
                    // Flicker
                    float flicker = 0.05 * sin(gl_FragCoord.y * 10.0 + gl_FragCoord.x);
                    color.rgb += flicker;
            
                    // Grain
                    float grain = rand(gl_FragCoord.xy) * 0.15;
                    color.rgb += grain;
            
                    // Clamp
                    gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
                }
            """.trimIndent()

            "Vintage" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
                void main() {
                    vec4 c = texture2D(uTexture, vTexCoord);
                    float r = c.r, g = c.g, b = c.b;
                    vec3 sepia = vec3(
                        r * 0.393 + g * 0.769 + b * 0.189,
                        r * 0.349 + g * 0.686 + b * 0.168,
                        r * 0.272 + g * 0.534 + b * 0.131
                    );
                    float vignette = smoothstep(0.8, 0.4, distance(vTexCoord, vec2(0.5)));
                    gl_FragColor = vec4(sepia * vignette, c.a);
                }
            """.trimIndent()

            "GameBoy" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
            
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                void main() {
                    vec4 color = texture2D(uTexture, vTexCoord);
                    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            
                    // Smooth 8-level brightness steps
                    float level = floor(gray * 8.0) / 8.0;
            
                    // Soft green tint
                    vec3 green = vec3(0.3, 0.55, 0.3);
                    gl_FragColor = vec4(green * level, 1.0);
                }
            """.trimIndent()


            "8bit" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
            
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                // Posterize pomocnicza funkcja
                vec3 posterize(vec3 color, float levels) {
                    return floor(color * levels) / levels;
                }
            
                void main() {
                    vec4 texColor = texture2D(uTexture, vTexCoord);
            
                    // Posterizacja — redukcja liczby kolorów
                    vec3 reduced = posterize(texColor.rgb, 6.0); // tylko 4 poziomy na kanał (czyli 64 kolory)
            
                    // Dithering (symulacja ditheringu Bayera)
                    float grid = mod(gl_FragCoord.x + gl_FragCoord.y, 2.0);
                    reduced += grid * 0.02;
            
                    gl_FragColor = vec4(clamp(reduced, 0.0, 1.0), texColor.a);
                }
            """.trimIndent()
            "Sobel" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                void main() {
                    float dx = 1.0 / 512.0; // dostosuj do rozdzielczości tekstury
                    float dy = 1.0 / 512.0;
            
                    vec3 tc0 = texture2D(uTexture, vTexCoord + vec2(-dx, -dy)).rgb;
                    vec3 tc1 = texture2D(uTexture, vTexCoord + vec2( 0.0, -dy)).rgb;
                    vec3 tc2 = texture2D(uTexture, vTexCoord + vec2( dx, -dy)).rgb;
                    vec3 tc3 = texture2D(uTexture, vTexCoord + vec2(-dx,  0.0)).rgb;
                    vec3 tc4 = texture2D(uTexture, vTexCoord + vec2( dx,  0.0)).rgb;
                    vec3 tc5 = texture2D(uTexture, vTexCoord + vec2(-dx,  dy)).rgb;
                    vec3 tc6 = texture2D(uTexture, vTexCoord + vec2( 0.0,  dy)).rgb;
                    vec3 tc7 = texture2D(uTexture, vTexCoord + vec2( dx,  dy)).rgb;
            
                    vec3 sobelX = -tc0 - 2.0 * tc3 - tc5 + tc2 + 2.0 * tc4 + tc7;
                    vec3 sobelY = -tc0 - 2.0 * tc1 - tc2 + tc5 + 2.0 * tc6 + tc7;
            
                    float edge = length(sobelX + sobelY);
                    gl_FragColor = vec4(vec3(edge), 1.0);
                }
            """.trimIndent()
            "Glitch" -> """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;
        
            float rand(vec2 co) {
                return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453);
            }
        
            void main() {
                vec2 uv = vTexCoord;
                uv.x += sin(uv.y * 50.0) * 0.01 * rand(uv);
        
                vec4 color = texture2D(uTexture, uv);
                float noise = rand(gl_FragCoord.xy) * 0.1;
                gl_FragColor = vec4(clamp(color.rgb + noise, 0.0, 1.0), color.a);
            }
        """.trimIndent()
            "Neon" -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
            
                void main() {
                    vec4 texColor = texture2D(uTexture, vTexCoord);
            
                    // Vaporwave gradient overlay (cyan to magenta)
                    float gradient = sin(vTexCoord.y * 10.0) * 0.5 + 0.5;
                    vec3 vaporColor = mix(vec3(0.0, 1.0, 1.0), vec3(1.0, 0.0, 1.0), gradient);
            
                    // Mix original with vaporwave overlay
                    vec3 finalColor = mix(texColor.rgb, vaporColor, 0.25); // 0.25 = 25% tint
            
                    gl_FragColor = vec4(finalColor, texColor.a);
                }
            """.trimIndent()
            else -> """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTexCoord;
                void main() {
                    gl_FragColor = texture2D(uTexture, vTexCoord);
                }
            """.trimIndent()

        }
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        return program
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

}
