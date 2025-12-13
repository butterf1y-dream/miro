package com.game;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Screen {

    private int width, height;
    private int[][] map;

    private BufferedImage img;
    private int[] pixels;

    // 시야각 (지금 값 유지)
    private static final double FOV = 0.35;

    // 공포 연출 파라미터
    private static final double MAX_VIEW_DIST = 20.0;
    private static final double DARKNESS_STRENGTH = 1.15;

    public Screen(int w, int h, int[][] map) {
        this.width = w;
        this.height = h;
        this.map = map;

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    public BufferedImage getImage() {
        return img;
    }

    public void render(Camera cam) {

        // --- 배경 (어두운 하늘 / 바닥) ---
        int skyColor = 0x0A0E1A;    // 매우 어두운 남색
        int floorColor = 0x080808;  // 거의 검정

        for (int y = 0; y < height; y++) {
            int col = (y < height / 2) ? skyColor : floorColor;
            for (int x = 0; x < width; x++) {
                pixels[x + y * width] = col;
            }
        }

        double cos = Math.cos(cam.rot);
        double sin = Math.sin(cam.rot);

        // --- 레이캐스팅 ---
        for (int x = 0; x < width; x++) {

            double cameraX = 2.0 * x / width - 1.0;

            double rayX = cos + cameraX * (-sin) * FOV;
            double rayY = sin + cameraX * ( cos) * FOV;

            double len = Math.sqrt(rayX * rayX + rayY * rayY);
            rayX /= len;
            rayY /= len;

            double dist = 0;
            int hit = 0;

            while (dist < MAX_VIEW_DIST) {
                dist += 0.05;

                int mx = (int) (cam.x + rayX * dist);
                int my = (int) (cam.y + rayY * dist);

                if (mx < 0 || my < 0 || mx >= map[0].length || my >= map.length)
                    break;

                hit = map[my][mx];
                if (hit != 0) break;
            }

            // fisheye 보정
            dist *= Math.cos(cameraX * 0.6);

            int lineHeight = (int) (height / (dist + 0.0001));
            int drawStart = height / 2 - lineHeight / 2;
            int drawEnd = height / 2 + lineHeight / 2;

            if (drawStart < 0) drawStart = 0;
            if (drawEnd >= height) drawEnd = height - 1;

            // 벽 기본 색
            int baseColor = (hit == 2) ? 0x1FFF1F : 0x888888;

            // --- 거리 기반 어둠 (Fog) ---
            double darkness = Math.min(1.0, dist / MAX_VIEW_DIST);
            darkness = Math.pow(darkness, DARKNESS_STRENGTH);

            int r = (int)(((baseColor >> 16) & 0xFF) * (1.0 - darkness));
            int g = (int)(((baseColor >> 8) & 0xFF) * (1.0 - darkness));
            int b = (int)((baseColor & 0xFF) * (1.0 - darkness));

            int shaded = (r << 16) | (g << 8) | b;

            for (int y = drawStart; y < drawEnd; y++) {
                pixels[x + y * width] = shaded;
            }
        }

        // --- 비네트 (화면 가장자리 어둡게) ---
        applyVignette();
    }

    // 공포게임 핵심 요소
    private void applyVignette() {
        int cx = width / 2;
        int cy = height / 2;
        double maxDist = Math.sqrt(cx * cx + cy * cy);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int dx = x - cx;
                int dy = y - cy;
                double d = Math.sqrt(dx * dx + dy * dy) / maxDist;

                double v = Math.min(1.0, d * 1.25);

                int idx = x + y * width;
                int col = pixels[idx];

                int r = (int)(((col >> 16) & 0xFF) * (1.0 - v));
                int g = (int)(((col >> 8) & 0xFF) * (1.0 - v));
                int b = (int)((col & 0xFF) * (1.0 - v));

                pixels[idx] = (r << 16) | (g << 8) | b;
            }
        }
    }
}
