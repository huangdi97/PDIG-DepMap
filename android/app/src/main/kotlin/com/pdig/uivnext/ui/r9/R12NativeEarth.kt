package com.pdig.uivnext.ui.r9

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pdig.uivnext.globe.GlobeCamera
import com.pdig.uivnext.globe.GlobeController
import com.pdig.uivnext.globe.GlobeRenderState
import com.pdig.uivnext.globe.greatCircleSamples
import com.pdig.uivnext.globe.latLonToVec
import com.pdig.uivnext.model.RegionPresentation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Real GPU sphere proof, separate from the production CPU-orthographic renderer. */
@Composable
internal fun R12NativeEarth(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    links: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onFailure: () -> Unit = {},
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val camera = controller.camera
    val view = remember(context, regions, links) {
        R12EarthSurface(context, controller, regions, links,
            onReady = {
                ready = true
                controller.renderState = GlobeRenderState.TEXTURE_READY
            },
            onFail = {
                failed = true
                controller.renderState = GlobeRenderState.ERROR
                onFailure()
            },
        )
    }
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
        onDispose { owner.lifecycle.removeObserver(observer); view.onPause() }
    }
    AndroidView(
        factory = { view },
        update = { it.scene.camera = camera; it.requestRender() },
        modifier = modifier.fillMaxSize().semantics {
            contentDescription = "全球基础设施导航器；渲染器=GPU_3D；纹理状态=" +
                (if(ready) "TEXTURE_READY" else if(failed) "ERROR" else "LOADING")
        },
    )
}

private class R12EarthSurface(
    ctx: Context, controller: GlobeController,
    regions: List<RegionPresentation>, links: List<Pair<String,String>>,
    onReady: () -> Unit, onFail: () -> Unit,
) : GLSurfaceView(ctx) {
    val scene = R12Scene(ctx.applicationContext, regions, links,
        { post { onReady() } }, { post { onFail() } })
    private var x = 0f
    private var y = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragged = false

    /** Project factual region points using the same perspective matrices as the GPU.
     * A tap focuses a region; a second tap opens its Region Drawer.
     * Neither movement nor empty-space taps manufacture a dependency edge.
     */
    private fun hitRegion(xPixel: Float, yPixel: Float): RegionPresentation? {
        if(width < 1 || height < 1) return null
        val camera = controller.camera
        val m = FloatArray(16)
        val v = FloatArray(16)
        val p = FloatArray(16)
        val vp = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        Matrix.rotateM(m, 0, camera.pitchDeg, 1f,0f,0f)
        Matrix.rotateM(m, 0, camera.yawDeg, 0f,1f,0f)
        Matrix.scaleM(m, 0, camera.zoom,camera.zoom,camera.zoom)
        Matrix.setLookAtM(v,0,0f,0f,3.65f,0f,0f,0f,0f,1f,0f)
        Matrix.perspectiveM(p,0,39f,width.toFloat()/height,0.5f,16f)
        Matrix.multiplyMM(vp,0,p,0,v,0)
        Matrix.multiplyMM(mvp,0,vp,0,m,0)
        return regions.mapNotNull { region ->
            val point = latLonToVec(region.latitude.toFloat(), region.longitude.toFloat())
            val transformed = FloatArray(4)
            Matrix.multiplyMV(transformed,0,mvp,0,
                floatArrayOf(point.x*1.028f,point.y*1.028f,point.z*1.028f,1f),0)
            if(transformed[3] <= 0f) return@mapNotNull null
            val clipX=transformed[0]/transformed[3]
            val clipY=transformed[1]/transformed[3]
            val clipZ=transformed[2]/transformed[3]
            if(clipZ !in -1f..1f || clipX !in -1f..1f || clipY !in -1f..1f) return@mapNotNull null
            val screenX=(clipX*.5f+.5f)*width
            val screenY=(.5f-clipY*.5f)*height
            val distance=kotlin.math.hypot(xPixel-screenX,yPixel-screenY)
            if(distance <= 40f*resources.displayMetrics.density) region to distance else null
        }.minByOrNull { it.second }?.first
    }

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,24,0)
        setRenderer(scene)
        renderMode = RENDERMODE_WHEN_DIRTY
        setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    x=event.x; y=event.y
                    downX=x; downY=y; dragged=false
                    controller.interactive=false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx=event.x-x; val dy=event.y-y
                    x=event.x; y=event.y
                    if(kotlin.math.hypot(x-downX,y-downY) > 9f*resources.displayMetrics.density) dragged=true
                    if(!dragged) return@setOnTouchListener true
                    controller.camera=controller.camera.copy(
                        yawDeg=controller.camera.yawDeg-dx*.31f,
                        pitchDeg=(controller.camera.pitchDeg-dy*.31f).coerceIn(-60f,60f))
                    scene.camera=controller.camera
                    requestRender()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if(!dragged) {
                        val hit=hitRegion(event.x,event.y)
                        if(hit != null) {
                            if(controller.selectedRegion==hit.regionCode) {
                                controller.state=com.pdig.uivnext.model.VGlobeState.REGION_DETAIL
                            } else {
                                controller.focusRegion(hit)
                            }
                            scene.camera=controller.camera
                            requestRender()
                        }
                    }
                    controller.interactive=true; true
                }
                MotionEvent.ACTION_CANCEL -> {
                    controller.interactive=true; true
                }
                else -> false
            }
        }
    }
}

