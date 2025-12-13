package com.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
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
    private boolean showPath = true;

    private boolean dragging = false;
    private int lastMouseX;

    public Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        MAP = MazeGenerator.generate(41, 41);
        escapePath = MazeGenerator.getEscapePath();

        camera = new Camera(1.5, 1.5, 0);
        screen = new Screen(WIDTH, HEIGHT, MAP);
        input = new Input();

        addKeyListener(input);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragging = true;
                lastMouseX = e.getX();
            }
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int dx = e.getX() - lastMouseX;
                    lastMouseX = e.getX();
                    camera.rotate(dx * 0.003);
                }
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

                if (input.togglePath) {
                    showPath = !showPath;
                    input.togglePath = false;
                }

                if (camera.escaped) {
                    JOptionPane.showMessageDialog(null, "탈출!");
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

            // ===== 월드 =====
            g.drawImage(screen.getImage(), 0, 0, null);

            // ===== 미니맵 =====
            drawMiniMap(g);

            // ===== 스태미나 바 =====
            drawStamina(g);

            g.dispose();
            bs.show();

            Toolkit.getDefaultToolkit().sync();
        }
    }

    private void drawMiniMap(Graphics g) {
        int size = 180;
        int cell = size / MAP.length;
        int ox = WIDTH - size - 12;
        int oy = 12;

        g.setColor(new Color(0,0,0,150));
        g.fillRoundRect(ox-6, oy-6, size+12, size+12, 10,10);

        for (int y=0;y<MAP.length;y++) {
            for (int x=0;x<MAP[0].length;x++) {
                if (MAP[y][x]==1) g.setColor(Color.darkGray);
                else if (MAP[y][x]==2) g.setColor(Color.green);
                else g.setColor(Color.LIGHT_GRAY);
                g.fillRect(ox+x*cell, oy+y*cell, cell, cell);
            }
        }

        if (showPath && escapePath != null) {
            g.setColor(Color.RED);
            for (int i=0;i<escapePath.size()-1;i++) {
                Point a = escapePath.get(i);
                Point b = escapePath.get(i+1);
                g.drawLine(
                        ox + a.x*cell + cell/2,
                        oy + a.y*cell + cell/2,
                        ox + b.x*cell + cell/2,
                        oy + b.y*cell + cell/2
                );
            }
        }

        g.setColor(Color.RED);
        g.fillOval(
                ox + (int)(camera.x*cell)-3,
                oy + (int)(camera.y*cell)-3,
                6,6
        );
    }

    // ===== 스태미나 바 =====
    private void drawStamina(Graphics g) {
        int barW = 220;
        int barH = 12;

        int x = WIDTH / 2 - barW / 2;
        int y = HEIGHT - 28;

        double ratio = camera.stamina / Camera.MAX_STAMINA;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x - 4, y - 4, barW + 8, barH + 8, 8, 8);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y, barW, barH);

        g.setColor(ratio > 0.3
                ? new Color(90, 220, 90)
                : new Color(220, 80, 80));
        g.fillRect(x, y, (int) (barW * ratio), barH);//응으으으으으으으으응아니야

        g.setColor(Color.BLACK);
        g.drawRect(x, y, barW, barH);
    }

    public static void main(String[] args) {
        new Game();
    }
}
