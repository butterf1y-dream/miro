// Monster.java (새 파일 - 몬스터 클래스, 1마리만 사용)
package com.game;

public class Monster {
    public double x, y;
    public double rot;
    public boolean frozen = false;
    public double freezeTime = 0.0;

    public Monster(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(Camera cam, int[][] map, double dt) {
        if (frozen) {
            freezeTime -= dt;
            if (freezeTime <= 0.0) frozen = false;
            return;
        }

        double dx = cam.x - x;
        double dy = cam.y - y;
        rot = Math.atan2(dy, dx);

        double speed = 0.028;
        double nx = x + Math.cos(rot) * speed;
        double ny = y + Math.sin(rot) * speed;

        if (nx > 0 && nx < map[0].length && map[(int)ny][(int)nx] != 1) x = nx;
        if (ny > 0 && ny < map.length && map[(int)ny][(int)x] != 1) y = ny;
    }

    public boolean collidesWith(Camera cam) {
        double dx = x - cam.x;
        double dy = y - cam.y;
        return dx*dx + dy*dy < 0.2;
    }
}