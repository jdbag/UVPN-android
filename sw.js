const CACHE_NAME = 'uvpn-v1.0.0';
const ASSETS = [
  '/Steptop-android/',
  '/Steptop-android/index.html',
  '/Steptop-android/style.css',
  '/Steptop-android/app.js',
  '/Steptop-android/logo.svg',
  '/Steptop-android/manifest.json',
  '/Steptop-android/icons/icon-192.png',
  '/Steptop-android/icons/icon-512.png',
  'https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;900&family=Rajdhani:wght@600;700&display=swap'
];

// Install: cache all assets
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
  self.skipWaiting();
});

// Activate: clean old caches
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

// Fetch: cache-first strategy
self.addEventListener('fetch', e => {
  e.respondWith(
    caches.match(e.request).then(cached => {
      if (cached) return cached;
      return fetch(e.request).then(res => {
        if (!res || res.status !== 200 || res.type === 'opaque') return res;
        const clone = res.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(e.request, clone));
        return res;
      }).catch(() => caches.match('/Steptop-android/index.html'));
    })
  );
});
