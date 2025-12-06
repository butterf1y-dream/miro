package com.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

public class Game extends Canvas implements Runnable {
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 720;

    private Thread thread;
    private boolean running = false;

    private BufferedImage screenImage;
    private Screen screen;
    private Camera camera;
    private Input input;

    // map will be generated
    private int[][] MAP;
    private final int MAP_W = 41; // odd recommended
    private final int MAP_H = 41;

    // mouse drag state
    private boolean dragging = false;
    private int lastMouseX;

    public Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        screenImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        // generate maze
        MAP = MazeGenerator.generate(MAP_W, MAP_H);

        // place an EXIT tile (value 2) at far corner
        MAP[MAP_H - 2][MAP_W - 2] = 2;

        // create camera at entrance (1,1)
        camera = new Camera(1.5, 1.5, 0.0);

        screen = new Screen(WIDTH, HEIGHT, MAP);
        input = new Input();

        addKeyListener(input);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                lastMouseX = e.getX();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int dx = e.getX() - lastMouseX;
                    lastMouseX = e.getX();
                    camera.rotate(dx * 0.0035); // sensitivity
                }
            }
        });

        JFrame frame = new JFrame("Escape Maze — Dark Vision Minimap");
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
        running = true;
        thread = new Thread(this, "GameThread");
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        try { thread.join(); } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        final double ns = 1000000000.0 / 60.0;
        long last = System.nanoTime();
        double delta = 0;
        while (running) {
            long now = System.nanoTime();
            delta += (now - last) / ns;
            last = now;
            while (delta >= 1) {
                camera.update(input, MAP);
                delta--;
            }
            screen.render(camera);
            BufferStrategy bs = getBufferStrategy();
            if (bs == null) { createBufferStrategy(3); continue; }
            Graphics g = bs.getDrawGraphics();

            // draw world
            g.drawImage(screen.getImage(), 0, 0, null);

            // draw HUD / minimap (top-right)
            drawMiniMap(g);

            // crosshair
            drawCrosshair(g);

            g.dispose();
            bs.show();

            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
    }

    private void drawCrosshair(Graphics g) {
        int cx = WIDTH / 2;
        int cy = HEIGHT / 2;
        g.setColor(Color.WHITE);
        g.drawLine(cx - 8, cy, cx - 3, cy);
        g.drawLine(cx + 3, cy, cx + 8, cy);
        g.drawLine(cx, cy - 8, cx, cy - 3);
        g.drawLine(cx, cy + 3, cx, cy + 8);
    }

    private void drawMiniMap(Graphics g) {
        int mapW = MAP[0].length;
        int mapH = MAP.length;
        int miniSize = 180; // square minimap size
        int px = WIDTH - miniSize - 12; // top-right margin
        int py = 12;
        int cell = Math.max(2, miniSize / mapW);

        // dark background
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(px - 6, py - 6, miniSize + 12, miniSize + 12, 10, 10);

        // vision radius in tiles
        int vision = 6;

        // draw tiles, but only brighten tiles within vision radius
        for (int my = 0; my < mapH; my++) {
            for (int mx = 0; mx < mapW; mx++) {
                int dx = mx - (int) camera.x;
                int dy = my - (int) camera.y;
                double d = Math.sqrt(dx * dx + dy * dy);
                int sx = px + mx * cell;
                int sy = py + my * cell;

                if (d <= vision) {
                    // visible — color by tile
                    if (MAP[my][mx] == 1) g.setColor(new Color(120,120,120)); // wall
                    else if (MAP[my][mx] == 2) g.setColor(new Color(255, 200, 30)); // exit
                    else g.setColor(new Color(200,200,200)); // path
                } else {
                    // dark / hidden
                    g.setColor(new Color(30, 30, 30));
                }
                g.fillRect(sx, sy, cell, cell);
            }
        }

        // draw player as triangle showing facing direction
        int centerX = px + (int) (camera.x * cell);
        int centerY = py + (int) (camera.y * cell);
        int dirX = (int) (Math.cos(camera.rot) * (cell * 1.2));
        int dirY = (int) (Math.sin(camera.rot) * (cell * 1.2));
        g.setColor(Color.RED);
        int[] xs = { centerX + dirX, centerX - dirY / 2, centerX + dirY / 2 };
        int[] ys = { centerY + dirY, centerY + dirX / 2, centerY - dirX / 2 };
        g.fillPolygon(xs, ys, 3);

        // border
        g.setColor(Color.DARK_GRAY);
        g.drawRoundRect(px - 6, py - 6, miniSize + 12, miniSize + 12, 10, 10);
    }

    public static void main(String[] args) {
        new Game();
    }
}
