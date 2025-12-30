package com.game;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;

public class Screen {

    private int width, height;
    private int[][] map;
    private List<Item> items;

    private BufferedImage img;
    private int[] pixels;

    private static final double FOV = 0.6;
    private static final double MAX_DIST = 15.0;

    public Screen(int w, int h, int[][] map, List<Item> items) {
        this.width = w;
        this.height = h;
        this.map = map;
        this.items = items;

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    public BufferedImage getImage() {
        return img;
    }

    public void render(Camera cam) {
        float[] zBuffer = new float[width];
        for (int i = 0; i < width; i++) zBuffer[i] = Float.MAX_VALUE;

        // 배경 완전 검정
        for (int i = 0; i < pixels.length; i++) pixels[i] = 0x000000;

        double cos = Math.cos(cam.rot);
        double sin = Math.sin(cam.rot);

        // 벽 raycasting
        for (int x = 0; x < width; x++) {
            double cx = 2.0 * x / width - 1.0;
            double rayX = cos + cx * -sin * FOV;
            double rayY = sin + cx * cos * FOV;

            double dist = 0.0;
            int hit = 0;
            double hitX = 0, hitY = 0;

            while (dist < MAX_DIST) {
                dist += 0.03;
                int mx = (int) (cam.x + rayX * dist);
                int my = (int) (cam.y + rayY * dist);
                if (mx < 0 || my < 0 || mx >= map[0].length || my >= map.length) break;
                hit = map[my][mx];
                hitX = mx + 0.5;
                hitY = my + 0.5;
                if (hit != 0) break;
            }

            double wallDist = Math.hypot(cam.x - hitX, cam.y - hitY);
            zBuffer[x] = (float) Math.min(dist, wallDist);

            int h = (int) (height / (dist + 0.0001));
            int y1 = Math.max(0, height / 2 - h / 2);
            int y2 = Math.min(height, height / 2 + h / 2);

            int base = (hit == 2) ? 0x00CC00 : 0x666666;

            double fog = Math.max(0, Math.min(1, dist / MAX_DIST - cam.flashlightBoost));

            int r = (int) (((base >> 16) & 255) * (1 - fog));
            int g = (int) (((base >> 8) & 255) * (1 - fog));
            int b = (int) ((base & 255) * (1 - fog));
            int col = (r << 16) | (g << 8) | b;

            for (int y = y1; y < y2; y++) {
                pixels[x + y * width] = col;
            }
        }

        // 아이템 렌더링
        renderItems(cam, zBuffer, cos, sin);

        // 몬스터 렌더링
        renderMonster(cam, zBuffer, cos, sin);

        // 손전등 vignette 효과 (마지막에 적용)
        applyFlashlightVignette(cam);
    }

    private void renderItems(Camera cam, float[] zBuffer, double cos, double sin) {
        for (Item it : items) {
            if (it.collected) continue;
            it.update();

            double dx = it.x - cam.x;
            double dy = it.y - cam.y;

            double perpDist = dx * cos + dy * sin;
            if (perpDist < 0.05) continue;

            double itemDist = Math.hypot(dx, dy);

            double projX = (-dx * sin + dy * cos) / perpDist;
            int sx = (int) (projX * (width / 2.0) + width / 2.0);

            int size = (int) (350.0 / perpDist);
            if (size < 8) continue;

            int half = size / 2;
            if (sx + half < 0 || sx - half >= width) continue;

            int sy = height / 2 - half + (int) (Math.sin(it.bob) * 15);
            if (sy + half < 0 || sy - half >= height) continue;

            int color = switch (it.type) {
                case STAMINA -> 0x66FF66;
                case FREEZE -> 0xFFFF66;
                case FLASHLIGHT -> 0x66AAFF;
            };

            // 아이템 본체 (원형)
            for (int iy = -half; iy < half; iy++) {
                for (int ix = -half; ix < half; ix++) {
                    if (ix * ix + iy * iy > half * half * 0.8) continue;

                    int px = sx + ix;
                    int py = sy + iy;
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        if (itemDist < zBuffer[px]) {
                            pixels[px + py * width] = color;
                        }
                    }
                }
            }

            // 빛나는 테두리
            int glow = 0xFFFFFF;
            for (int i = 0; i < half; i += 3) {
                int[] offsets = {i, -i};
                for (int off : offsets) {
                    int px1 = sx + off;
                    int px2 = sx - off;
                    int py1 = sy + off;
                    int py2 = sy - off;

                    if (px1 >= 0 && px1 < width && sy >= 0 && sy < height && itemDist < zBuffer[px1]) pixels[px1 + sy * width] = glow;
                    if (px2 >= 0 && px2 < width && sy >= 0 && sy < height && itemDist < zBuffer[px2]) pixels[px2 + sy * width] = glow;
                    if (sx >= 0 && sx < width && py1 >= 0 && py1 < height && itemDist < zBuffer[sx]) pixels[sx + py1 * width] = glow;
                    if (sx >= 0 && sx < width && py2 >= 0 && py2 < height && itemDist < zBuffer[sx]) pixels[sx + py2 * width] = glow;
                }
            }
        }
    }

