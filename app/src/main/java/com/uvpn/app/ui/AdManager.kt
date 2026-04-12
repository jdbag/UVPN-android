package com.uvpn.app.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdManager — Rewarded Ads with VPN Session Logic
 * ─────────────────────────────────────────────────
 * 1 ad watched  → 2 hours VPN access
 * 2 ads watched → 12 hours VPN access
 *
 * Ad Unit IDs (Google AdMob):
 *  Rewarded #1 : ca-app-pub-3640039090708511/9040037837
 *  Rewarded #2 : ca-app-pub-3640039090708511/1032826356  (second ad = bonus)
 */
object AdManager {

    private const val TAG = "AdManager"
    private const val PREFS = "uvpn_session"
    private const val KEY_EXPIRY = "session_expiry_ms"
    private const val KEY_ADS_WATCHED = "ads_watched_session"

    // AdMob Rewarded Unit IDs
    private const val REWARDED_AD_1 = "ca-app-pub-3640039090708511/9040037837"
    private const val REWARDED_AD_2 = "ca-app-pub-3640039090708511/1032826356"

    // Session durations
    private const val ONE_AD_HOURS   = 2L    // 2 hours
    private const val TWO_ADS_HOURS  = 12L   // 12 hours
    private const val MS_PER_HOUR    = 3_600_000L

    private var rewardedAd1: RewardedAd? = null
    private var rewardedAd2: RewardedAd? = null
    private var isLoading1 = false
    private var isLoading2 = false

    // ── Init AdMob (call once from Application or MainActivity) ──
    fun init(context: Context) {
        MobileAds.initialize(context) {
            Log.i(TAG, "AdMob initialized")
            preloadAds(context)
        }
    }

    // ── Preload both rewarded ads ─────────────────────────────
    fun preloadAds(context: Context) {
        loadAd1(context)
        loadAd2(context)
    }

    private fun loadAd1(context: Context) {
        if (isLoading1 || rewardedAd1 != null) return
        isLoading1 = true
        val req = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_1, req, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd1 = ad
                isLoading1 = false
                Log.i(TAG, "Rewarded Ad 1 loaded")
            }
            override fun onAdFailedToLoad(err: LoadAdError) {
                rewardedAd1 = null
                isLoading1 = false
                Log.e(TAG, "Ad 1 failed: ${err.message}")
            }
        })
    }

    private fun loadAd2(context: Context) {
        if (isLoading2 || rewardedAd2 != null) return
        isLoading2 = true
        val req = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_2, req, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd2 = ad
                isLoading2 = false
                Log.i(TAG, "Rewarded Ad 2 loaded")
            }
            override fun onAdFailedToLoad(err: LoadAdError) {
                rewardedAd2 = null
                isLoading2 = false
                Log.e(TAG, "Ad 2 failed: ${err.message}")
            }
        })
    }

    // ── Session state ─────────────────────────────────────────
    fun hasActiveSession(context: Context): Boolean {
        val expiry = prefs(context).getLong(KEY_EXPIRY, 0L)
        return System.currentTimeMillis() < expiry
    }

    fun remainingMinutes(context: Context): Long {
        val expiry = prefs(context).getLong(KEY_EXPIRY, 0L)
        val remaining = expiry - System.currentTimeMillis()
        return if (remaining > 0) remaining / 60_000L else 0L
    }

    fun adsWatchedInSession(context: Context): Int {
        return prefs(context).getInt(KEY_ADS_WATCHED, 0)
    }

    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // ── Show Ad 1 → 2 hours ───────────────────────────────────
    fun showFirstAd(
        activity: Activity,
        onGranted: (hoursGranted: Long) -> Unit,
        onFailed: () -> Unit
    ) {
        val ad = rewardedAd1
        if (ad == null) {
            Log.w(TAG, "Ad 1 not ready, reloading...")
            loadAd1(activity)
            onFailed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd1 = null
                loadAd1(activity)           // preload next
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                rewardedAd1 = null
                onFailed()
            }
        }

        ad.show(activity) { _ ->
            // Reward earned — grant 2 hours
            val prefs = prefs(activity)
            val now = System.currentTimeMillis()
            val existing = prefs.getLong(KEY_EXPIRY, now)
            val base = if (existing > now) existing else now
            val newExpiry = base + (ONE_AD_HOURS * MS_PER_HOUR)
            prefs.edit()
                .putLong(KEY_EXPIRY, newExpiry)
                .putInt(KEY_ADS_WATCHED, 1)
                .apply()
            Log.i(TAG, "Ad 1 rewarded: 2 hours granted")
            onGranted(ONE_AD_HOURS)
        }
    }

    // ── Show Ad 2 → total 12 hours (bonus) ───────────────────
    fun showSecondAd(
        activity: Activity,
        onGranted: (hoursGranted: Long) -> Unit,
        onFailed: () -> Unit
    ) {
        val ad = rewardedAd2
        if (ad == null) {
            Log.w(TAG, "Ad 2 not ready, reloading...")
            loadAd2(activity)
            onFailed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd2 = null
                loadAd2(activity)
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                rewardedAd2 = null
                onFailed()
            }
        }

        ad.show(activity) { _ ->
            // Reward earned — upgrade to 12 hours total
            val prefs = prefs(activity)
            val now = System.currentTimeMillis()
            val newExpiry = now + (TWO_ADS_HOURS * MS_PER_HOUR)
            prefs.edit()
                .putLong(KEY_EXPIRY, newExpiry)
                .putInt(KEY_ADS_WATCHED, 2)
                .apply()
            Log.i(TAG, "Ad 2 rewarded: 12 hours granted")
            onGranted(TWO_ADS_HOURS)
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
