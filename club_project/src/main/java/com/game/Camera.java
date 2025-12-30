package com.game;

import java.awt.Point;
import java.util.List;

public class Camera {

    public double x, y;
    public double rot;

    private static final double WALK_SPEED = 0.035;
    private static final double SPRINT_SPEED = 0.065;

    public double stamina = 100.0;
    public static final double MAX_STAMINA = 100.0;
    private static final double STAMINA_DRAIN_BASE = 0.6;
    private static final double STAMINA_DRAIN_MULTIPLIER = 1.5;
    private static final double STAMINA_RECOVER = 0.6;

    private static final double SPRINT_BLOCK_THRESHOLD = 30.0;

    private boolean sprintBlocked = false;

    public boolean escaped = false;
    public boolean gameOver = false;

    public double flashlightBoost = 0.0;
    public double flashlightDuration = 0.0;

    public double monsterFreezeTime = 0.0;

    private Monster monster;

    public Camera(double x, double y, double rot) {
        this.x = x;
        this.y = y;
        this.rot = rot;
    }

    public void rotate(double amt) {
        rot += amt;
    }

    public void setMonster(Monster monster) {
        this.monster = monster;
    }

    public Monster getMonster() {
        return monster;
    }

    public void update(Input in, int[][] map, double dt, List<Point> escapePath, boolean showPath) {
        boolean moving = in.forward || in.backward || in.left || in.right;

        boolean sprintRequested = in.sprint && moving;
        boolean canSprint = sprintRequested && !sprintBlocked;

        double speed = canSprint ? SPRINT_SPEED : WALK_SPEED;

        if (canSprint) {
            double drainMultiplier = 1.0 + (1.0 - stamina / MAX_STAMINA) * STAMINA_DRAIN_MULTIPLIER;
            double drain = STAMINA_DRAIN_BASE * drainMultiplier;
            stamina = Math.max(0, stamina - drain);

            if (stamina <= 0) {
                sprintBlocked = true;
            }
        } else {
            stamina = Math.min(MAX_STAMINA, stamina + STAMINA_RECOVER);

            if (sprintBlocked && stamina >= SPRINT_BLOCK_THRESHOLD) {
                sprintBlocked = false;
            }
        }

        double dx = Math.cos(rot);
        double dy = Math.sin(rot);

        double nx = x;
        double ny = y;

        if (in.forward)  { nx += dx * speed; ny += dy * speed; }
        if (in.backward) { nx -= dx * speed; ny -= dy * speed; }
        if (in.left)     { nx += dy * speed; ny -= dx * speed; }
        if (in.right)    { nx -= dy * speed; ny += dx * speed; }

        if (nx >= 0 && nx < map[0].length && map[(int)ny][(int)nx] != 1) x = nx;
        if (ny >= 0 && ny < map.length && map[(int)ny][(int)x] != 1) y = ny;

        if (map[(int)y][(int)x] == 2) escaped = true;

        if (flashlightDuration > 0) {
            flashlightDuration -= dt;
            flashlightBoost = 1.0;
        } else {
            flashlightBoost = 0.0;
        }

        if (monsterFreezeTime > 0) {
            monsterFreezeTime -= dt;
        }

        // 몬스터 업데이트: map 전달해서 실시간 경로 계산
        if (monster != null) {
            monster.update(map, this, dt);

            if (!monster.frozen && monster.canGrab(this)) {
                gameOver = true;
            }
        }
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
                    case FREEZE -> {
                        monsterFreezeTime = 5.0;
                        if (monster != null) {
                            monster.frozen = true;
                            monster.freezeTime = 5.0;
                        }
                    }
                    case FLASHLIGHT -> flashlightDuration = 5.0;
                }
            }
        }
    }
}