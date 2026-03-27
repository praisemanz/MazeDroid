package mazeUI;

import mazePD.Coordinates;
import mazePD.Droid;
import mazePD.Maze;
import mazePD.Maze.Content;
import mazePD.Maze.MazeMode;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MazeApp — modern dark-theme Swing UI for the R2D2 Maze Explorer.
 *
 * Layout:
 *   NORTH  — header bar (title + size selector)
 *   CENTER — maze grid panel + legend
 *   SOUTH  — stats bar + control buttons
 *
 * The droid exploration runs on a daemon thread. All UI mutations are
 * dispatched to the EDT via SwingUtilities.invokeLater.
 */
public class MazeApp extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    static final Color C_BG         = new Color(0x0d1117);
    static final Color C_PANEL      = new Color(0x161b22);
    static final Color C_BORDER     = new Color(0x30363d);
    static final Color C_WALL       = new Color(0x1c2128);
    static final Color C_EMPTY      = new Color(0x0d1117);
    static final Color C_VISITED    = new Color(0x0a2a1a);
    static final Color C_PATH       = new Color(0x14532d);
    static final Color C_DROID      = new Color(0x00d4ff);
    static final Color C_PORTAL_DN  = new Color(0x9333ea);
    static final Color C_PORTAL_UP  = new Color(0x2563eb);
    static final Color C_END        = new Color(0xf59e0b);
    static final Color C_TEXT       = new Color(0xe6edf3);
    static final Color C_MUTED      = new Color(0x8b949e);
    static final Color C_SUCCESS    = new Color(0x22c55e);
    static final Color C_WARN       = new Color(0xf59e0b);
    static final Color C_DANGER     = new Color(0xef4444);
    static final Color C_BTN_GREEN  = new Color(0x238636);
    static final Color C_BTN_STOP   = new Color(0x9b2c2c);
    static final Color C_BTN_DARK   = new Color(0x21262d);
    static final Color C_BTN_MID    = new Color(0x30363d);

    // ── App state ─────────────────────────────────────────────────────────────
    private Maze              maze;
    private int               dim    = 8;
    private final int         levels = 1;

    // All mutations happen on EDT; no locking needed.
    private final List<String> pathList    = new ArrayList<>();
    private final Set<String>  pathSet     = new HashSet<>();
    private final Set<String>  visitedSet  = new HashSet<>();
    private Coordinates        droidPos;
    private int                stepCount   = 0;
    private int                currentLevel= 0;

    private Thread          explorerThread;
    private volatile boolean running      = false;
    private volatile int     stepDelay    = 250;

    // ── Components ────────────────────────────────────────────────────────────
    private MazePanel mazePanel;
    private JLabel    stepsLabel, levelLabel, statusLabel;
    private JButton   startBtn, newMazeBtn, resetBtn;

    // ─────────────────────────────────────────────────────────────────────────

    public MazeApp() {
        super("R2D2 Maze Explorer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(C_BG);
        buildUI();
        newMaze();
        pack();
        setMinimumSize(new Dimension(560, 680));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = panel(new BorderLayout());
        p.setBorder(compound(
            matte(0, 0, 1, 0, C_BORDER),
            empty(14, 20, 14, 20)
        ));

        JLabel title = new JLabel("  R2D2   MAZE   EXPLORER");
        title.setFont(new Font("Monospaced", Font.BOLD, 18));
        title.setForeground(C_DROID);
        p.add(title, BorderLayout.WEST);

        JPanel right = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(muted("Size:"));

        String[] sizes = {"6×6", "8×8", "10×10", "12×12"};
        int[]    dims  = { 6,    8,     10,      12     };
        JComboBox<String> sizeBox = styledCombo(sizes, 1);
        sizeBox.addActionListener(e -> {
            dim = dims[sizeBox.getSelectedIndex()];
            newMaze();
        });
        right.add(sizeBox);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        mazePanel = new MazePanel();
        mazePanel.setPreferredSize(new Dimension(500, 500));

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(C_BG);
        wrapper.setBorder(empty(20, 20, 8, 20));
        wrapper.add(mazePanel);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(C_BG);
        center.add(wrapper,       BorderLayout.CENTER);
        center.add(buildLegend(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        p.setBackground(C_BG);
        addLegend(p, C_WALL,      "Wall");
        addLegend(p, C_EMPTY,     "Open");
        addLegend(p, C_PATH,      "Path");
        addLegend(p, C_DROID,     "R2D2");
        addLegend(p, C_PORTAL_DN, "Portal↓");
        addLegend(p, C_PORTAL_UP, "Portal↑");
        addLegend(p, C_END,       "Exit");
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = panel(new BorderLayout());
        footer.setBorder(matte(1, 0, 0, 0, C_BORDER));

        // ── Stats row ──────────────────────────────────────────────────────
        JPanel stats = panel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        stats.setBorder(empty(0, 20, 0, 20));

        stepsLabel  = statLabel("Steps: 0");
        levelLabel  = statLabel("Level: 0/0");
        statusLabel = statLabel("Ready");
        statusLabel.setForeground(C_SUCCESS);

        JLabel speedLbl = muted("  Speed:");
        JSlider speedSlider = new JSlider(30, 700, stepDelay);
        speedSlider.setInverted(true);   // right = faster
        speedSlider.setBackground(C_PANEL);
        speedSlider.setForeground(C_MUTED);
        speedSlider.setPreferredSize(new Dimension(110, 22));
        speedSlider.addChangeListener(e -> stepDelay = speedSlider.getValue());

        stats.add(stepsLabel);
        stats.add(pipe());
        stats.add(levelLabel);
        stats.add(pipe());
        stats.add(statusLabel);
        stats.add(speedLbl);
        stats.add(speedSlider);
        footer.add(stats, BorderLayout.NORTH);

        // ── Buttons row ────────────────────────────────────────────────────
        JPanel btns = panel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        newMazeBtn = btn("⟳  New Maze", C_BTN_MID,   C_TEXT);
        startBtn   = btn("▶  Start",    C_BTN_GREEN,  Color.WHITE);
        resetBtn   = btn("↺  Reset",    C_BTN_DARK,   C_MUTED);

        newMazeBtn.addActionListener(e -> newMaze());
        startBtn.addActionListener(e -> toggleRun());
        resetBtn.addActionListener(e -> resetMaze());

        btns.add(newMazeBtn);
        btns.add(startBtn);
        btns.add(resetBtn);
        footer.add(btns, BorderLayout.SOUTH);

        return footer;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void newMaze() {
        stopExplorer();
        maze = new Maze(dim, levels, MazeMode.NORMAL);
        clearState();
        mazePanel.repaint();
        refreshStats();
        setStartBtn("▶  Start", C_BTN_GREEN);
        setStatus("Ready", C_SUCCESS);
    }

    private void resetMaze() {
        stopExplorer();
        clearState();
        mazePanel.repaint();
        refreshStats();
        setStartBtn("▶  Start", C_BTN_GREEN);
        setStatus("Ready", C_SUCCESS);
    }

    private void toggleRun() {
        if (running) {
            stopExplorer();
            setStartBtn("▶  Start", C_BTN_GREEN);
            setStatus("Stopped", C_MUTED);
        } else {
            startExplorer();
        }
    }

    private void startExplorer() {
        // Reset visual state so re-runs start clean
        clearState();
        mazePanel.repaint();

        running = true;
        setStartBtn("⏹  Stop", C_BTN_STOP);
        setStatus("Exploring…", C_DROID);

        Droid droid = new Droid("R2D2");
        droid.setStepDelay(() -> stepDelay);

        droid.setStepListener((pos, step, backtrack) ->
            SwingUtilities.invokeLater(() -> {
                if (!running || pos == null) return;
                String key = pos.getX() + "," + pos.getY() + "," + pos.getZ();
                visitedSet.add(key);
                if (backtrack) {
                    if (!pathList.isEmpty()) {
                        String removed = pathList.remove(pathList.size() - 1);
                        pathSet.remove(removed);
                    }
                } else {
                    pathList.add(key);
                    pathSet.add(key);
                }
                droidPos     = pos;
                stepCount    = step;
                currentLevel = pos.getZ();
                refreshStats();
                mazePanel.repaint();
            })
        );

        droid.setCompleteListener((found, total) ->
            SwingUtilities.invokeLater(() -> {
                if (!running) return;
                running   = false;
                stepCount = total;
                refreshStats();
                if (found) {
                    setStartBtn("✓  Solved!", C_BTN_GREEN);
                    setStatus("Exit found in " + total + " steps!", C_SUCCESS);
                } else {
                    setStartBtn("✗  No Path", C_BTN_DARK);
                    setStatus("No path found", C_DANGER);
                }
            })
        );

        explorerThread = new Thread(() -> droid.exploreMaze(maze), "maze-explorer");
        explorerThread.setDaemon(true);
        explorerThread.start();
    }

    private void stopExplorer() {
        running = false;
        if (explorerThread != null) {
            explorerThread.interrupt();
            explorerThread = null;
        }
    }

    private void clearState() {
        pathList.clear();
        pathSet.clear();
        visitedSet.clear();
        droidPos     = null;
        stepCount    = 0;
        currentLevel = 0;
        running      = false;
    }

    private void refreshStats() {
        stepsLabel.setText("Steps: " + stepCount);
        levelLabel.setText("Level: " + currentLevel + "/" + (levels - 1));
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void setStartBtn(String text, Color bg) {
        startBtn.setText(text);
        startBtn.setBackground(bg);
    }

    // ── MazePanel ─────────────────────────────────────────────────────────────

    /**
     * Custom panel that renders the maze grid. Reads visual state directly
     * from the enclosing MazeApp fields (all on EDT, so no locking needed).
     */
    class MazePanel extends JPanel {

        MazePanel() { setBackground(C_BG); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maze == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size     = Math.min(getWidth(), getHeight()) - 8;
            int cellSize = size / dim;
            int ox       = (getWidth()  - cellSize * dim) / 2;
            int oy       = (getHeight() - cellSize * dim) / 2;
            int gap      = Math.max(1, cellSize / 10);
            int arc      = Math.max(2, cellSize / 5);

            Coordinates startCoord = maze.getMazeStartCoord();

            for (int y = 0; y < dim; y++) {
                for (int x = 0; x < dim; x++) {
                    int px = ox + x * cellSize + gap;
                    int py = oy + y * cellSize + gap;
                    int w  = cellSize - 2 * gap;
                    int h  = cellSize - 2 * gap;
                    if (w < 2 || h < 2) continue;

                    Content content    = maze.getContent(x, y, currentLevel);
                    String  key        = x + "," + y + "," + currentLevel;
                    boolean isDroid    = droidPos != null
                                        && droidPos.getX() == x
                                        && droidPos.getY() == y
                                        && droidPos.getZ() == currentLevel;
                    boolean isOnPath   = pathSet.contains(key);
                    boolean wasVisited = visitedSet.contains(key);
                    boolean isStart    = startCoord != null
                                        && startCoord.getX() == x
                                        && startCoord.getY() == y
                                        && startCoord.getZ() == currentLevel;

                    // ── Cell fill ──────────────────────────────────────────
                    Color fill;
                    if (content == Content.BLOCK) {
                        fill = C_WALL;
                    } else if (isDroid) {
                        fill = C_DROID;
                    } else if (isOnPath) {
                        fill = C_PATH;
                    } else if (wasVisited) {
                        fill = C_VISITED;
                    } else {
                        switch (content) {
                            case PORTAL_DN: fill = C_PORTAL_DN; break;
                            case PORTAL_UP: fill = C_PORTAL_UP; break;
                            case END:       fill = C_END;        break;
                            default:        fill = isStart ? new Color(0x0f3320) : C_EMPTY;
                        }
                    }

                    g2.setColor(fill);
                    g2.fillRoundRect(px, py, w, h, arc, arc);

                    // ── Subtle border for wall cells ───────────────────────
                    if (content == Content.BLOCK) {
                        g2.setColor(new Color(0x30363d));
                        g2.setStroke(new BasicStroke(0.5f));
                        g2.drawRoundRect(px, py, w, h, arc, arc);
                    }

                    // ── Cell icon / droid face ─────────────────────────────
                    if (isDroid) {
                        drawDroid(g2, px, py, w, h);
                    } else if (content == Content.END) {
                        drawIcon(g2, "★", wasVisited || isOnPath
                            ? new Color(0xfef08a, true) : new Color(0xfef08a),
                            px, py, w, h);
                    } else if (!wasVisited && !isOnPath) {
                        if (content == Content.PORTAL_DN) drawIcon(g2, "↓", new Color(0xe9d5ff), px, py, w, h);
                        else if (content == Content.PORTAL_UP) drawIcon(g2, "↑", new Color(0xbfdbfe), px, py, w, h);
                        else if (isStart) drawIcon(g2, "S", new Color(0x86efac), px, py, w, h);
                    }
                }
            }
            g2.dispose();
        }

        private void drawIcon(Graphics2D g2, String symbol, Color color,
                              int px, int py, int w, int h) {
            if (w < 10) return;
            Font f  = new Font("Monospaced", Font.BOLD, Math.max(8, w * 55 / 100));
            g2.setFont(f);
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            int tx = px + (w - fm.stringWidth(symbol)) / 2;
            int ty = py + (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(symbol, tx, ty);
        }

        private void drawDroid(Graphics2D g2, int px, int py, int w, int h) {
            // Outer glow
            int glow = w / 3;
            g2.setColor(new Color(0, 212, 255, 45));
            g2.fillOval(px - glow / 2, py - glow / 2, w + glow, h + glow);

            // Face circle
            int face = w * 65 / 100;
            int fx   = px + (w - face) / 2;
            int fy   = py + (h - face) / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(fx, fy, face, face);
            g2.setColor(C_DROID);
            g2.setStroke(new BasicStroke(Math.max(1.0f, w / 14.0f)));
            g2.drawOval(fx, fy, face, face);

            // Eyes
            int eyeR = Math.max(1, face / 7);
            int eyeY = fy + face * 38 / 100;
            g2.setColor(new Color(0x0d1117));
            g2.fillOval(fx + face * 28 / 100 - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);
            g2.fillOval(fx + face * 68 / 100 - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);

            // Smile arc
            if (face > 16) {
                int smileW = face * 40 / 100;
                int smileX = fx + (face - smileW) / 2;
                int smileY = fy + face * 55 / 100;
                g2.setColor(C_DROID);
                g2.setStroke(new BasicStroke(Math.max(1.0f, w / 16.0f)));
                g2.drawArc(smileX, smileY, smileW, face * 20 / 100, 0, -180);
            }
        }
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(C_PANEL);
        return p;
    }

    private JLabel statLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setForeground(C_TEXT);
        return l;
    }

    private JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setForeground(C_MUTED);
        return l;
    }

    private JLabel pipe() {
        JLabel l = new JLabel("  |  ");
        l.setForeground(C_BORDER);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return l;
    }

    private JButton btn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Monospaced", Font.BOLD, 13));
        b.setBorder(empty(8, 18, 8, 18));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JComboBox<String> styledCombo(String[] items, int selected) {
        JComboBox<String> box = new JComboBox<>(items);
        box.setSelectedIndex(selected);
        box.setBackground(C_BTN_DARK);
        box.setForeground(C_TEXT);
        box.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return box;
    }

    private void addLegend(JPanel panel, Color color, String label) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setBackground(C_BG);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 3, 12, 12, 4, 4);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(12, 18));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(C_MUTED);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));

        item.add(dot);
        item.add(lbl);
        panel.add(item);
    }

    // ── Border shortcuts ──────────────────────────────────────────────────────

    private static Border empty(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    private static Border matte(int t, int l, int b, int r, Color c) {
        return BorderFactory.createMatteBorder(t, l, b, r, c);
    }

    private static Border compound(Border outside, Border inside) {
        return BorderFactory.createCompoundBorder(outside, inside);
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(MazeApp::new);
    }
}
