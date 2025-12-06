package com.game;

public class Camera {
    public double x, y;   // position (tile coords)
    public double rot;    // yaw

    // movement tuning
    private final double MOVE_SPEED = 0.085;
    private final double STRAFE_SPEED = 0.065;

    public Camera(double sx, double sy, double srot) {
        this.x = sx;
        this.y = sy;
        this.rot = srot;
    }

    public void rotate(double d) {
        rot += d;
    }

    public void update(Input in, int[][] map) {
        double dx = 0, dy = 0;
        double fwdX = Math.cos(rot), fwdY = Math.sin(rot);
        double rightX = Math.cos(rot + Math.PI / 2.0), rightY = Math.sin(rot + Math.PI / 2.0);

        if (in.forward) { dx += fwdX * MOVE_SPEED; dy += fwdY * MOVE_SPEED; }
        if (in.back)    { dx -= fwdX * MOVE_SPEED; dy -= fwdY * MOVE_SPEED; }
        if (in.left)    { dx -= rightX * STRAFE_SPEED; dy -= rightY * STRAFE_SPEED; }
        if (in.right)   { dx += rightX * STRAFE_SPEED; dy += rightY * STRAFE_SPEED; }

        attemptMove(dx, dy, map);
    }

    private void attemptMove(double dx, double dy, int[][] map) {
        if (Math.abs(dx) < 1e-9 && Math.abs(dy) < 1e-9) return;
        double nx = x + dx;
        double ny = y + dy;

        int wx = (int)Math.floor(nx);
        int wy = (int)Math.floor(y);
        if (isWalkable(wx, wy, map)) x = nx;

        wx = (int)Math.floor(x);
        wy = (int)Math.floor(ny);
        if (isWalkable(wx, wy, map)) y = ny;
    }

    private boolean isWalkable(int gx, int gy, int[][] map) {
        if (gy < 0 || gx < 0 || gy >= map.length || gx >= map[0].length) return false;
        return map[gy][gx] == 0 || map[gy][gx] == 2; // 0 empty, 2 exit allowed
    }
}
