package com.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainMenu extends JFrame {

    private CardLayout cardLayout;
    private JPanel container;
    private JPanel mainPanel, howtoPanel, recordsPanel;

    public MainMenu() { //메인 화면
        setTitle("미로 탈출");
        setSize(1200, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);
        container.setBackground(Color.DARK_GRAY);

        initMainPanel();
        initHowtoPanel();
        initRecordsPanel();

        container.add(mainPanel, "main");
        container.add(howtoPanel, "howto");
        container.add(recordsPanel, "records");

        add(container);
        cardLayout.show(container, "main");

        setVisible(true);
    }

    private void initMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(20, 20, 40));

        // 제목
        JLabel title = new JLabel("미로 탈출", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 60));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(100, 0, 100, 0));
        mainPanel.add(title, BorderLayout.NORTH);

        // 버튼 패널
        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 20, 20));
        btnPanel.setBackground(new Color(20, 20, 40));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 300, 0, 300));

        JButton startBtn = createButton("게임 시작", e -> {
            dispose();
            new Game();
        });
        JButton howtoBtn = createButton("게임 방법", e -> cardLayout.show(container, "howto"));
        JButton recordsBtn = createButton("기록 보기", e -> cardLayout.show(container, "records"));
        JButton exitBtn = createButton("종료", e -> System.exit(0));

        btnPanel.add(startBtn);
        btnPanel.add(howtoBtn);
        btnPanel.add(recordsBtn);
        btnPanel.add(exitBtn);

        mainPanel.add(btnPanel, BorderLayout.CENTER);
    }

    private void initHowtoPanel() {
        howtoPanel = new JPanel(new BorderLayout());
        howtoPanel.setBackground(new Color(20, 20, 40));

        // 제목
        JLabel title = new JLabel("게임 방법", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(50, 0, 30, 0));
        howtoPanel.add(title, BorderLayout.NORTH);

        // 설명 텍스트
        JPanel textPanel = getJPanel();

        howtoPanel.add(textPanel, BorderLayout.CENTER);

        // 뒤로 버튼
        JButton backBtn = createButton("메인 메뉴로", e -> cardLayout.show(container, "main"));
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        backPanel.setBackground(new Color(20, 20, 40));
        backPanel.add(backBtn);
        howtoPanel.add(backPanel, BorderLayout.SOUTH);
    }

    private static JPanel getJPanel() { //게임 방법설정 글써둔거
        String[] lines = {
                "WASD: 이동",
                "Space: 스프린트 (스태미나 소모)",
                "마우스 드래그 / L: Shift Lock (시점 회전)",
                "P: 탈출 경로 토글",
                "아이템 효과:",
                "  초록 - 스태미나 회복",
                "  노랑 - 시야 밝아짐",
                "  파랑 - 경로 표시",
                "초록 구역 도착 = 탈출 성공!"
        };

        JPanel textPanel = new JPanel(new GridLayout(lines.length, 1, 0, 8));
        textPanel.setBackground(new Color(20, 20, 40));
        textPanel.setBorder(BorderFactory.createEmptyBorder(50, 200, 50, 200));

        for (String line : lines) {
            JLabel label = new JLabel(line, SwingConstants.LEFT);
            label.setFont(new Font("SansSerif", Font.PLAIN, 28));
            label.setForeground(Color.WHITE);
            textPanel.add(label);
        }
        return textPanel;
    }

    private void initRecordsPanel() {
        recordsPanel = new JPanel(new BorderLayout());
        recordsPanel.setBackground(new Color(20, 20, 40));

        // 제목
        JLabel title = new JLabel("최고 기록", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(50, 0, 30, 0));
        recordsPanel.add(title, BorderLayout.NORTH);

        // 기록 목록
        JPanel listPanel = new JPanel(new GridLayout(0, 1, 0, 15));
        listPanel.setBackground(new Color(20, 20, 40));
        listPanel.setBorder(BorderFactory.createEmptyBorder(50, 300, 50, 300));

        ArrayList<Double> records = Game.RECORDS;
        DecimalFormat df = new DecimalFormat("0.00");

        if (records.isEmpty()) {
            JLabel noRec = new JLabel("아직 기록이 없습니다.", SwingConstants.CENTER);
            noRec.setFont(new Font("SansSerif", Font.BOLD, 32));
            noRec.setForeground(Color.GRAY);
            listPanel.add(noRec);
        } else {
            for (int i = 0; i < Math.min(5, records.size()); i++) {
                JLabel recLabel = new JLabel((i + 1) + ". " + df.format(records.get(i)) + "초", SwingConstants.CENTER);
                recLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
                recLabel.setForeground(Color.CYAN);
                listPanel.add(recLabel);
            }
        }

        recordsPanel.add(listPanel, BorderLayout.CENTER);

        // 뒤로 버튼
        JButton backBtn = createButton("메인 메뉴로", e -> cardLayout.show(container, "main"));
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        backPanel.setBackground(new Color(20, 20, 40));
        backPanel.add(backBtn);
        recordsPanel.add(backPanel, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(350, 70));
        btn.setFont(new Font("SansSerif", Font.BOLD, 24));
        btn.setBackground(new Color(60, 60, 120));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        btn.addActionListener(listener);
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu());
    }
}