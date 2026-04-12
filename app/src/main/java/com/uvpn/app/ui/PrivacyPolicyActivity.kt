package com.uvpn.app.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.uvpn.app.R

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        val wv = findViewById<WebView>(R.id.webView)
        wv.webViewClient = WebViewClient()
        wv.settings.javaScriptEnabled = false

        // Load from GitHub raw
        wv.loadUrl("https://raw.githubusercontent.com/jdbag/UVPN-android/main/PRIVACY_POLICY.md")

        // Fallback: load local HTML
        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              body { font-family: sans-serif; padding: 20px; background: #0b1623; color: #e2eaf4; line-height: 1.6; }
              h1 { color: #38bdf8; font-size: 20px; }
              h2 { color: #3b82f6; font-size: 16px; margin-top: 24px; }
              p, li { color: #8ba4c0; font-size: 14px; }
              strong { color: #ffffff; }
              table { width: 100%; border-collapse: collapse; }
              td, th { border: 1px solid #1e2f45; padding: 8px; font-size: 13px; }
              th { background: #162236; color: #3b82f6; }
            </style>
            </head><body>
            <h1>🛡️ U VPN Privacy Policy</h1>
            <p><strong>Last updated:</strong> April 12, 2026</p>
            <h2>What we collect</h2>
            <ul>
              <li>❌ We do NOT log VPN traffic</li>
              <li>❌ We do NOT store your IP address</li>
              <li>❌ No account required</li>
              <li>✅ Ads use Google AdMob (device ID only)</li>
            </ul>
            <h2>VPN Traffic</h2>
            <p>All traffic is routed through <strong>Cloudflare WARP</strong> and encrypted with WireGuard. We have no access to your traffic.</p>
            <h2>Ads</h2>
            <p>We use Google AdMob rewarded ads. AdMob may collect device advertising ID. See <a href="https://policies.google.com/privacy" style="color:#38bdf8">Google Privacy Policy</a>.</p>
            <h2>Contact</h2>
            <p>Questions? Open an issue on <a href="https://github.com/jdbag/UVPN-android" style="color:#38bdf8">GitHub</a>.</p>
            </body></html>
        """.trimIndent()

        wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        // Back button
        try {
            val backBtn = findViewById<ImageButton>(R.id.btnBack)
            backBtn.setOnClickListener { finish() }
        } catch (_: Exception) { }
    }

    override fun onBackPressed() { super.onBackPressed(); finish() }
}
