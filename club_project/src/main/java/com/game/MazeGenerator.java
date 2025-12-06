package com.game;

import java.util.*;

public class MazeGenerator {

    // generate odd-sized maze (width,height should be odd)
    public static int[][] generate(int w, int h) {
        if (w % 2 == 0) w++;
        if (h % 2 == 0) h++;

        int[][] map = new int[h][w];
        // fill walls
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) map[y][x] = 1;

        Random rand = new Random();

        // DFS carve
        int sx = 1, sy = 1;
        map[sy][sx] = 0;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sx, sy});

        int[][] dirs = {{2,0},{-2,0},{0,2},{0,-2}};

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            int cx = cur[0], cy = cur[1];
            List<int[]> neighbors = new ArrayList<>();
            for (int[] d: dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx > 0 && ny > 0 && nx < w-1 && ny < h-1 && map[ny][nx] == 1) {
                    neighbors.add(new int[]{nx, ny});
                }
            }
            if (!neighbors.isEmpty()) {
                int[] n = neighbors.get(rand.nextInt(neighbors.size()));
                int nx = n[0], ny = n[1];
                // carve wall between
                map[(ny+cy)/2][(nx+cx)/2] = 0;
                map[ny][nx] = 0;
                stack.push(new int[]{nx, ny});
            } else {
                stack.pop();
            }
        }

        // ensure start and exit open
        map[1][1] = 0;
        map[h-2][w-2] = 0;
        return map;
    }
}
