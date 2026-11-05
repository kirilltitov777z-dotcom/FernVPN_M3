package com.fern.vpn

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Interstitial advertising shown once the tunnel is up.
 *
 * The IDs below are Google's official *test* units — they render a demo ad and
 * earn nothing. Create an app in AdMob, put your real app ID into
 * AndroidManifest.xml and your real interstitial unit ID here.
 */
object Ads {

    private const val INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /** Do not bother the user more often than this. */
    private const val MIN_INTERVAL_MS = 3 * 60 * 1000L

    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L

    fun initialize(context: Context) {
        runCatching {
            MobileAds.initialize(context) {
                runCatching { load(context) }
            }
        }.onFailure { Log.w("FernAds", "init failed: " + it.javaClass.simpleName) }
    }

    fun load(context: Context) {
        if (loading || interstitial != null) return
        loading = true
        InterstitialAd.load(
            context,
            INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loading = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Log.w("FernAds", "load failed: " + error.message)
                }
            }
        )
    }

    /** Shows an ad if one is ready and the cooldown has passed. */
    fun maybeShow(activity: Activity) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShownAt < MIN_INTERVAL_MS) return
        val ad = interstitial ?: run {
            load(activity)
            return
        }
        lastShownAt = now
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                runCatching { load(activity) }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                runCatching { load(activity) }
            }
        }
        runCatching { ad.show(activity) }.onFailure {
            Log.w("FernAds", "show failed: " + it.javaClass.simpleName)
            runCatching { load(activity) }
        }
    }
}

/** Unwraps the Activity that hosts a Compose context. */
fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
