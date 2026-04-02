// ===== APP STATE =====
const state = {
  connected: false,
  connecting: false,
  selectedServer: null,
  killSwitch: false,
  adBlock: false,
  autoConnect: false,
  splitTunnel: false,
  dns: false,
  dnsLeak: true,
  currentTab: 'all',
  speedInterval: null,
  timeInterval: null,
};

// ===== SERVERS DATA =====
const servers = [
  { id: 1, name: 'الولايات المتحدة', city: 'نيويورك', flag: '🇺🇸', ping: 42, type: 'free', ip: '104.21.45.12', speed: 95 },
  { id: 2, name: 'المملكة المتحدة', city: 'لندن', flag: '🇬🇧', ping: 58, type: 'free', ip: '188.114.97.22', speed: 87 },
  { id: 3, name: 'ألمانيا', city: 'فرانكفورت', flag: '🇩🇪', ping: 65, type: 'free', ip: '172.67.183.44', speed: 91 },
  { id: 4, name: 'اليابان', city: 'طوكيو', flag: '🇯🇵', ping: 112, type: 'vip', ip: '172.67.14.92', speed: 99 },
  { id: 5, name: 'كندا', city: 'تورنتو', flag: '🇨🇦', ping: 55, type: 'free', ip: '104.21.23.77', speed: 88 },
  { id: 6, name: 'هولندا', city: 'أمستردام', flag: '🇳🇱', ping: 61, type: 'vip', ip: '188.114.100.5', speed: 96 },
  { id: 7, name: 'سنغافورة', city: 'سنغافورة', flag: '🇸🇬', ping: 145, type: 'vip', ip: '172.67.200.11', speed: 98 },
  { id: 8, name: 'فرنسا', city: 'باريس', flag: '🇫🇷', ping: 70, type: 'free', ip: '104.21.77.33', speed: 85 },
  { id: 9, name: 'أستراليا', city: 'سيدني', flag: '🇦🇺', ping: 190, type: 'vip', ip: '188.114.55.99', speed: 90 },
  { id: 10, name: 'السويد', city: 'ستوكهولم', flag: '🇸🇪', ping: 75, type: 'free', ip: '172.67.45.66', speed: 88 },
  { id: 11, name: 'الإمارات', city: 'دبي', flag: '🇦🇪', ping: 35, type: 'vip', ip: '104.21.12.55', speed: 97 },
  { id: 12, name: 'البرازيل', city: 'ساو باولو', flag: '🇧🇷', ping: 155, type: 'free', ip: '188.114.88.44', speed: 79 },
  { id: 13, name: 'كوريا الجنوبية', city: 'سيئول', flag: '🇰🇷', ping: 125, type: 'vip', ip: '172.67.234.99', speed: 98 },
  { id: 14, name: 'سويسرا', city: 'زيورخ', flag: '🇨🇭', ping: 68, type: 'vip', ip: '104.21.99.11', speed: 99 },
  { id: 15, name: 'المكسيك', city: 'مكسيكو', flag: '🇲🇽', ping: 88, type: 'free', ip: '188.114.66.22', speed: 82 },
];

// ===== INIT =====
window.addEventListener('DOMContentLoaded', () => {
  // Splash → App
  setTimeout(() => {
    document.getElementById('splash').style.opacity = '0';
    document.getElementById('splash').style.transition = 'opacity 0.5s';
    setTimeout(() => {
      document.getElementById('splash').style.display = 'none';
      document.getElementById('app').classList.remove('hidden');
    }, 500);
  }, 2500);

  renderServers();
  document.getElementById('connectBtn').addEventListener('click', toggleConnection);
  document.getElementById('settingsBtn').addEventListener('click', () => { showScreen('settings'); setNav(document.querySelectorAll('.nav-btn')[3]); });
  document.getElementById('premiumBtn').addEventListener('click', () => { showScreen('premium'); setNav(document.querySelectorAll('.nav-btn')[2]); });

  // Restore DNS leak toggle
  document.getElementById('leakToggle').classList.add('active');
});

