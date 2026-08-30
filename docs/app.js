// PulseForge Live Simulator & Interactions

document.addEventListener('DOMContentLoaded', () => {
  initLiveSimulator();
  initTabs();
  initCopyButtons();
});

function initLiveSimulator() {
  const ecgCanvas = document.getElementById('ecgCanvas');
  const ppgCanvas = document.getElementById('ppgCanvas');
  if (!ecgCanvas || !ppgCanvas) return;

  const ecgCtx = ecgCanvas.getContext('2d');
  const ppgCtx = ppgCanvas.getContext('2d');

  // Resize canvas according to device pixel ratio
  function resizeCanvas(canvas) {
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * window.devicePixelRatio;
    canvas.height = rect.height * window.devicePixelRatio;
  }

  resizeCanvas(ecgCanvas);
  resizeCanvas(ppgCanvas);

  window.addEventListener('resize', () => {
    resizeCanvas(ecgCanvas);
    resizeCanvas(ppgCanvas);
  });

  const hrSlider = document.getElementById('hrSlider');
  const sysSlider = document.getElementById('sysSlider');
  const hrValLabel = document.getElementById('hrVal');
  const sysValLabel = document.getElementById('sysVal');
  const pttValDisplay = document.getElementById('pttVal');

  const watchSys = document.getElementById('watchSys');
  const watchDia = document.getElementById('watchDia');
  const watchHr = document.getElementById('watchHr');
  const watchPtt = document.getElementById('watchPtt');

  let hr = 72;
  let sys = 120;
  let dia = 80;
  let pttMs = 210;

  function updateMetrics() {
    hr = parseInt(hrSlider.value);
    sys = parseInt(sysSlider.value);
    dia = Math.round(sys * 0.66);

    // Physiological PTT approximation: higher BP & HR -> faster pulse wave -> lower PTT
    pttMs = Math.max(140, Math.min(320, Math.round(340 - (sys * 0.85) - (hr * 0.35))));

    hrValLabel.textContent = `${hr} BPM`;
    sysValLabel.textContent = `${sys}/${dia} mmHg`;
    pttValDisplay.textContent = `${pttMs} ms`;

    if (watchSys) watchSys.textContent = sys;
    if (watchDia) watchDia.textContent = dia;
    if (watchHr) watchHr.textContent = `${hr} BPM`;
    if (watchPtt) watchPtt.textContent = `${pttMs}ms`;
  }

  hrSlider.addEventListener('input', updateMetrics);
  sysSlider.addEventListener('input', updateMetrics);
  updateMetrics();

  // Animation Loop
  let step = 0;
  const ecgPoints = [];
  const ppgPoints = [];
  const maxPoints = 200;

  function animate() {
    const cycleLength = Math.round(3000 / (hr / 60) / 20); // samples per beat at 50fps
    const phase = step % cycleLength;

    // ECG wave with QRS spike
    let ecgVal = 0;
    if (phase === 0) ecgVal = -0.15; // Q
    else if (phase === 1) ecgVal = 1.0; // R peak
    else if (phase === 2) ecgVal = -0.3; // S
    else if (phase > 8 && phase < 18) {
      ecgVal = Math.sin((phase - 8) * Math.PI / 10) * 0.25; // T wave
    } else {
      ecgVal = (Math.random() - 0.5) * 0.02; // Baseline noise
    }

    // PPG pulse wave delayed by PTT phase
    const pttSamples = Math.round((pttMs / 1000) * 50); // PTT translated to sample delay
    const ppgPhase = (step - pttSamples + cycleLength * 10) % cycleLength;
    let ppgVal = 0;
    if (ppgPhase >= 0 && ppgPhase < cycleLength * 0.75) {
      const pRatio = ppgPhase / (cycleLength * 0.75);
      ppgVal = Math.sin(pRatio * Math.PI) * 0.8 + (Math.sin(pRatio * 2 * Math.PI) * 0.15);
    }

    ecgPoints.push(ecgVal);
    ppgPoints.push(ppgVal);
    if (ecgPoints.length > maxPoints) ecgPoints.shift();
    if (ppgPoints.length > maxPoints) ppgPoints.shift();

    drawWaveform(ecgCtx, ecgCanvas, ecgPoints, '#FF5252');
    drawWaveform(ppgCtx, ppgCanvas, ppgPoints, '#00E676');

    step++;
    requestAnimationFrame(animate);
  }

  function drawWaveform(ctx, canvas, points, strokeColor) {
    const w = canvas.width;
    const h = canvas.height;
    ctx.clearRect(0, 0, w, h);

    // Draw Grid
    ctx.strokeStyle = '#101726';
    ctx.lineWidth = 1;
    for (let x = 0; x < w; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = 0; y < h; y += 30) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }

    if (points.length < 2) return;

    ctx.strokeStyle = strokeColor;
    ctx.lineWidth = 3 * window.devicePixelRatio;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.shadowColor = strokeColor;
    ctx.shadowBlur = 8;

    const stepX = w / (maxPoints - 1);
    ctx.beginPath();

    points.forEach((val, idx) => {
      const x = idx * stepX;
      // normalize centered
      const y = (h / 2) - (val * (h * 0.4));
      if (idx === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });

    ctx.stroke();
    ctx.shadowBlur = 0;
  }

  animate();
}

function initTabs() {
  const tabBtns = document.querySelectorAll('.tab-btn');
  const tabPanes = document.querySelectorAll('.tab-pane');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      tabPanes.forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.getAttribute('data-tab');
      const targetPane = document.getElementById(targetId);
      if (targetPane) targetPane.classList.add('active');
    });
  });
}

function initCopyButtons() {
  const copyBtns = document.querySelectorAll('.copy-btn');
  copyBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const code = btn.parentElement.innerText.replace('Copy', '').trim();
      navigator.clipboard.writeText(code).then(() => {
        const originalText = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => btn.textContent = originalText, 2000);
      });
    });
  });
}
