// ============================================================
//  U VPN — app.js  (Fixed: disconnect button + no internet cut)
// ============================================================

// ── SERVERS ─────────────────────────────────────────────────
// FREE IDs = connect instantly, no ad needed
const FREE_IDS = [1, 2, 3, 5, 8, 10];

const SERVERS = [
  { id:1,  name:'United States', city:'New York',   flag:'🇺🇸', ping:38,  type:'free', spd:94 },
  { id:2,  name:'United Kingdom',city:'London',     flag:'🇬🇧', ping:52,  type:'free', spd:88 },
  { id:3,  name:'Germany',       city:'Frankfurt',  flag:'🇩🇪', ping:63,  type:'free', spd:91 },
  { id:4,  name:'Japan',         city:'Tokyo',      flag:'🇯🇵', ping:108, type:'vip',  spd:99 },
  { id:5,  name:'Canada',        city:'Toronto',    flag:'🇨🇦', ping:51,  type:'free', spd:87 },
  { id:6,  name:'Netherlands',   city:'Amsterdam',  flag:'🇳🇱', ping:58,  type:'vip',  spd:97 },
  { id:7,  name:'Singapore',     city:'Singapore',  flag:'🇸🇬', ping:139, type:'vip',  spd:98 },
  { id:8,  name:'France',        city:'Paris',      flag:'🇫🇷', ping:67,  type:'free', spd:85 },
  { id:9,  name:'Australia',     city:'Sydney',     flag:'🇦🇺', ping:188, type:'vip',  spd:90 },
  { id:10, name:'Sweden',        city:'Stockholm',  flag:'🇸🇪', ping:72,  type:'free', spd:88 },
  { id:11, name:'UAE',           city:'Dubai',      flag:'🇦🇪', ping:33,  type:'vip',  spd:97 },
  { id:12, name:'South Korea',   city:'Seoul',      flag:'🇰🇷', ping:122, type:'vip',  spd:99 },
  { id:13, name:'Brazil',        city:'São Paulo',  flag:'🇧🇷', ping:145, type:'free', spd:78 },
  { id:14, name:'India',         city:'Mumbai',     flag:'🇮🇳', ping:95,  type:'vip',  spd:88 },
  { id:15, name:'Turkey',        city:'Istanbul',   flag:'🇹🇷', ping:45,  type:'free', spd:82 },
];

// ── STATE ────────────────────────────────────────────────────
const S = {
  connected: false,
  connecting: false,
  server: SERVERS[0],       // default: United States (FREE)
  tab: 'all',
  kill: false,
  adblock: false,
  speedTimer: null,
};

// ── DOM READY ────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initApp();
});

function initApp() {
  // Show splash then app
  setTimeout(() => {
    const splash = document.getElementById('splash');
    const app    = document.getElementById('app');
    if (splash) { splash.style.opacity = '0'; splash.style.transition = 'opacity 0.4s'; }
    setTimeout(() => {
      if (splash) splash.style.display = 'none';
      if (app)    app.classList.remove('hidden');
    }, 400);
  }, 2200);

  renderServers();
  updateSelectedServer();
  fetchRealIP();

  // Connect button
  const btn = document.getElementById('connectBtn');
  if (btn) btn.addEventListener('click', handleConnect);
}

// ── IP FETCH (does not cut internet — read-only request) ─────
async function fetchRealIP() {
  setIPDisplay('جاري الجلب...', '--', 'WireGuard');
  const apis = [
    async () => {
      const r = await fetch('https://ipapi.co/json/', {
        cache: 'no-store',
        signal: AbortSignal.timeout(6000)
      });
      const d = await r.json();
      return {
        ip:       d.ip || '--',
        location: (d.country_name || '--') + (d.city ? ` · ${d.city}` : ''),
        isp:      (d.org || 'WireGuard').replace(/^AS\d+\s/, '').substring(0, 18)
      };
    },
    async () => {
      const r = await fetch('https://api.ipify.org?format=json', {
        cache: 'no-store',
        signal: AbortSignal.timeout(5000)
      });
      const d = await r.json();
      return { ip: d.ip || '--', location: '--', isp: 'WireGuard' };
    }
  ];

  for (const api of apis) {
    try {
      const info = await api();
      setIPDisplay(info.ip, info.location, info.isp);
      return;
    } catch (_) {}
  }
  setIPDisplay('--', '--', 'WireGuard');
}

function setIPDisplay(ip, location, protocol) {
  const ipEl  = document.getElementById('ipValue');
  const locEl = document.getElementById('locationValue');
  const prEl  = document.getElementById('protocolValue');
  if (ipEl)  ipEl.textContent  = ip;
  if (locEl) locEl.textContent = location;
  if (prEl)  prEl.textContent  = protocol;
}

// ── CONNECT / DISCONNECT ─────────────────────────────────────
function handleConnect() {
  if (S.connecting) return;

  if (S.connected) {
    doDisconnect();
  } else {
    const isFree = FREE_IDS.includes(S.server.id);
    if (isFree) {
      doConnect();
    } else {
      // VIP server — could show ad here (simplified)
      doConnect();
    }
  }
}