// ===== NAVIGATION =====
function showScreen(name) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const t = document.getElementById('screen-' + name);
  if (t) t.classList.add('active');
}

function setNav(btn) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
}

// ===== CONNECTION =====
function toggleConnection() {
  if (state.connecting) return;

  if (state.connected) {
    disconnect();
  } else {
    connect();
  }
}

function connect() {
  if (!state.selectedServer) {
    // Auto-select fastest free server
    const fastest = servers.filter(s => s.type === 'free').sort((a, b) => a.ping - b.ping)[0];
    state.selectedServer = fastest;
    updateSelectedServerUI();
  }

  state.connecting = true;
  const btn = document.getElementById('connectBtn');
  btn.classList.add('connecting');
  updateStatus('connecting', 'جاري الاتصال...', 'يرجى الانتظار...');

  setTimeout(() => {
    state.connecting = false;
    state.connected = true;
    btn.classList.remove('connecting');
    btn.classList.add('connected');
    updateStatus('connected', 'متصل', `${state.selectedServer.name} · آمن`);

    // Show IP
    const s = state.selectedServer;
    document.getElementById('ipValue').textContent = s.ip;
    document.getElementById('locationValue').textContent = s.city;
    document.getElementById('protocolValue').textContent = 'WireGuard';
    document.getElementById('ipCard').classList.add('active');

    // Show speed
    document.getElementById('speedRow').classList.add('active');
    startSpeedSimulation();

    showToast(`✅ متصل بـ ${s.name}`);
  }, 2200);
}

function disconnect() {
  state.connected = false;
  const btn = document.getElementById('connectBtn');
  btn.classList.remove('connected', 'connecting');

  updateStatus('', 'غير متصل', 'اضغط للاتصال بـ U VPN');
  document.getElementById('ipCard').classList.remove('active');
  document.getElementById('speedRow').classList.remove('active');
  document.getElementById('ipValue').textContent = '---.---.---.---';
  document.getElementById('locationValue').textContent = '--';
  stopSpeedSimulation();
  showToast('🔴 تم قطع الاتصال');
}

function updateStatus(type, label, sub) {
  const dot = document.getElementById('statusDot');
  dot.className = 'status-indicator';
  if (type) dot.classList.add(type);
  document.getElementById('statusLabel').textContent = label;
  document.getElementById('statusSub').textContent = sub;
}

// ===== SPEED SIMULATION =====
function startSpeedSimulation() {
  const s = state.selectedServer;
  const baseDown = (s.speed / 100) * 15;
  const baseUp = (s.speed / 100) * 8;

  state.speedInterval = setInterval(() => {
    if (!state.connected) { stopSpeedSimulation(); return; }
    const down = (baseDown + (Math.random() - 0.5) * 4).toFixed(1);
    const up = (baseUp + (Math.random() - 0.5) * 2).toFixed(1);
    const ping = s.ping + Math.floor((Math.random() - 0.5) * 10);
    document.getElementById('downloadSpeed').textContent = down;
    document.getElementById('uploadSpeed').textContent = up;
    document.getElementById('pingValue').textContent = Math.max(10, ping);
  }, 1200);
}
function stopSpeedSimulation() {
  clearInterval(state.speedInterval);
  document.getElementById('downloadSpeed').textContent = '0.0';
  document.getElementById('uploadSpeed').textContent = '0.0';
  document.getElementById('pingValue').textContent = '--';
}

