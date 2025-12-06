package com.game;

import java.awt.image.BufferedImage;

public class Screen {
    private final int width, height;
    private final int[] pixels;
    private final BufferedImage img;
    private final int[][] map;

    private final double planeX = 0; // we'll compute per column using perpendicular basis
    private final double planeY = 0.66; // camera plane length -> controls FOV

    public Screen(int w, int h, int[][] map) {
        this.width = w; this.height = h;
        this.map = map;
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        pixels = ((java.awt.image.DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    public BufferedImage getImage() { return img; }

    public void render(Camera cam) {
        // sky
        final int SKY = 0x87CEEB;
        for (int y = 0; y < height/2; y++) {
            int off = y * width;
            for (int x = 0; x < width; x++) pixels[off + x] = SKY;
        }
        // floor
        for (int y = height/2; y < height; y++) {
            int off = y * width;
            int shade = 80 + (int)((y - height/2) / (double)height * 80);
            int col = (shade<<16) | (shade<<8) | shade;
            for (int x = 0; x < width; x++) pixels[off + x] = col;
        }

        for (int x = 0; x < width; x++) {
            // cameraX in [-1,1]
            double cameraX = 2.0 * x / (double)width - 1.0;
            // ray direction
            double dirX = Math.cos(cam.rot) + (-Math.sin(cam.rot)) * cameraX * planeY;
            double dirY = Math.sin(cam.rot) + ( Math.cos(cam.rot)) * cameraX * planeY;

            // normalize direction
            double len = Math.sqrt(dirX*dirX + dirY*dirY);
            dirX /= len; dirY /= len;

            double dist = 0;
            double px = cam.x, py = cam.y;
            int hitType = 0;
            int hitMX = 0, hitMY = 0;

            // simple step ray march
            while (dist < 40.0) {
                dist += 0.03;
                px = cam.x + dirX * dist;
                py = cam.y + dirY * dist;
                int mx = (int)Math.floor(px);
                int my = (int)Math.floor(py);
                if (mx < 0 || my < 0 || mx >= map[0].length || my >= map.length) break;
                hitType = map[my][mx];
                if (hitType == 1 || hitType == 2) { hitMX = mx; hitMY = my; break; }
            }

            // fisheye correction (using cameraX)
            double corrected = dist * Math.cos(cameraX * 0.5);

            // projected line height
            double lineHeight = (height / (corrected + 0.0001)) * 0.8; // tweak
            int drawStart = (int)(height/2 - lineHeight/2);
            int drawEnd = (int)(height/2 + lineHeight/2);
            if (drawStart < 0) drawStart = 0;
            if (drawEnd >= height) drawEnd = height - 1;

            int color;
            if (hitType == 1) color = 0xCCCCCC; // wall
            else if (hitType == 2) color = 0xFFD166; // exit (gold)
            else color = 0; // no hit -> leave sky/floor

            // shading by distance
            if (color != 0) {
                double shade = 1.0 / (1.0 + corrected * 0.12);
                int r = (int)(((color >> 16) & 0xFF) * shade);
                int g = (int)(((color >> 8) & 0xFF) * shade);
                int b = (int)((color & 0xFF) * shade);
                int col = (r << 16) | (g << 8) | b;
                for (int y = drawStart; y <= drawEnd; y++) pixels[y * width + x] = col;
            }
        }

        img.setRGB(0,0,width,height,pixels,0,width);
    }
}