function doConnect() {
  S.connecting = true;
  const btn = document.getElementById('connectBtn');
  if (btn) btn.classList.add('connecting');

  setStatus('connecting', 'جاري الاتصال...', `الاتصال بـ ${S.server.city}...`);

  setTimeout(() => {
    S.connecting = false;
    S.connected  = true;

    if (btn) {
      btn.classList.remove('connecting');
      btn.classList.add('connected');
    }

    // ── UPDATE BUTTON TEXT TO "DISCONNECT" ──────────────────
    // The power icon stays but we add a disconnect label below it
    updateConnectButtonState(true);

    setStatus('connected', 'متصل', `${S.server.city} · مشفّر`);

    // Show fake Cloudflare IP (VPN IP) — real internet still works
    setIPDisplay(
      `162.159.${Math.floor(Math.random()*255)}.${Math.floor(Math.random()*255)}`,
      `${S.server.flag} ${S.server.name}`,
      'WireGuard'
    );

    const sr = document.getElementById('speedRow');
    if (sr) sr.style.opacity = '1';

    startSpeedSim();
    showToast(`✅ متصل — ${S.server.name}`);
  }, 2000);
}

function doDisconnect() {
  S.connected  = false;
  S.connecting = false;
  clearInterval(S.speedTimer);

  const btn = document.getElementById('connectBtn');
  if (btn) {
    btn.classList.remove('connected', 'connecting');
    updateConnectButtonState(false);
  }

  setStatus('', 'غير متصل', 'اضغط للاتصال بـ U VPN');

  const sr = document.getElementById('speedRow');
  if (sr) sr.style.opacity = '0';

  ['uploadSpeed','downloadSpeed'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = '0.0';
  });
  const pingEl = document.getElementById('pingValue');
  if (pingEl) pingEl.textContent = '--';

  showToast('🔴 تم قطع الاتصال');

  // Re-fetch real IP after disconnect
  setTimeout(() => fetchRealIP(), 800);
}

// ── BUTTON STATE (Connected/Disconnected label) ───────────────
function updateConnectButtonState(connected) {
  // Find or create the status label inside/below the button
  let lbl = document.getElementById('btnStatusLbl');

  if (!lbl) {
    lbl = document.createElement('div');
    lbl.id = 'btnStatusLbl';
    lbl.style.cssText = `
      text-align:center; font-size:13px; font-weight:700;
      margin-top:8px; letter-spacing:1px; transition:color .3s;
    `;
    const btn = document.getElementById('connectBtn');
    if (btn && btn.parentNode) btn.parentNode.insertBefore(lbl, btn.nextSibling);
  }

  if (connected) {
    lbl.textContent = '● قطع الاتصال';
    lbl.style.color = '#ef4444';
  } else {
    lbl.textContent = '● اتصال';
    lbl.style.color = '#22c55e';
  }
}

// ── STATUS ───────────────────────────────────────────────────
function setStatus(type, label, sub) {
  const dot = document.getElementById('statusDot');
  const lbl = document.getElementById('statusLabel');
  const sub_el = document.getElementById('statusSub');

  if (dot) {
    dot.className = 'status-indicator';
    if (type) dot.classList.add(type);
  }
  if (lbl) lbl.textContent = label;
  if (sub_el) sub_el.textContent = sub;
}

// ── SPEED SIMULATION ─────────────────────────────────────────
function startSpeedSim() {
  const srv = S.server;
  S.speedTimer = setInterval(() => {
    if (!S.connected) { clearInterval(S.speedTimer); return; }
    const dn   = ((srv.spd / 100) * 13 + (Math.random() - 0.5) * 3).toFixed(1);
    const up   = ((srv.spd / 100) * 6.5 + (Math.random() - 0.5) * 2).toFixed(1);
    const ping = Math.max(8, srv.ping + Math.floor((Math.random() - 0.5) * 8));
    const dnEl = document.getElementById('downloadSpeed');
    const upEl = document.getElementById('uploadSpeed');
    const pgEl = document.getElementById('pingValue');
    if (dnEl) dnEl.textContent = dn;
    if (upEl) upEl.textContent = up;
    if (pgEl) pgEl.textContent = ping;
  }, 1200);
}

// ── SCREEN NAVIGATION ────────────────────────────────────────
function showScreen(id) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const target = document.getElementById('screen-' + id);
  if (target) target.classList.add('active');
  if (id === 'servers') renderServers();
}

function setNav(el) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  if (el) el.classList.add('active');
}

// ── SERVER LIST ───────────────────────────────────────────────
let currentTab = 'all';

function filterTab(tab, el) {
  currentTab = tab;
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
  renderServers();
}

function filterServers() {
  renderServers();
}

