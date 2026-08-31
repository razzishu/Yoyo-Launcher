package com.yoyo.launcher.google

import android.view.MotionEvent
import com.yoyo.launcher.Launcher
import com.android.systemui.plugins.shared.LauncherOverlayManager
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayTouchProxy
import com.google.android.libraries.gsa.launcherclient.LauncherClient
import com.google.android.libraries.gsa.launcherclient.LauncherClientCallbacks

/**
 * Implementation of LauncherOverlayManager that connects to the Google App using LauncherClient.
 */
class GoogleFeedOverlayManager(private val launcher: Launcher) : 
    LauncherOverlayManager, LauncherOverlayTouchProxy, LauncherClientCallbacks {

    private val client: LauncherClient = LauncherClient(launcher, this, LauncherClient.ClientOptions(true, true, true))
    private var callbacks: LauncherOverlayCallbacks? = null

    init {
        launcher.setLauncherOverlay(this)
    }

    override fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?) {
        this.callbacks = callbacks
    }

    override fun onOverlayMotionEvent(ev: MotionEvent, distance: Float) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> client.startMove()
            // Modern LauncherClient expects a progress ratio (0.0 to 1.0), not pixels!
            // Passing pixels caused the progress to instantly exceed 1.0, skipping the animation.
            MotionEvent.ACTION_MOVE -> client.updateMove(distance)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> client.endMove()
        }
    }

    override fun onOverlayMotionEvent(ev: MotionEvent, distance: Int) {
        onOverlayMotionEvent(ev, distance.toFloat())
    }

    override fun onFlingVelocity(velocity: Int) {
        // endMove in LauncherClient handles velocity internally or via the MotionEvents
    }

    override fun onActivityDestroyed() {
        client.onDestroy()
    }

    override fun onAttachedToWindow() {
        client.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        client.onDetachedFromWindow()
    }

    override fun onActivityStopped() {
        client.onStop()
    }

    override fun onActivityStarted() {
        client.onStart()
    }

    override fun onActivityResumed() {
        client.onResume()
    }

    override fun onActivityPaused() {
        client.onPause()
    }

    override fun hideOverlay(animate: Boolean) {
        client.hideOverlay(if (animate) 1 else 0)
    }

    override fun onOverlayScrollChanged(progress: Float) {
        callbacks?.onOverlayScrollChanged(progress)
    }

    override fun onServiceStateChanged(overlayAttached: Boolean, hotwordActive: Boolean) {
        // Optional: track service state
    }
}
