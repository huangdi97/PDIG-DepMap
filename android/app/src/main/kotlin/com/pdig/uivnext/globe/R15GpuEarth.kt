package com.pdig.uivnext.globe

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI

/**
 * R15 - independent GPU world renderer, replacing the per-frame CPU Bitmap
 * projection for the Preview hero. The screen uses ONLY locally bundled NASA
 * textures. No external tiles, advertising, tracking or invented region links.
 *
 * The fragment shader evaluates the actual visible sphere in camera space
 * (including spherical normal, specular ocean, night lights, translucent clouds,
 * blue atmospheric Fresnel and a separate exterior atmosphere).
 */
internal fun supportsR15GpuEarth(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return (manager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0) >= 0x20000
}

internal class R15GpuEarthView(
    context: Context,
    onFailure: () -> Unit,
    onReady: () -> Unit,
) : GLSurfaceView(context) {
    private val worldRenderer = R15GpuEarthRenderer(
        context.applicationContext,
        failure = { post { onFailure() } },
        ready = { post { onReady() } },
    )

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderMediaOverlay(true)
        preserveEGLContextOnPause = true
        setRenderer(worldRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun showCamera(camera: GlobeCamera) {
        queueEvent { worldRenderer.camera = camera }
        requestRender()
    }
}

private class R15GpuEarthRenderer(
    private val context: Context,
    private val failure: () -> Unit,
    private val ready: () -> Unit,
) : GLSurfaceView.Renderer {
    @Volatile var camera: GlobeCamera = focusCamera(16f, 107f)
    private var width = 1
    private var height = 1
    private var program = 0
    private var textures = IntArray(3)
    private val quad: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0) }
    private var ready = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            program = linkProgram(VERTEX, FRAGMENT)
            GLES20.glGenTextures(3, textures, 0)
            uploadTexture(0, "earth/earth_albedo_2048.png")
            uploadTexture(1, "earth/earth_night_lights_2048.png")
            uploadTexture(2, "earth/cloud_2048.png")
            ready = true
            Log.i("PDIG_R15", "R15_GPU_TEXTURES_READY")
            ready()
        } catch (t: Throwable) {
            ready = false
            Log.e("PDIG_R15", "R15_GPU_INIT_FAILED", t)
            failure()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0.94f, 0.977f, 1f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (!ready) return
        GLES20.glUseProgram(program)
        val coord = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(coord)
        quad.position(0)
        GLES20.glVertexAttribPointer(coord, 2, GLES20.GL_FLOAT, false, 0, quad)
        val cam = camera
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uViewport"), width.toFloat(), height.toFloat())
        GLES20.glUniform3f(
            GLES20.glGetUniformLocation(program, "uCamera"),
            cam.yawDeg * PI.toFloat() / 180f,
            cam.pitchDeg * PI.toFloat() / 180f,
            cam.zoom,
        )
        for (i in 0 until 3) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])
            GLES20.glUniform1i(
                GLES20.glGetUniformLocation(program, arrayOf("uAlbedo", "uNight", "uCloud")[i]), i,
            )
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(coord)
    }

    private fun uploadTexture(slot: Int, filename: String) {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 2
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val image = context.assets.open(filename).use { input ->
            BitmapFactory.decodeStream(input, null, options)
                ?: throw IllegalStateException("Unusable bundled Earth texture: " + filename)
        }
        try {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[slot])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0)
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                throw IllegalStateException("OpenGL texture upload failed: " + filename + " / " + error)
            }
        } finally {
            image.recycle()
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val result = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, result, 0)
        if (result[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Shader compile failed: " + error)
        }
        return shader
    }

    private fun linkProgram(vertex: String, fragment: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
        val id = GLES20.glCreateProgram()
        GLES20.glAttachShader(id, v)
        GLES20.glAttachShader(id, f)
        GLES20.glLinkProgram(id)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw IllegalStateException("Shader link failed: " + GLES20.glGetProgramInfoLog(id))
        GLES20.glDeleteShader(v)
        GLES20.glDeleteShader(f)
        return id
    }
}