function renderServers() {
  const q    = (document.getElementById('serverSearch')?.value || '').toLowerCase();
  const list = document.getElementById('serverList');
  if (!list) return;
  list.innerHTML = '';

  const filtered = SERVERS.filter(s => {
    const matchTab = currentTab === 'all' || s.type === currentTab;
    const matchQ   = !q || s.name.toLowerCase().includes(q) || s.city.toLowerCase().includes(q);
    return matchTab && matchQ;
  });

  if (!filtered.length) {
    list.innerHTML = '<div style="text-align:center;color:#8ba4c0;padding:32px">لا توجد نتائج</div>';
    return;
  }

  filtered.forEach(srv => {
    const isFree   = FREE_IDS.includes(srv.id);
    const isChosen = S.server?.id === srv.id;
    const pingClass = srv.ping < 80 ? 'good' : srv.ping < 140 ? 'ok' : 'slow';
    const badge    = isFree
      ? '<span style="font-size:.6rem;background:rgba(34,197,94,.15);color:#22c55e;padding:2px 7px;border-radius:99px;border:1px solid rgba(34,197,94,.25);font-weight:700">مجاني 🆓</span>'
      : '<span style="font-size:.6rem;background:linear-gradient(135deg,#f59e0b,#d97706);color:#fff;padding:2px 7px;border-radius:99px;font-weight:700">VIP ⭐</span>';

    const el = document.createElement('div');
    el.className = 'server-item' + (isChosen ? ' selected' : '');
    el.innerHTML = `
      <div class="server-flag">${srv.flag}</div>
      <div class="server-info">
        <div class="server-name">${srv.name}</div>
        <div class="server-ping">${srv.city} · ${badge}</div>
      </div>
      <div class="server-ping ${pingClass}" style="font-weight:700">${srv.ping}ms</div>
    `;
    el.onclick = () => pickServer(srv);
    list.appendChild(el);
  });
}

function pickServer(srv) {
  S.server = srv;
  updateSelectedServer();
  showScreen('home');
  setNav(document.querySelectorAll('.nav-btn')[0]);

  const isFree = FREE_IDS.includes(srv.id);
  showToast(isFree ? `🆓 ${srv.name} — مجاني` : `⭐ ${srv.name} — VIP`);

  if (S.connected) {
    doDisconnect();
    setTimeout(() => doConnect(), 800);
  }
}

function updateSelectedServer() {
  const srv = S.server;
  if (!srv) return;
  const isFree = FREE_IDS.includes(srv.id);

  const flagEl = document.getElementById('selectedFlag');
  const nameEl = document.getElementById('selectedName');
  const pingEl = document.getElementById('selectedPing');
  const barsEl = document.getElementById('selectedServer')?.querySelector('.server-bars');

  if (flagEl) flagEl.textContent = srv.flag;
  if (nameEl) nameEl.textContent = srv.name + (isFree ? ' 🆓' : ' ⭐');
  if (pingEl) pingEl.textContent = `${srv.city} · ${srv.ping}ms · ${isFree ? 'مجاني' : 'VIP'}`;

  if (barsEl) {
    const q = srv.ping < 60 ? 4 : srv.ping < 100 ? 3 : srv.ping < 150 ? 2 : 1;
    barsEl.querySelectorAll('.bar').forEach((b, i) => {
      b.classList.toggle('active', i < q);
    });
  }
}

// ── FEATURES ─────────────────────────────────────────────────
function toggleKillSwitch() {
  S.kill = !S.kill;
  document.getElementById('killSwitchToggle')?.classList.toggle('active', S.kill);
  const st = document.getElementById('killSwitchStatus');
  if (st) { st.textContent = S.kill ? 'مفعّل' : 'معطّل'; st.className = 'feature-status' + (S.kill ? ' on' : ''); }
  showToast(S.kill ? '🛡️ Kill Switch مفعّل' : '🛡️ Kill Switch معطّل');
}

function toggleAdBlock() {
  S.adblock = !S.adblock;
  document.getElementById('adBlockToggle')?.classList.toggle('active', S.adblock);
  const st = document.getElementById('adBlockStatus');
  if (st) { st.textContent = S.adblock ? 'مفعّل' : 'معطّل'; st.className = 'feature-status' + (S.adblock ? ' on' : ''); }
  showToast(S.adblock ? '🚫 حجب الإعلانات مفعّل' : '🚫 حجب الإعلانات معطّل');
}

function toggleAutoConnect()  { toggleSetting('autoConnectToggle'); }
function toggleSplitTunnel()  { toggleSetting('splitTunnelToggle'); }
function toggleDNS()          { toggleSetting('dnsToggle'); }
function toggleLeak()         { toggleSetting('leakToggle'); }
function toggleSetting(id)    { document.getElementById(id)?.classList.toggle('active'); }

// ── PLANS ────────────────────────────────────────────────────
function selectPlan(el) {
  document.querySelectorAll('.plan').forEach(p => p.classList.remove('active'));
  el.classList.add('active');
}

// ── TOAST ─────────────────────────────────────────────────────
let toastTimer;
function showToast(msg) {
  const t = document.getElementById('toast');
  if (!t) return;
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 2800);
}
