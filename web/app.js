/**
 * app.js — Canvas renderer, animation loop, and UI wiring.
 *
 * Depends on maze.js (Maze class) and droid.js (Droid class).
 */

// ── Palette (matches the Swing app) ───────────────────────────────────────
const C = {
  bg:       '#0d1117',
  wall:     '#1c2128',
  empty:    '#0d1117',
  visited:  '#0a2a1a',
  path:     '#14532d',
  droid:    '#00d4ff',
  end:      '#f59e0b',
  start:    '#0f3320',
  border:   '#30363d',
};

// ── State ─────────────────────────────────────────────────────────────────
let maze       = null;
let droid      = null;
let timer      = null;   // setInterval handle

// ── DOM refs ──────────────────────────────────────────────────────────────
const canvas      = document.getElementById('maze-canvas');
const ctx         = canvas.getContext('2d');
const stepsLabel  = document.getElementById('steps-label');
const statusLabel = document.getElementById('status-label');
const startBtn    = document.getElementById('start-btn');
const newBtn      = document.getElementById('new-btn');
const resetBtn    = document.getElementById('reset-btn');
const sizeSelect  = document.getElementById('size-select');
const speedSlider = document.getElementById('speed-slider');

// ── Timing ────────────────────────────────────────────────────────────────
/** Convert slider value (1–20) to millisecond delay, exponentially. */
function getDelay() {
  const v = parseInt(speedSlider.value); // 1 = slowest, 20 = fastest
  return Math.round(20 * Math.pow(500 / 20, (20 - v) / 19));
}

// ── App actions ───────────────────────────────────────────────────────────
function newMaze() {
  stopTimer();
  maze  = new Maze(parseInt(sizeSelect.value));
  droid = null;
  syncCanvasSize();
  draw();
  setStatus('Ready', 'success');
  stepsLabel.textContent = 'Steps: 0';
  setStartBtn('▶\u00a0 Start', 'btn-green');
}

function resetMaze() {
  stopTimer();
  droid = null;
  draw();
  setStatus('Ready', 'success');
  stepsLabel.textContent = 'Steps: 0';
  setStartBtn('▶\u00a0 Start', 'btn-green');
}

function toggleRun() {
  if (timer !== null) {
    stopTimer();
    setStatus('Stopped', 'muted');
    setStartBtn('▶\u00a0 Start', 'btn-green');
  } else {
    startExplorer();
  }
}

function startExplorer() {
  if (!maze) return;

  // Always restart from a fresh droid
  droid = new Droid(maze);
  setStartBtn('⏹\u00a0 Stop', 'btn-stop');
  setStatus('Exploring…', 'droid');

  function tick() {
    const running = droid.step();
    draw();
    stepsLabel.textContent = `Steps: ${droid.stepCount}`;

    if (!running) {
      stopTimer();
      if (droid.found) {
        setStatus(`Exit found in ${droid.stepCount} steps!`, 'success');
        setStartBtn('✓\u00a0 Solved!', 'btn-green');
      } else {
        setStatus('No path found', 'danger');
        setStartBtn('✗\u00a0 No Path', 'btn-dark');
      }
    }
  }

  timer = setInterval(tick, getDelay());
}

function stopTimer() {
  if (timer !== null) { clearInterval(timer); timer = null; }
}

// ── Canvas sizing ─────────────────────────────────────────────────────────
function syncCanvasSize() {
  const wrapper = canvas.parentElement;
  const size    = Math.min(wrapper.clientWidth, 560);
  canvas.width  = size;
  canvas.height = size;
}