// ===== SERVER LIST =====
function renderServers() {
  const q = (document.getElementById('serverSearch')?.value || '').trim().toLowerCase();
  const tab = state.currentTab;
  const list = document.getElementById('serverList');
  list.innerHTML = '';

  const filtered = servers.filter(s => {
    const matchTab = tab === 'all' || s.type === tab;
    const matchQ = !q || s.name.toLowerCase().includes(q) || s.city.toLowerCase().includes(q);
    return matchTab && matchQ;
  });

  if (!filtered.length) {
    list.innerHTML = '<div style="text-align:center;color:var(--text2);padding:30px">لا توجد نتائج</div>';
    return;
  }

  filtered.forEach(s => {
    const el = document.createElement('div');
    el.className = 'server-item' + (state.selectedServer?.id === s.id ? ' selected' : '');
    const pingClass = s.ping < 80 ? 'good' : s.ping < 140 ? 'ok' : 'slow';
    const badge = s.type === 'vip'
      ? '<span class="vip-badge">VIP ⭐</span>'
      : '<span class="free-badge">مجاني</span>';

    el.innerHTML = `
      <div class="si-flag">${s.flag}</div>
      <div class="si-info">
        <div class="si-name">${s.name}</div>
        <div class="si-detail">${s.city} · ${badge}</div>
      </div>
      <div class="si-ping ${pingClass}">${s.ping}ms</div>
    `;
    el.addEventListener('click', () => selectServer(s));
    list.appendChild(el);
  });
}

function filterServers() { renderServers(); }

function filterTab(tab, el) {
  state.currentTab = tab;
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  renderServers();
}

function selectServer(s) {
  if (s.type === 'vip') {
    showToast('⭐ يتطلب اشتراك بريميوم');
    return;
  }
  state.selectedServer = s;
  updateSelectedServerUI();
  showScreen('home');
  setNav(document.querySelectorAll('.nav-btn')[0]);
  renderServers();

  if (state.connected) {
    disconnect();
    setTimeout(() => connect(), 500);
  }
  showToast(`تم اختيار ${s.name}`);
}

function updateSelectedServerUI() {
  const s = state.selectedServer;
  if (!s) return;
  document.getElementById('selectedFlag').textContent = s.flag;
  document.getElementById('selectedName').textContent = s.name;
  document.getElementById('selectedPing').textContent = `${s.city} · ${s.ping}ms`;

  // Update bars
  const bars = document.querySelectorAll('.server-selected .bar');
  const quality = s.ping < 60 ? 4 : s.ping < 100 ? 3 : s.ping < 150 ? 2 : 1;
  bars.forEach((b, i) => b.classList.toggle('active', i < quality));
}

// ===== TOGGLES =====
function toggleKillSwitch() {
  state.killSwitch = !state.killSwitch;
  document.getElementById('killSwitchToggle').classList.toggle('active', state.killSwitch);
  const st = document.getElementById('killSwitchStatus');
  st.textContent = state.killSwitch ? 'مفعّل' : 'معطّل';
  st.className = 'feature-status' + (state.killSwitch ? ' on' : '');
  showToast(state.killSwitch ? '🛡️ Kill Switch مفعّل' : '🛡️ Kill Switch معطّل');
}

function toggleAdBlock() {
  state.adBlock = !state.adBlock;
  document.getElementById('adBlockToggle').classList.toggle('active', state.adBlock);
  const st = document.getElementById('adBlockStatus');
  st.textContent = state.adBlock ? 'مفعّل' : 'معطّل';
  st.className = 'feature-status' + (state.adBlock ? ' on' : '');
  showToast(state.adBlock ? '🚫 حجب الإعلانات مفعّل' : '🚫 حجب الإعلانات معطّل');
}

function toggleAutoConnect() {
  state.autoConnect = !state.autoConnect;
  document.getElementById('autoConnectToggle').classList.toggle('active', state.autoConnect);
}

function toggleSplitTunnel() {
  state.splitTunnel = !state.splitTunnel;
  document.getElementById('splitTunnelToggle').classList.toggle('active', state.splitTunnel);
}

function toggleDNS() {
  state.dns = !state.dns;
  document.getElementById('dnsToggle').classList.toggle('active', state.dns);
}

function toggleLeak() {
  state.dnsLeak = !state.dnsLeak;
  document.getElementById('leakToggle').classList.toggle('active', state.dnsLeak);
}

// ===== PLAN SELECTION =====
function selectPlan(el) {
  document.querySelectorAll('.plan').forEach(p => p.classList.remove('active'));
  el.classList.add('active');
}

// ===== TOAST =====
let toastTimeout;
function showToast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => t.classList.remove('show'), 2500);
}
