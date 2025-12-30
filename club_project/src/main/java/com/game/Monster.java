package com.game;

import java.awt.Point;
import java.util.*;

public class Monster {
    public double x, y;
    public double rot;
    public boolean frozen = false;
    public double freezeTime = 0.0;

    private double spawnDelay = 10.0;
    public boolean spawned = false;

    private static final double FOLLOW_SPEED = 0.042;

    private static final double GRAB_RANGE_SQ = 0.09;
    private static final double FOV_HALF = Math.PI / 3.0;

    private List<Point> currentPath = new ArrayList<>();
    private int pathIndex = 0;

    private double pathUpdateTimer = 0.0;
    private static final double PATH_UPDATE_INTERVAL = 0.5;  // 0.5초마다 재계산

    public Monster(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(int[][] map, Camera cam, double dt) {
        if (!spawned) {
            spawnDelay -= dt;
            if (spawnDelay <= 0.0) {
                spawned = true;
            }
            return;
        }

        if (frozen) {
            freezeTime -= dt;
            if (freezeTime <= 0.0) {
                frozen = false;
            }
            return;
        }

        // 경로 재계산 타이머
        pathUpdateTimer += dt;
        if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
            pathUpdateTimer = 0.0;
            currentPath = findPath(map, (int)x, (int)y, (int)cam.x, (int)cam.y);
            pathIndex = 0;
        }

        if (currentPath == null || currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            return;
        }

        Point target = currentPath.get(pathIndex);
        double targetX = target.x + 0.5;
        double targetY = target.y + 0.5;

        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.hypot(dx, dy);

        if (dist < 0.2) {
            pathIndex++;
        } else {
            rot = Math.atan2(dy, dx);
            x += (dx / dist) * FOLLOW_SPEED;
            y += (dy / dist) * FOLLOW_SPEED;
        }
    }

    // BFS로 최단 경로 찾기 (MazeGenerator의 findPath와 동일)
    private List<Point> findPath(int[][] m, int sx, int sy, int ex, int ey) {
        if (sx < 0 || sy < 0 || sx >= m[0].length || sy >= m.length) return null;

        boolean[][] visited = new boolean[m.length][m[0].length];
        Point[][] parent = new Point[m.length][m[0].length];
        Queue<Point> q = new ArrayDeque<>();

        q.add(new Point(sx, sy));
        visited[sy][sx] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!q.isEmpty()) {
            Point c = q.poll();
            if (c.x == ex && c.y == ey) {
                // 경로 재구성
                List<Point> path = new ArrayList<>();
                for (Point cur = new Point(ex, ey); cur != null; cur = parent[cur.y][cur.x]) {
                    path.add(cur);
                }
                Collections.reverse(path);
                return path;
            }

            for (int i = 0; i < 4; i++) {
                int nx = c.x + dx[i];
                int ny = c.y + dy[i];
                if (nx >= 0 && ny >= 0 && nx < m[0].length && ny < m.length &&
                        !visited[ny][nx] && m[ny][nx] != 1) {
                    visited[ny][nx] = true;
                    parent[ny][nx] = c;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return new ArrayList<>();  // 경로 없으면 빈 리스트
    }

    public boolean canGrab(Camera cam) {
        if (!spawned || frozen) return false;

        double dx = cam.x - x;
        double dy = cam.y - y;
        double distSq = dx * dx + dy * dy;

        if (distSq > GRAB_RANGE_SQ) return false;

        double forwardX = Math.cos(rot);
        double forwardY = Math.sin(rot);
        double dot = (dx * forwardX + dy * forwardY) / Math.sqrt(distSq);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        return Math.acos(dot) < FOV_HALF;
    }
}