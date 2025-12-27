// Game.java (전체 수정본 - revealMap 완전 제거 + enum switch 에러 해결)
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

    // 정적 기록 저장소 (MainMenu와 공유)
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

    // 타이머
    private long startTime;
    private double elapsedTime;

    // Shift Lock & Cursor
    private boolean shiftLock = false;
    private Robot robot;
    private Cursor blankCursor;

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

        // Blank Cursor
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

        // 게임 초기화
        // Game.java (몬스터 1마리 초기화 부분 수정)
        // 게임 초기화 부분 (기존 코드 중 해당 부분만 교체)
        MAP = MazeGenerator.generate(41, 41);
        escapePath = MazeGenerator.getEscapePath();
        items = MazeGenerator.getItems();
        Monster monster = MazeGenerator.getMonster();  // 1마리만

        camera = new Camera(1.5, 1.5, 0);
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

                double dt = 1.0 / 60.0;  // 60FPS 고정 dt

                camera.update(input, MAP, dt);
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

                if (camera.escaped) {
                    double time = elapsedTime;

                    RECORDS.add(time);
                    RECORDS.sort(Double::compareTo);
                    if (RECORDS.size() > MAX_RECORDS) {
                        RECORDS.remove(MAX_RECORDS);
                    }

                    DecimalFormat df = new DecimalFormat("0.00");
                    JOptionPane.showMessageDialog(this,
                            "탈출 성공!\n소요 시간: " + df.format(time) + "초",
                            "축하합니다!",
                            JOptionPane.INFORMATION_MESSAGE);

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
        g.drawString("시간: " + df.format(elapsedTime) + "초", WIDTH - 200, 30);

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

        // revealMap 관련 경로 표시 코드 완전 삭제 (더 이상 필요 없음)

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