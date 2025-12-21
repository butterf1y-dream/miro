// Game.java (전체 수정본 - 회전 감도 줄임)
package com.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.List;

public class Game extends Canvas implements Runnable {

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 720;

    private boolean running = true;
    private Thread thread;

    private Screen screen;
    private Camera camera;
    private Input input;

    private int[][] MAP;
    private List<Point> escapePath;
    private List<Item> items;
    private boolean showPath = true;

    private boolean dragging = false;
    private int lastMouseX;

    // Shift Lock
    private boolean shiftLock = false;
    private Robot robot;

    // Blank Cursor
    private Cursor blankCursor;

    public Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        // Blank Cursor
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank");

        // Robot
        try {
            robot = new Robot();
        } catch (Exception e) {
            System.err.println("Robot 초기화 실패: " + e.getMessage());
        }

        MAP = MazeGenerator.generate(41, 41);
        escapePath = MazeGenerator.getEscapePath();
        items = MazeGenerator.getItems();

        camera = new Camera(1.5, 1.5, 0);
        screen = new Screen(WIDTH, HEIGHT, MAP, items);
        input = new Input();

        addKeyListener(input);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!shiftLock) {
                    dragging = true;
                    lastMouseX = e.getX();
                }
            }
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseMoveHandler(e);
            }
            public void mouseDragged(MouseEvent e) {
                mouseMoveHandler(e);
            }
        });

        JFrame frame = new JFrame("미로");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        requestFocus();
        start();
    }

    private void mouseMoveHandler(MouseEvent e) {
        double sensitivity = 0.002;  // 감도 줄임 (0.003 → 0.002)

        if (shiftLock) {
            int centerX = WIDTH / 2;
            int dx = e.getX() - centerX;
            camera.rotate(dx * sensitivity);
            centerMouse();
        } else if (dragging) {
            int dx = e.getX() - lastMouseX;
            lastMouseX = e.getX();
            camera.rotate(dx * sensitivity);
        }
    }

    private void centerMouse() {
        if (robot != null) {
            Point loc = getLocationOnScreen();
            robot.mouseMove(loc.x + WIDTH / 2, loc.y + HEIGHT / 2);
        }
    }

    public synchronized void start() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        final double ns = 1_000_000_000.0 / 60.0;
        long last = System.nanoTime();
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - last) / ns;
            last = now;

            while (delta >= 1) {
                camera.update(input, MAP);
                camera.collectItems(items);

                if (input.togglePath) {
                    showPath = !showPath;
                    input.togglePath = false;
                }

                if (input.toggleShiftLock) {
                    shiftLock = !shiftLock;
                    input.toggleShiftLock = false;
                    if (shiftLock) {
                        centerMouse();
                        setCursor(blankCursor);
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }

                if (camera.escaped) {
                    JOptionPane.showMessageDialog(null, "탈출 성공!");
                    System.exit(0);
                }

                delta--;
            }

            screen.render(camera);

            BufferStrategy bs = getBufferStrategy();
            if (bs == null) {
                createBufferStrategy(3);
                continue;
            }

            Graphics g = bs.getDrawGraphics();

            g.drawImage(screen.getImage(), 0, 0, null);
            drawMiniMap(g);
            drawStamina(g);

            // 상태 표시
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(shiftLock ? "Shift Lock: ON (L 토글)" : "Shift Lock: OFF (L 토글)", 12, 30);

            // 컨트롤 힌트
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("WASD: 이동 | Space: 스프린트 | P: 경로 | L: Shift Lock", 12, HEIGHT - 60);

            g.dispose();
            bs.show();
            Toolkit.getDefaultToolkit().sync();
        }
    }

    // drawMiniMap, drawStamina (이전과 동일)
    private void drawMiniMap(Graphics g) {
        int size = 180;
        int cell = size / MAP.length;
        int ox = WIDTH - size - 12;
        int oy = 12;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(ox - 6, oy - 6, size + 12, size + 12, 10, 10);

        for (int y = 0; y < MAP.length; y++) {
            for (int x = 0; x < MAP[0].length; x++) {
                if (MAP[y][x] == 1) g.setColor(Color.DARK_GRAY);
                else if (MAP[y][x] == 2) g.setColor(Color.GREEN);
                else g.setColor(Color.LIGHT_GRAY);
                g.fillRect(ox + x * cell, oy + y * cell, cell, cell);

                if (MAP[y][x] == 2) {
                    g.setColor(Color.YELLOW);
                    g.drawRect(ox + x * cell, oy + y * cell, cell - 1, cell - 1);
                    g.drawRect(ox + x * cell + 1, oy + y * cell + 1, cell - 3, cell - 3);
                }
            }
        }

        for (Item it : items) {
            if (it.collected) continue;
            int ix = (int) (it.x * cell);
            int iy = (int) (it.y * cell);
            int color = switch (it.type) {
                case STAMINA -> 0x66FF66;
                case LIGHT -> 0xFFFF66;
                case MAP -> 0x66AAFF;
            };
            g.setColor(new Color(color));
            g.fillOval(ox + ix - 3, oy + iy - 3, 6, 6);
        }

        boolean shouldShowPath = showPath || camera.revealMap;
        if (shouldShowPath && escapePath != null) {
            g.setColor(new Color(255, 0, 0, 180));
            for (int i = 0; i < escapePath.size() - 1; i++) {
                Point a = escapePath.get(i);
                Point b = escapePath.get(i + 1);
                g.drawLine(ox + a.x * cell + cell / 2, oy + a.y * cell + cell / 2,
                        ox + b.x * cell + cell / 2, oy + b.y * cell + cell / 2);
            }
        }

        g.setColor(Color.RED);
        g.fillOval(ox + (int)(camera.x * cell) - 4, oy + (int)(camera.y * cell) - 4, 8, 8);
        g.setColor(Color.WHITE);
        g.drawOval(ox + (int)(camera.x * cell) - 4, oy + (int)(camera.y * cell) - 4, 8, 8);
    }

    private void drawStamina(Graphics g) {
        int barW = 220;
        int barH = 20;
        int x = WIDTH / 2 - barW / 2;
        int y = HEIGHT - 40;

        double ratio = camera.stamina / Camera.MAX_STAMINA;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x - 6, y - 6, barW + 12, barH + 32, 12, 12);

        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(x, y, barW, barH, 8, 8);

        g.setColor(ratio > 0.3 ? new Color(90, 220, 90) : new Color(220, 80, 80));
        g.fillRoundRect(x, y, (int)(barW * ratio), barH, 8, 8);

        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, barW, barH, 8, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String text = String.format("Stamina: %.0f%%", camera.stamina);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + barW / 2 - fm.stringWidth(text) / 2, y + barH + 20);
    }

    public static void main(String[] args) {
        new Game();
    }
}