// ── Rendering ─────────────────────────────────────────────────────────────
function draw() {
  if (!maze) return;

  // Background
  ctx.fillStyle = C.bg;
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  const dim      = maze.dim;
  const cellSize = Math.floor(canvas.width / dim);
  const gap      = Math.max(1, Math.floor(cellSize / 10));
  const arc      = Math.max(2, Math.floor(cellSize / 5));

  const pathSet = droid ? droid.pathSet  : new Set();
  const visited = droid ? droid.visited  : new Set();
  const pos     = droid ? droid.pos      : null;

  for (let y = 0; y < dim; y++) {
    for (let x = 0; x < dim; x++) {
      const px = x * cellSize + gap;
      const py = y * cellSize + gap;
      const w  = cellSize - 2 * gap;
      const h  = cellSize - 2 * gap;
      if (w < 2 || h < 2) continue;

      const content  = maze.getContent(x, y);
      const key      = `${x},${y}`;
      const isDroid  = pos && pos.x === x && pos.y === y;
      const onPath   = pathSet.has(key);
      const wasVis   = visited.has(key);
      const isStart  = maze.start && maze.start.x === x && maze.start.y === y;
      const isEnd    = content === 'END';

      // ── Cell fill ───────────────────────────────────────────────────
      let fill;
      if      (content === 'BLOCK') fill = C.wall;
      else if (isDroid)             fill = C.droid;
      else if (onPath)              fill = C.path;
      else if (wasVis)              fill = C.visited;
      else if (isEnd)               fill = C.end;
      else if (isStart)             fill = C.start;
      else                          fill = C.empty;

      fillRoundRect(px, py, w, h, arc, fill);

      // ── Cell icon ───────────────────────────────────────────────────
      if (isDroid) {
        drawDroid(px, py, w, h);
      } else if (isEnd) {
        drawSymbol('★', px, py, w, h, '#fef08a');
      } else if (isStart && !wasVis) {
        drawSymbol('S', px, py, w, h, '#86efac');
      }
    }
  }
}

// ── Drawing helpers ───────────────────────────────────────────────────────
function fillRoundRect(x, y, w, h, r, fill) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + w - r, y);
  ctx.arcTo(x + w, y,     x + w, y + r,     r);
  ctx.lineTo(x + w, y + h - r);
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
  ctx.lineTo(x + r, y + h);
  ctx.arcTo(x,     y + h, x,       y + h - r, r);
  ctx.lineTo(x, y + r);
  ctx.arcTo(x,     y,     x + r,   y,         r);
  ctx.closePath();
  ctx.fillStyle = fill;
  ctx.fill();
}

function drawSymbol(symbol, px, py, w, h, color) {
  if (w < 10) return;
  const fontSize = Math.max(8, Math.floor(w * 0.55));
  ctx.font         = `bold ${fontSize}px monospace`;
  ctx.fillStyle    = color;
  ctx.textAlign    = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(symbol, px + w / 2, py + h / 2);
}

function drawDroid(px, py, w, h) {
  const cx = px + w / 2;
  const cy = py + h / 2;

  // Soft glow
  const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, w * 0.8);
  grad.addColorStop(0, 'rgba(0,212,255,0.22)');
  grad.addColorStop(1, 'rgba(0,212,255,0)');
  ctx.beginPath();
  ctx.arc(cx, cy, w * 0.75, 0, Math.PI * 2);
  ctx.fillStyle = grad;
  ctx.fill();

  // Face circle
  const fr = w * 0.30;
  ctx.beginPath();
  ctx.arc(cx, cy, fr, 0, Math.PI * 2);
  ctx.fillStyle = '#ffffff';
  ctx.fill();
  ctx.strokeStyle = C.droid;
  ctx.lineWidth   = Math.max(1, w / 14);
  ctx.stroke();

  // Eyes
  const er = Math.max(1, fr / 5);
  ctx.fillStyle = '#0d1117';
  ctx.beginPath(); ctx.arc(cx - fr * 0.30, cy - fr * 0.20, er, 0, Math.PI * 2); ctx.fill();
  ctx.beginPath(); ctx.arc(cx + fr * 0.30, cy - fr * 0.20, er, 0, Math.PI * 2); ctx.fill();

  // Smile
  if (fr > 8) {
    ctx.beginPath();
    ctx.arc(cx, cy, fr * 0.50, 0.2, Math.PI - 0.2);
    ctx.strokeStyle = C.droid;
    ctx.lineWidth   = Math.max(1, w / 16);
    ctx.stroke();
  }
}

// ── UI helpers ────────────────────────────────────────────────────────────
function setStatus(text, type) {
  statusLabel.textContent = text;
  statusLabel.className   = `status-${type}`;
}

function setStartBtn(text, cls) {
  startBtn.textContent = text;
  startBtn.className   = cls;
}

// ── Event wiring ──────────────────────────────────────────────────────────
newBtn.addEventListener('click', newMaze);
startBtn.addEventListener('click', toggleRun);
resetBtn.addEventListener('click', resetMaze);
sizeSelect.addEventListener('change', newMaze);

speedSlider.addEventListener('input', () => {
  // If currently running, restart interval with new delay
  if (timer !== null) {
    stopTimer();
    startExplorer();
  }
});

window.addEventListener('resize', () => {
  syncCanvasSize();
  draw();
});

// ── Boot ──────────────────────────────────────────────────────────────────
window.addEventListener('DOMContentLoaded', newMaze);
