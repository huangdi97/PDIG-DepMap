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
    private var initialized = false
    /** True only after the first *drawn* frame without a GL error. Texture upload
     * alone is not an on-screen ready signal. Reset on EGL context recreation. */
    private var firstFrameReported = false
    private var firstFrameFailed = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        firstFrameReported = false
        firstFrameFailed = false
        initialized = false
        try {
            program = linkProgram(VERTEX, FRAGMENT)
            GLES20.glGenTextures(3, textures, 0)
            uploadTexture(0, "earth/earth_albedo_2048.png")
            uploadTexture(1, "earth/earth_night_lights_2048.png")
            uploadTexture(2, "earth/cloud_2048.png")
            initialized = true
            Log.i("PDIG_R15", "R15_GPU_TEXTURES_READY_AWAITING_FIRST_FRAME")
        } catch (t: Throwable) {
            initialized = false
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
        if (!initialized) return
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
        if (!firstFrameReported && !firstFrameFailed) {
            val error = GLES20.glGetError()
            if (error == GLES20.GL_NO_ERROR) {
                firstFrameReported = true
                Log.i("PDIG_R15", "R15_GPU_FIRST_FRAME_DRAWN")
                ready()
            } else {
                firstFrameFailed = true
                initialized = false
                Log.e("PDIG_R15", "R15_GPU_FIRST_FRAME_FAILED glError=" + error)
                failure()
            }
        }
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
    // A soft wide scattering halo merges sky and globe into one visual plane.
    // The thin outer optical ring is atmospheric artwork, never a dependency.
    float orbitHaze = exp(-pow((radius - 1.20) / 0.15, 2.0));
    vec3 skyOut = mix(sky, vec3(0.19, 0.65, 0.99),
                       clamp(auraWide * 0.30 + auraSharp * 0.43 + orbitHaze * 0.08, 0.0, 0.72));
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
    // Rich light-first albedo without flattening the actual photographed
    // continents. The night hemisphere remains visibly distinct.
    float ocean = smoothstep(0.018, 0.18, earth.b - earth.r);
    earth *= 0.74 + 0.48 * day;
    earth = mix(earth, earth * vec3(0.84, 1.05, 1.20), ocean * 0.29);

    // Proper world-space view/sun half vector: unlike normalize(sun + world),
    // this produces a bounded reflective highlight on the real ocean surface.
    vec3 viewWorld = normalize(vec3(-sy*cp, sp, cy*cp));
    vec3 halfLight = normalize(sun + viewWorld);
    float oceanGlint = ocean * pow(max(dot(world, halfLight), 0.0), 58.0)
                       * 0.66 * day;
    earth = mix(earth, vec3(0.95, 0.99, 1.0), clamp(oceanGlint, 0.0, 0.72));

    // Existing offline cloud / night light textures are factual imagery,
    // never simulated accounts or fabricated service connectivity.
    earth = mix(earth, vec3(0.94, 0.975, 1.0), cloud.r * (0.20 + 0.06*day));
    earth += lights * pow(1.0-day, 2.2) * 0.47;

    // Translucent cyan limb and warm/cold photographic fill light.
    float fresnel = pow(1.0-z, 2.25);
    earth = mix(earth, vec3(0.25, 0.69, 1.0), clamp(fresnel*0.72, 0.0, 0.78));
    earth += vec3(0.09, 0.23, 0.39)*pow(1.0-z, 6.5);
    earth = mix(earth, vec3(0.70, 0.86, 1.0), 0.055);
    earth = pow(clamp(earth, 0.0, 1.0), vec3(0.91));
    // Soften the sphere silhouette into the wide sky aura, avoiding the
    // rigid dark edge of a texture pasted into a rectangular image box.
    float rimBlend = smoothstep(0.992, 1.0, radius) * 0.43;
    gl_FragColor = vec4(clamp(mix(earth, skyOut, rimBlend), 0.0, 1.0), 1.0);
}
"""
