package com.nhathuy.nextmeet.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.addListener
import com.nhathuy.nextmeet.R
import kotlin.math.hypot

object AnimationUtils {
    //Animation cho finish activity voi hieu ung hoan thanh
    fun finishWithSuccessAnimation(activity: Activity){
        activity.overridePendingTransition(R.anim.slide_in_left,R.anim.slide_in_bottom)
    }

    //animation hoàn thành với hieu ung tron
    fun createSuccessAnimation(view:View,centerX: Float, centerY:Float, onAnimationEnd: () -> Unit){
        //tinh toan ban kinh cho hieu ung lan toa
        val finalRadius = hypot(view.width.toDouble(),view.height.toDouble()).toFloat()

        //tao hieu ung circular regular
        val circularReveal = ViewAnimationUtils.createCircularReveal(
            view,centerX.toInt(), centerY.toInt(), 0f, finalRadius
        )

        // thiết lập thời gian interpolator
        circularReveal.duration = 500
        circularReveal.interpolator = AccelerateInterpolator()

        //thêm listener cho animation
        circularReveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }
        })

        circularReveal.start()
    }
    fun bounceAnimation(context: Context, view: View) {
        val bounceAnim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.bounce)
        view.startAnimation(bounceAnim)
    }
}