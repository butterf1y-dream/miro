// Camera.java (전체 수정본 - 이동 속도 줄임)
package com.game;

import java.util.List;

public class Camera {

    public double x, y;
    public double rot;

    private static final double WALK_SPEED = 0.035;    // 속도 줄임 (0.045 → 0.035)
    private static final double SPRINT_SPEED = 0.065;  // 속도 줄임 (0.085 → 0.065)

    public double stamina = 100.0;
    public static final double MAX_STAMINA = 100.0;
    private static final double STAMINA_DRAIN = 0.9;
    private static final double STAMINA_RECOVER = 0.6;

    public boolean escaped = false;

    public double lightBoost = 0.0;
    public boolean revealMap = false;

    public Camera(double x, double y, double rot) {
        this.x = x;
        this.y = y;
        this.rot = rot;
    }

    public void rotate(double amt) {
        rot += amt;
    }

    public void update(Input in, int[][] map) {
        boolean moving = in.forward || in.backward || in.left || in.right;
        boolean sprint = in.sprint && stamina > 0 && moving;

        double speed = sprint ? SPRINT_SPEED : WALK_SPEED;

        if (sprint) stamina = Math.max(0, stamina - STAMINA_DRAIN);
        else stamina = Math.min(MAX_STAMINA, stamina + STAMINA_RECOVER);

        double dx = Math.cos(rot);
        double dy = Math.sin(rot);

        double nx = x;
        double ny = y;

        if (in.forward)  { nx += dx * speed; ny += dy * speed; }
        if (in.backward) { nx -= dx * speed; ny -= dy * speed; }
        if (in.left)     { nx += dy * speed; ny -= dx * speed; }
        if (in.right)    { nx -= dy * speed; ny += dx * speed; }

        // 경계 체크 강화
        if (nx >= 0 && nx < map[0].length && map[(int)ny][(int)nx] != 1) x = nx;
        if (ny >= 0 && ny < map.length && map[(int)ny][(int)x] != 1) y = ny;

        if (map[(int)y][(int)x] == 2) escaped = true;

        lightBoost = Math.max(0, lightBoost - 0.002);
    }

    public void collectItems(List<Item> items) {
        for (Item it : items) {
            if (it.collected) continue;

            double dx = it.x - x;
            double dy = it.y - y;

            if (dx*dx + dy*dy < 0.25) {
                it.collected = true;

                switch (it.type) {
                    case STAMINA -> stamina = MAX_STAMINA;
                    case LIGHT   -> lightBoost = 0.6;
                    case MAP     -> revealMap = true;
                }
            }
        }
    }
}