# Privacy Policy — U VPN

**Last updated:** April 12, 2026  
**App:** U VPN  
**Developer:** jdbag  
**Contact:** jdbag@github.com

---

## 1. Introduction

U VPN ("we", "our", "the app") is committed to protecting your privacy. This Privacy Policy explains what information we collect, how we use it, and your rights.

By using U VPN, you agree to the terms described in this policy.

---

## 2. Information We Collect

### 2.1 Information We Do NOT Collect
- ❌ VPN traffic content or browsing history
- ❌ DNS queries made through the VPN
- ❌ Your real IP address (not stored)
- ❌ Personal identification information
- ❌ Account credentials (no account required)

### 2.2 Information Collected by Third Parties

**Google AdMob**  
We use Google AdMob to display rewarded video ads. AdMob may collect:
- Device advertising ID (GAID)
- Device information (model, OS version)
- Ad interaction data

AdMob Privacy Policy: https://policies.google.com/privacy

**IP Lookup Services**  
To show your current IP address, the app makes requests to:
- `ipapi.co` — to display your IP, country, city, ISP
- `ip-api.com` — fallback IP lookup
- `api.ipify.org` — fallback IP lookup

These services may log requests per their own policies.

---

## 3. How We Use Information

| Purpose | Data Used |
|---------|-----------|
| Display current IP | IP lookup API responses |
| Show relevant ads | AdMob (device ID) |
| Track session time | Local device storage only |

---

## 4. VPN Traffic

U VPN routes your internet traffic through **Cloudflare's WARP network**. When connected:

- Your traffic is encrypted using **WireGuard** protocol
- Your traffic exits through Cloudflare's servers
- **We do not log, monitor, or store your VPN traffic**
- DNS queries are handled by **Cloudflare 1.1.1.1** (privacy-first DNS)

Cloudflare's Privacy Policy: https://www.cloudflare.com/privacypolicy/

---

## 5. Data Storage

All session data (e.g., remaining VPN time from watching ads) is stored **locally on your device only** using Android SharedPreferences. No data is transmitted to our servers.

---

## 6. Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Required for VPN and IP lookup |
| `FOREGROUND_SERVICE` | Required to keep VPN active |
| `BIND_VPN_SERVICE` | Required to create VPN tunnel |
| `AD_ID` | Required by Google AdMob |

---

## 7. Third-Party Services

| Service | Purpose | Privacy Policy |
|---------|---------|----------------|
| Cloudflare WARP | VPN tunnel | https://www.cloudflare.com/privacypolicy/ |
| Google AdMob | Rewarded ads | https://policies.google.com/privacy |
| ipapi.co | IP lookup | https://ipapi.co/privacy/ |

---

## 8. Children's Privacy

U VPN is not directed at children under 13. We do not knowingly collect personal information from children.

---

## 9. Your Rights

You have the right to:
- Know what data is collected about you
- Request deletion of your data
- Opt out of personalized ads (via Android Settings → Google → Ads)

---

## 10. Changes to This Policy

We may update this Privacy Policy. Changes will be reflected by updating the "Last updated" date. Continued use of the app after changes constitutes acceptance.

---

## 11. Contact

For privacy-related questions:

- **GitHub**: https://github.com/jdbag/UVPN-android/issues
- **Email**: jdbag@github.com

---

*This privacy policy was last updated on April 12, 2026.*
