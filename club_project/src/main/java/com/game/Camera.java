package com.game;

public class Camera {

    public double x, y;
    public double rot;

    // movement
    private static final double WALK_SPEED = 0.045;
    private static final double SPRINT_SPEED = 0.085;

    // stamina
    public double stamina = 100.0;
    public static final double MAX_STAMINA = 100.0;
    private static final double STAMINA_DRAIN = 0.9;
    private static final double STAMINA_RECOVER = 0.6;

    public boolean escaped = false;

    // head bob
    private double bobPhase = 0.0;
    public double bobOffset = 0.0;

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
        boolean canSprint = in.sprint && stamina > 0 && moving;

        double speed = canSprint ? SPRINT_SPEED : WALK_SPEED;

        // stamina logic
        if (canSprint) {
            stamina -= STAMINA_DRAIN;
            if (stamina < 0) stamina = 0;
        } else {
            stamina += STAMINA_RECOVER;
            if (stamina > MAX_STAMINA) stamina = MAX_STAMINA;
        }

        // head bob
        if (canSprint) {
            bobPhase += 0.22;
            bobOffset = Math.sin(bobPhase) * 6.0;
        } else if (moving) {
            bobPhase += 0.12;
            bobOffset = Math.sin(bobPhase) * 3.0;
        } else {
            bobOffset *= 0.8;
        }

        double dx = Math.cos(rot);
        double dy = Math.sin(rot);

        double nx = x;
        double ny = y;

        if (in.forward) {
            nx += dx * speed;
            ny += dy * speed;
        }
        if (in.backward) {
            nx -= dx * speed;
            ny -= dy * speed;
        }
        if (in.left) {
            nx += dy * speed;
            ny -= dx * speed;
        }
        if (in.right) {
            nx -= dy * speed;
            ny += dx * speed;
        }

        // collision (벽 = 1, 탈출구 = 2 는 통과 가능)
        if (map[(int) y][(int) nx] != 1) x = nx;
        if (map[(int) ny][(int) x] != 1) y = ny;

        // ===== 탈출 판정 =====
        int tx = (int) x;
        int ty = (int) y;

        if (map[ty][tx] == 2) {
            escaped = true;
        }
    }
}
