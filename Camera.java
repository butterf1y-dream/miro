// Camera.java (전체 수정본 - JOptionPane 에러 완전 해결 + 몬스터 게임 오버 처리 이동)
package com.game;

import java.util.List;

public class Camera {

    public double x, y;
    public double rot;

    private static final double WALK_SPEED = 0.035;
    private static final double SPRINT_SPEED = 0.065;

    public double stamina = 100.0;
    public static final double MAX_STAMINA = 100.0;
    private static final double STAMINA_DRAIN = 0.9;
    private static final double STAMINA_RECOVER = 0.6;

    public boolean escaped = false;
    public boolean gameOver = false;  // 몬스터에 잡혔는지 플래그 추가

    public double flashlightBoost = 0.0;
    public double flashlightDuration = 0.0;

    public double monsterFreezeTime = 0.0;

    private Monster monster;
    // Camera.java (getter 추가 - monster 접근 허용)
    // 필드


    // getter 추가
    public Monster getMonster() {
        return monster;
    }

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

    public void update(Input in, int[][] map, double dt) {
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

        // 몬스터 업데이트
        if (monster != null) {
            monster.update(this, map, dt);

            // 몬스터 충돌 체크 (JOptionPane은 Game.java에서 처리)
            if (!monster.frozen && monster.collidesWith(this)) {
                gameOver = true;  // 플래그만 설정 (Game.java에서 메시지 표시)
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