    private void renderMonster(Camera cam, float[] zBuffer, double cos, double sin) {
        Monster mon = cam.getMonster();
        if (mon == null || !mon.spawned) return;  // 스폰 전에는 안 보dla

        double dx = mon.x - cam.x;
        double dy = mon.y - cam.y;

        double perpDist = dx * cos + dy * sin;
        if (perpDist < 0.1) return;

        double monDist = Math.hypot(dx, dy);

        double projX = (-dx * sin + dy * cos) / perpDist;
        int sx = (int) (projX * (width / 2.0) + width / 2.0);

        int size = (int) (400.0 / perpDist);
        if (size < 10) return;

        int half = size / 2;
        if (sx + half < 0 || sx - half >= width) return;

        int sy = height / 2 - half + 20;

        int bodyColor = mon.frozen ? 0x444444 : 0x000000;
        int eyeColor = mon.frozen ? 0x8888FF : 0xFF0000;

        // 몸체
        for (int iy = -half; iy < half; iy++) {
            for (int ix = -half; ix < half; ix++) {
                if (ix * ix + iy * iy > half * half) continue;
                int px = sx + ix;
                int py = sy + iy;
                if (px >= 0 && px < width && py >= 0 && py < height) {
                    if (monDist < zBuffer[px]) {
                        pixels[px + py * width] = bodyColor;
                    }
                }
            }
        }

        // 눈
        int eyeSize = size / 6;
        int eyeOffset = size / 4;
        drawCircle(sx - eyeOffset, sy - eyeOffset / 2, eyeSize, eyeColor, monDist, zBuffer);
        drawCircle(sx + eyeOffset, sy - eyeOffset / 2, eyeSize, eyeColor, monDist, zBuffer);
    }

    private void drawCircle(int cx, int cy, int radius, int color, double monDist, float[] zBuffer) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    int px = cx + x;
                    int py = cy + y;
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        if (monDist < zBuffer[px]) {
                            pixels[px + py * width] = color;
                        }
                    }
                }
            }
        }
    }

    private void applyFlashlightVignette(Camera cam) {
        int centerX = width / 2;
        int centerY = height / 2;
        double maxRadius = Math.hypot(centerX, centerY) * 0.9;

        double boost = cam.flashlightBoost;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                int pixel = pixels[idx];

                if (pixel == 0) continue;

                double dx = x - centerX;
                double dy = y - centerY;
                double distFromCenter = Math.hypot(dx, dy);

                double vignette = 1.0 - (distFromCenter / maxRadius);
                vignette = Math.max(0, vignette);

                double brightness = vignette * (0.6 + 0.4 * boost);

                int r = (int) (((pixel >> 16) & 255) * brightness);
                int g = (int) (((pixel >> 8) & 255) * brightness);
                int b = (int) ((pixel & 255) * brightness);

                pixels[idx] = (r << 16) | (g << 8) | b;
            }
        }
    }
}