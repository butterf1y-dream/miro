package com.game;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Input implements KeyListener {

    public boolean forward, backward, left, right;
    public boolean sprint;
    public boolean toggleShiftLock = false;
    public boolean togglePath = false;  // P 키 토글 추가

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> forward = true;
            case KeyEvent.VK_S -> backward = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> sprint = true;
            case KeyEvent.VK_L -> toggleShiftLock = true;
            case KeyEvent.VK_P -> togglePath = true;  // P 키 추가
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> forward = false;
            case KeyEvent.VK_S -> backward = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_SPACE -> sprint = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}