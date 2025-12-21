// Screen.java (전체 수정본 - 아이템 투영/깊이 계산 완전 수정 + Z-Buffer 정확화)
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

    private static final double FOV = 0.35;
    private static final double MAX_DIST = 20.0;

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
        // Z-Buffer 초기화 (벽 유클리드 거리)
        float[] zBuffer = new float[width];
        for (int i = 0; i < width; i++) {
            zBuffer[i] = Float.MAX_VALUE;
        }

        // 배경 클리어
        for (int y = 0; y < height; y++) {
            int c = (y < height / 2) ? 0x0A0E1A : 0x050505;
            for (int x = 0; x < width; x++) pixels[x + y * width] = c;
        }

        double cos = Math.cos(cam.rot);
        double sin = Math.sin(cam.rot);

        // 벽 raycasting + Z-Buffer (유클리드 거리 저장)
        for (int x = 0; x < width; x++) {
            double cx = 2.0 * x / width - 1.0;
            double rayX = cos + cx * -sin * FOV;
            double rayY = sin + cx * cos * FOV;

            double dist = 0.0;
            int hit = 0;
            double hitX = 0, hitY = 0;

            while (dist < MAX_DIST) {
                dist += 0.04;  // 스텝 약간 줄여 정확도 ↑
                int mx = (int) (cam.x + rayX * dist);
                int my = (int) (cam.y + rayY * dist);
                if (mx < 0 || my < 0 || mx >= map[0].length || my >= map.length) break;
                hit = map[my][mx];
                hitX = mx + 0.5;  // 셀 중앙
                hitY = my + 0.5;
                if (hit != 0) break;
            }

            // Z-Buffer: 벽까지의 실제 유클리드 거리
            double wallDist = Math.hypot(cam.x - hitX, cam.y - hitY);
            zBuffer[x] = (float) Math.min(dist, wallDist);

            int h = (int) (height / dist);
            int y1 = Math.max(0, height / 2 - h / 2);
            int y2 = Math.min(height, height / 2 + h / 2);

            int base = (hit == 2) ? 0x00FF00 : 0x888888;
            double fog = Math.max(0, Math.min(1, dist / MAX_DIST - cam.lightBoost));

            int r = (int) (((base >> 16) & 255) * (1 - fog));
            int g = (int) (((base >> 8) & 255) * (1 - fog));
            int b = (int) ((base & 255) * (1 - fog));
            int col = (r << 16) | (g << 8) | b;

            for (int y = y1; y < y2; y++) {
                pixels[x + y * width] = col;
            }
        }

        // 아이템 렌더링 (Z-Buffer 후)
        renderItems(cam, zBuffer, cos, sin);
    }

    private void renderItems(Camera cam, float[] zBuffer, double cos, double sin) {
        for (Item it : items) {
            if (it.collected) continue;
            it.update();

            double dx = it.x - cam.x;
            double dy = it.y - cam.y;

            // 수직 거리 (투영 깊이: forward dot)
            double perpDist = dx * cos + dy * sin;
            if (perpDist < 0.05) continue;  // 카메라 뒤/너무 가까움

            // 실제 유클리드 거리 (Z 비교용)
            double itemDist = Math.hypot(dx, dy);

            // 화면 X 투영 (right vector dot)
            double projX = (-dx * sin + dy * cos) / perpDist;
            int sx = (int) (projX * (width / 2.0) + width / 2.0);

            // 크기 (perpDist 기준)
            int size = (int) (300.0 / perpDist);  // 크기 ↑ (200 → 300)
            if (size < 6) continue;

            int half = size / 2;
            if (sx + half < 0 || sx - half >= width) continue;

            int sy = height / 2 - half + (int) (Math.sin(it.bob) * 12);  // bob ↑
            if (sy + half < 0 || sy - half >= height) continue;

            int color = switch (it.type) {
                case STAMINA -> 0x66FF66;
                case LIGHT -> 0xFFFF66;
                case MAP -> 0x66AAFF;
            };

            // 스프라이트 픽셀 (원형 + Z-Buffer 체크)
            for (int iy = -half; iy < half; iy++) {
                for (int ix = -half; ix < half; ix++) {
                    if (ix * ix + iy * iy > half * half * 0.8) continue;  // 약간 납작 원

                    int px = sx + ix;
                    int py = sy + iy;
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        // Z-Buffer: 아이템이 벽보다 가까우면 그리기
                        if (itemDist < zBuffer[px]) {
                            pixels[px + py * width] = color;
                        }
                    }
                }
            }

            // 밝은 테두리 (Z 체크)
            int borderColor = 0xFFFFFF;
            for (int i = 0; i < half; i += 2) {
                int[] offsets = {i, -i};
                for (int off : offsets) {
                    int pxTop = sx + off;
                    int pyLeft = sy + off;
                    int pyRight = sy - off;  // bottom은 sy + i지만 상/하

                    // 상하
                    if (pxTop >= 0 && pxTop < width && sy >= 0 && sy < height && itemDist < zBuffer[pxTop]) {
                        pixels[pxTop + sy * width] = borderColor;
                    }
                    int pxBot = sx + off;
                    if (pxBot >= 0 && pxBot < width && sy + half - 1 >= 0 && sy + half - 1 < height && itemDist < zBuffer[pxBot]) {
                        pixels[pxBot + (sy + half - 1) * width] = borderColor;
                    }

                    // 좌우
                    if (sx >= 0 && sx < width && pyLeft >= 0 && pyLeft < height && itemDist < zBuffer[sx]) {
                        pixels[sx + pyLeft * width] = borderColor;
                    }
                    if (sx >= 0 && sx < width && pyRight >= 0 && pyRight < height && itemDist < zBuffer[sx]) {
                        pixels[sx + pyRight * width] = borderColor;
                    }
                }
            }
        }
    }
}