private const val VERTEX = """
attribute vec2 aPosition;
void main() { gl_Position = vec4(aPosition, 0.0, 1.0); }
"""

private const val FRAGMENT = """
precision highp float;
uniform vec2 uViewport;
uniform vec3 uCamera; // yaw radians, pitch radians, zoom multiplier
uniform sampler2D uAlbedo;
uniform sampler2D uNight;
uniform sampler2D uCloud;
const float PI = 3.141592653589793;
void main() {
    vec2 uvScreen = gl_FragCoord.xy / uViewport;
    vec3 sky = mix(vec3(0.90, 0.960, 1.00), vec3(0.976, 0.993, 1.00),
                   smoothstep(0.0, 1.0, uvScreen.y));
    vec2 pixel = gl_FragCoord.xy - uViewport * 0.5;
    float rPx = min(uViewport.x * 0.42, uViewport.y * 0.47) * clamp(uCamera.z, 0.70, 1.9);
    vec2 p = pixel / rPx;
    float r2 = dot(p, p);
    float radius = sqrt(r2);
    // Exterior layered optical glow, intentionally separate from surface albedo.
    float auraWide = exp(-pow((radius - 1.02) / 0.25, 2.0));
    float auraSharp = exp(-pow((radius - 1.005) / 0.052, 2.0));
    vec3 skyOut = mix(sky, vec3(0.23, 0.68, 1.0),
                       clamp(auraWide * 0.24 + auraSharp * 0.37, 0.0, 0.65));
    if (r2 > 1.0) {
        gl_FragColor = vec4(skyOut, 1.0);
        return;
    }
    float z = sqrt(max(0.0, 1.0-r2));
    vec3 normal = vec3(p, z);
    float cy = cos(uCamera.x); float sy = sin(uCamera.x);
    float cp = cos(uCamera.y); float sp = sin(uCamera.y);
    // Inverse of Kotlin rotateX(rotateY(world,yaw),pitch).
    vec3 unpitched = vec3(normal.x, cp*normal.y + sp*normal.z,
                        -sp*normal.y + cp*normal.z);
    vec3 world = normalize(vec3(cy*unpitched.x - sy*unpitched.z, unpitched.y,
                                sy*unpitched.x + cy*unpitched.z));
    float longitude = atan(world.z, world.x);
    float latitude = asin(clamp(world.y, -1.0, 1.0));
    vec2 uv = vec2((longitude / PI + 1.0)*0.5, 0.5 - latitude/PI);
    vec3 earth = texture2D(uAlbedo, uv).rgb;
    vec3 cloud = texture2D(uCloud, uv).rgb;
    vec3 lights = texture2D(uNight, uv).rgb;
    vec3 sun = normalize(vec3(-0.029, 0.309, 0.950));
    float day = smoothstep(-0.31, 0.44, dot(world, sun));
    // Reference photography: allow rich lit land and ocean, rather than
    // clamping the globe to the flat dark "space" source styling.
    earth *= 0.61 + 0.55 * day;
    float ocean = smoothstep(0.04, 0.19, earth.b-earth.r);
    vec3 reflected = normalize(sun + world);
    float oceanGlint = ocean * pow(max(dot(world, reflected),0.0), 32.0) * 0.24 * day;
    earth = mix(earth, vec3(0.97,0.99,1.0), oceanGlint);
    earth = mix(earth, vec3(0.93,0.97,1.0), cloud.r * 0.24);
    earth += lights * pow(1.0-day, 2.3)*0.42;
    // Atmospheric Rayleigh-inspired (art-directed) rim.
    float fresnel = pow(1.0-z, 2.7);
    earth = mix(earth, vec3(0.30,0.68,1.0), clamp(fresnel*0.68, 0.0, 0.8));
    earth += vec3(0.11,0.26,0.42)*pow(1.0-z, 7.0);
    earth = mix(earth, vec3(0.66,0.83,1.0), 0.075);
    gl_FragColor = vec4(clamp(earth, 0.0, 1.0), 1.0);
}