private class R12Scene(
    private val context: Context,
    regions: List<RegionPresentation>,
    links: List<Pair<String,String>>,
    private val readyCallback: () -> Unit,
    private val failCallback: () -> Unit,
) : GLSurfaceView.Renderer {
    @Volatile var camera = GlobeCamera(0f,0f)
    private var program=0
    private var overlay=0
    private var texture=0
    private var ready=false
    private val mesh=earthMesh()
    private val meshBuffer=buf(mesh)
    private val nodes=buf(regions.flatMap {
        val p=latLonToVec(it.latitude.toFloat(),it.longitude.toFloat())
        listOf(p.x*1.028f,p.y*1.028f,p.z*1.028f)
    }.toFloatArray())
    private val edges=buf(buildList {
        val map=regions.associateBy { it.regionCode }
        links.forEach { (a,b) ->
            val f=map[a]?:return@forEach
            val t=map[b]?:return@forEach
            val steps=greatCircleSamples(f.latitude.toFloat(),f.longitude.toFloat(),
                t.latitude.toFloat(),t.longitude.toFloat(),48)
            for(i in 0 until steps.lastIndex) {
                for(p in listOf(steps[i],steps[i+1])) {
                    add(p.x*1.025f); add(p.y*1.025f); add(p.z*1.025f)
                }
            }
        }
    }.toFloatArray())
    private val model=FloatArray(16)
    private val projection=FloatArray(16)
    private val view=FloatArray(16)
    private val vp=FloatArray(16)
    private val mvp=FloatArray(16)

    override fun onSurfaceCreated(unused: GL10?, config:EGLConfig?) {
        runCatching {
            GLES20.glClearColor(.93f,.969f,1f,1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            program=link(SPHERE_V,SPHERE_F)
            overlay=link(OVERLAY_V,OVERLAY_F)
            val image=context.assets.open("earth/earth_albedo_2048.png").use {
                BitmapFactory.decodeStream(it)
            } ?: error("Earth offline albedo missing")
            val ids=IntArray(1)
            GLES20.glGenTextures(1,ids,0)
            texture=ids[0]
            check(texture != 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,image,0)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            image.recycle()
            check(GLES20.glGetError()==GLES20.GL_NO_ERROR)
            ready=true
            readyCallback()
        }.onFailure { ready=false; failCallback() }
    }

    override fun onSurfaceChanged(unused:GL10?,w:Int,h:Int) {
        GLES20.glViewport(0,0,w,h)
        Matrix.perspectiveM(projection,0,39f,w.toFloat()/h.coerceAtLeast(1),.5f,16f)
        Matrix.setLookAtM(view,0,0f,0f,3.65f,0f,0f,0f,0f,1f,0f)
        Matrix.multiplyMM(vp,0,projection,0,view,0)
    }
    override fun onDrawFrame(unused:GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if(!ready) return
        Matrix.setIdentityM(model,0)
        Matrix.rotateM(model,0,camera.pitchDeg,1f,0f,0f)
        Matrix.rotateM(model,0,camera.yawDeg,0f,1f,0f)
        Matrix.scaleM(model,0,camera.zoom,camera.zoom,camera.zoom)
        Matrix.multiplyMM(mvp,0,vp,0,model,0)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(u(program,"uMvp"),1,false,mvp,0)
        GLES20.glUniformMatrix4fv(u(program,"uModel"),1,false,model,0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glUniform1i(u(program,"uTex"),0)
        val a=GLES20.glGetAttribLocation(program,"aPos")
        val uv=GLES20.glGetAttribLocation(program,"aUv")
        meshBuffer.position(0)
        GLES20.glVertexAttribPointer(a,3,GLES20.GL_FLOAT,false,20,meshBuffer)
        meshBuffer.position(3)
        GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,20,meshBuffer)
        GLES20.glEnableVertexAttribArray(a)
        GLES20.glEnableVertexAttribArray(uv)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,mesh.size/5)
        GLES20.glDisableVertexAttribArray(a)
        GLES20.glDisableVertexAttribArray(uv)
        if(overlay==0) return
        GLES20.glUseProgram(overlay)
        GLES20.glUniformMatrix4fv(u(overlay,"uMvp"),1,false,mvp,0)
        val pos=GLES20.glGetAttribLocation(overlay,"aPos")
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)
        if(edges.limit()>0) {
            GLES20.glUniform1i(u(overlay,"uDot"),0)
            edges.position(0)
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,12,edges)
            GLES20.glDrawArrays(GLES20.GL_LINES,0,edges.limit()/3)
        }
        if(nodes.limit()>0) {
            GLES20.glUniform1i(u(overlay,"uDot"),1)
            nodes.position(0)
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,12,nodes)
            GLES20.glDrawArrays(GLES20.GL_POINTS,0,nodes.limit()/3)
        }
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun u(program:Int,key:String)=GLES20.glGetUniformLocation(program,key)
    companion object {
        private fun buf(data:FloatArray):FloatBuffer=
            ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply{put(data);position(0)}
        private fun position(lat:Double,lon:Double):FloatArray =
            floatArrayOf((cos(lat)*cos(lon)).toFloat(),sin(lat).toFloat(),
                (cos(lat)*sin(lon)).toFloat())
        private fun earthMesh():FloatArray {
            val rings=48;val columns=96
            val data=FloatArray(rings*columns*6*5)
            var n=0
            fun v(i:Int,j:Int) {
                val xyz=position(-PI/2+PI*i/rings,-PI+2*PI*j/columns)
                data[n++]=xyz[0];data[n++]=xyz[1];data[n++]=xyz[2]
                data[n++]=j.toFloat()/columns
                data[n++]=1f-i.toFloat()/rings
            }
            for(i in 0 until rings) for(j in 0 until columns) {
                v(i,j);v(i+1,j);v(i,j+1)
                v(i,j+1);v(i+1,j);v(i+1,j+1)
            }
            return data
        }
        private fun link(vertex:String,fragment:String):Int {
            fun shader(type:Int,code:String):Int {
                val id=GLES20.glCreateShader(type)
                GLES20.glShaderSource(id,code)
                GLES20.glCompileShader(id)
                val status=IntArray(1)
                GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,status,0)
                if(status[0]!=GLES20.GL_TRUE) error(GLES20.glGetShaderInfoLog(id))
                return id
            }
            val vs=shader(GLES20.GL_VERTEX_SHADER,vertex)
            val fs=shader(GLES20.GL_FRAGMENT_SHADER,fragment)
            val id=GLES20.glCreateProgram()
            GLES20.glAttachShader(id,vs);GLES20.glAttachShader(id,fs)
            GLES20.glLinkProgram(id)
            val status=IntArray(1)
            GLES20.glGetProgramiv(id,GLES20.GL_LINK_STATUS,status,0)
            if(status[0]!=GLES20.GL_TRUE) error(GLES20.glGetProgramInfoLog(id))
            GLES20.glDeleteShader(vs);GLES20.glDeleteShader(fs)
            return id
        }
        private const val SPHERE_V="""
            attribute vec3 aPos;
            attribute vec2 aUv;
            uniform mat4 uMvp;
            uniform mat4 uModel;
            varying vec2 uv;
            varying vec3 norm;
            void main(){
                uv=aUv;norm=normalize(mat3(uModel)*aPos);
                gl_Position=uMvp*vec4(aPos,1.0);
            }
        """
        private const val SPHERE_F="""
            precision mediump float;
            uniform sampler2D uTex;
            varying vec2 uv;
            varying vec3 norm;
            void main(){
                vec3 n=normalize(norm);
                vec3 light=normalize(vec3(-.31,.45,.86));
                vec3 tex=texture2D(uTex,uv).rgb;
                float day=max(dot(n,light),0.0);
                float sea=step(tex.r+.07,tex.b)*step(tex.g+.02,tex.b);
                float glint=pow(max(dot(reflect(-light,n),vec3(0.,0.,1.)),0.0),42.0)*sea;
                float rim=pow(1.0-max(n.z,0.0),2.7);
                vec3 col=tex*(.50+.75*day)+glint*vec3(.65,.88,1.0)*.48;
                col=mix(col,vec3(.44,.76,1.0),rim*.34);
                gl_FragColor=vec4(min(col,vec3(1.0)),1.0);
            }
        """
        private const val OVERLAY_V="""
            attribute vec3 aPos;
            uniform mat4 uMvp;
            void main(){gl_Position=uMvp*vec4(aPos,1.0);gl_PointSize=12.0;}
        """
        private const val OVERLAY_F="""
            precision mediump float;
            uniform int uDot;
            void main(){
                if(uDot==1){
                    float r=length(gl_PointCoord-.5);
                    if(r>.5)discard;
                    gl_FragColor=vec4(mix(vec3(.17,.58,1.),vec3(1.),1.0-smoothstep(.06,.28,r)),1.);
                } else gl_FragColor=vec4(.22,.69,1.0,.78);
            }
        """
    }
}
