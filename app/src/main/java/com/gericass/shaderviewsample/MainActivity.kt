package com.gericass.shaderviewsample

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.opengl.GLES30
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.appspell.shaderview.ShaderView
import com.appspell.shaderview.gl.params.ShaderParamsBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.gericass.shaderviewsample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.util.DisplayMetrics

import android.os.Build

import android.view.*

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var previousX: Float = 0f
    private var previousY: Float = 0f

    private var targetSpeed: Float = 0f
    private var followPointerX: Float = 0f
    private var followPointerY: Float = 0f

    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { v, e ->
        val x: Float = e.x
        val y: Float = e.y

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                val speed = sqrt(
                    (previousX - x).toDouble().pow(2.0) +
                            (previousY - y).toDouble().pow(2.0)
                )
                targetSpeed -= (0.1 * (targetSpeed - speed)).toFloat()
                followPointerX -= (0.1 * (followPointerX - x)).toFloat()
                followPointerY -= (0.1 * (followPointerY - y)).toFloat()
            }
            MotionEvent.ACTION_UP -> {
                targetSpeed = 0f
                followPointerX = 0f
                followPointerY = 0f
            }
        }

        previousX = x
        previousY = y

        return@OnTouchListener true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            loadImage()?.also { bitmap ->
                setUpShaderView(bitmap)
            }
        }
    }

    private suspend fun loadImage(): Bitmap? = suspendCoroutine { cont ->
        Glide.with(this)
            .asBitmap()
            .load("https://s3-ap-northeast-1.amazonaws.com/qiita-image-store/0/155135/0c2db45b0bd4b1aa023f5a7da835b76c2d191bd4/x_large.png?1585895165")
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    // NOP
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    cont.resume(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    cont.resume(null)
                }
            })
    }

    private fun setUpShaderView(bitmap: Bitmap) {
        val params = ShaderParamsBuilder()
            .addTexture2D(
                "uTexture",
                bitmap,
                GLES30.GL_TEXTURE0
            )
            .addVec2f("resolution", floatArrayOf(1f, 1f))
            .addVec2f("uPointer", floatArrayOf(followPointerX, followPointerY))
            .addFloat("uVelo", 0f)
            .build()
        val shaderView = ShaderView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                getScreenWidth(),
                getScreenWidth(),
            )
            updateContinuously = true
            fragmentShaderRawResId = R.raw.fragment
            shaderParams = params
            onDrawFrameListener = { shaderParams ->
                shaderParams.updateValue(
                    "uPointer",
                    floatArrayOf(followPointerX / 1000, followPointerY / 1000)
                )
                shaderParams.updateValue("uVelo", min(targetSpeed / 100, 0.5f))
                targetSpeed *= 0.999.toFloat()
            }
            setOnTouchListener(touchListener)
        }
        binding.root.addView(shaderView)
    }

    private fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }
}
