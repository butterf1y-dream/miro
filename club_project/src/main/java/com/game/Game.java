package com.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class Game extends JFrame implements Runnable {

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 720;

    public static final ArrayList<Double> RECORDS = new ArrayList<>();
    private static final int MAX_RECORDS = 5;

    private Canvas canvas;
    private boolean running = false;
    private Thread thread;

    private Screen screen;
    private Camera camera;
    private Input input;

    private int[][] MAP;
    private List<Point> escapePath;
    private List<Item> items;

    private long startTime;
    private double elapsedTime;

    private boolean shiftLock = false;
    private Robot robot;
    private Cursor blankCursor;

    private boolean showPath = false;  // 플레이어에게만 경로 표시 토글

    public Game() {
        setTitle("미로 탈출");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);

        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        canvas.setBackground(Color.BLACK);

        add(canvas);
        pack();
        setVisible(true);

        canvas.createBufferStrategy(3);

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank");

        try {
            robot = new Robot();
        } catch (Exception e) {
            System.err.println("Robot 초기화 실패: " + e.getMessage());
        }

        input = new Input();
        canvas.addKeyListener(input);

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseMoveHandler(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoveHandler(e);
            }
        });

        canvas.requestFocusInWindow();

        MAP = MazeGenerator.generate(41, 41);
        escapePath = MazeGenerator.getEscapePath();
        items = MazeGenerator.getItems();

        camera = new Camera(1.5, 1.5, 0);
        Monster monster = MazeGenerator.getMonster();
        camera.setMonster(monster);

        screen = new Screen(WIDTH, HEIGHT, MAP, items);

        startTime = System.nanoTime();
        elapsedTime = 0;

        running = true;
        thread = new Thread(this);
        thread.start();
    }

    private void mouseMoveHandler(MouseEvent e) {
        double sensitivity = 0.002;

        if (shiftLock) {
            int centerX = WIDTH / 2;
            int dx = e.getX() - centerX;
            camera.rotate(dx * sensitivity);
            centerMouse();
        }
    }

    private void centerMouse() {
        if (robot != null) {
            Point loc = canvas.getLocationOnScreen();
            robot.mouseMove(loc.x + WIDTH / 2, loc.y + HEIGHT / 2);
        }
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
                elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;

                double dt = 1.0 / 60.0;

                // 몬스터는 항상 escapePath를 따라감, showPath는 플레이어 시야만
                camera.update(input, MAP, dt, escapePath, showPath);
                camera.collectItems(items);

                if (input.toggleShiftLock) {
                    shiftLock = !shiftLock;
                    input.toggleShiftLock = false;
                    if (shiftLock) {
                        centerMouse();
                        canvas.setCursor(blankCursor);
                    } else {
                        canvas.setCursor(Cursor.getDefaultCursor());
                    }
                }

                if (input.togglePath) {
                    showPath = !showPath;
                    input.togglePath = false;
                }

                if (camera.escaped) {
                    double time = elapsedTime;
                    RECORDS.add(time);
                    RECORDS.sort(Double::compareTo);
                    if (RECORDS.size() > MAX_RECORDS) RECORDS.remove(MAX_RECORDS);

                    DecimalFormat df = new DecimalFormat("0.00");
                    JOptionPane.showMessageDialog(this,
                            "탈출 성공!\n소요 시간: " + df.format(time) + "초",
                            "축하합니다!",
                            JOptionPane.INFORMATION_MESSAGE);

                    running = false;
                } else if (camera.gameOver) {
                    JOptionPane.showMessageDialog(this,
                            "몬스터에게 잡혔습니다!\n게임 오버",
                            "Game Over",
                            JOptionPane.ERROR_MESSAGE);

                    running = false;
                }

                delta--;
            }

            render();
            Toolkit.getDefaultToolkit().sync();
        }

        dispose();
        SwingUtilities.invokeLater(() -> new MainMenu());
    }

    private void render() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) return;

        Graphics g = bs.getDrawGraphics();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        screen.render(camera);
        g.drawImage(screen.getImage(), 0, 0, null);

        drawMiniMap(g);
        drawStamina(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        DecimalFormat df = new DecimalFormat("0.00");
        g.drawString("시간: " + df.format(elapsedTime) + "초", WIDTH - 200, HEIGHT - 30);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(shiftLock ? "Shift Lock: ON (L 토글)" : "Shift Lock: OFF (L 토글)", 12, 30);

        g.dispose();
        bs.show();
    }

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
                }
            }
        }

        for (Item it : items) {
            if (it.collected) continue;
            int ix = (int)(it.x * cell);
            int iy = (int)(it.y * cell);
            int color = switch (it.type) {
                case STAMINA -> 0x66FF66;
                case FREEZE -> 0xFFFF66;
                case FLASHLIGHT -> 0x66AAFF;
            };
            g.setColor(new Color(color));
            g.fillOval(ox + ix - 3, oy + iy - 3, 6, 6);
        }

        Monster mon = camera.getMonster();
        if (mon != null && mon.spawned) {
            int mx = (int)(mon.x * cell);
            int my = (int)(mon.y * cell);
            g.setColor(Color.RED);
            g.drawLine(ox + mx - 5, oy + my - 5, ox + mx + 5, oy + my + 5);
            g.drawLine(ox + mx + 5, oy + my - 5, ox + mx - 5, oy + my + 5);
            g.setColor(Color.WHITE);
            g.drawOval(ox + mx - 6, oy + my - 6, 12, 12);
        }

        // 플레이어에게만 showPath일 때 경로 표시
        if (showPath && escapePath != null) {
            g.setColor(new Color(255, 0, 0, 180));
            for (int i = 0; i < escapePath.size() - 1; i++) {
                Point a = escapePath.get(i);
                Point b = escapePath.get(i + 1);
                g.drawLine(ox + a.x * cell + cell/2, oy + a.y * cell + cell/2,
                        ox + b.x * cell + cell/2, oy + b.y * cell + cell/2);
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
        g.drawString(text, x + barW / 2 - fm.stringWidth(text)/2, y + barH + 20);
    }
}