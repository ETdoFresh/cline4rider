package com.aihelper.window;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class AIHelperPanel extends JPanel {
    private final Project project;
    private final JBTabbedPane tabbedPane;

    public AIHelperPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JBTabbedPane();
        
        // Add tabs
        createHomeTab();
        createTasksTab();
        createHistoryTab();

        // Add tabbed pane to panel
        add(tabbedPane, BorderLayout.CENTER);
        setBorder(JBUI.Borders.empty(2));
    }

    private void createHomeTab() {
        JPanel homePanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome to AI Helper!", SwingConstants.CENTER);
        welcomeLabel.setBorder(JBUI.Borders.empty(10));
        homePanel.add(welcomeLabel, BorderLayout.NORTH);
        
        // Add scrollable content area
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(new JLabel("Home screen content coming soon..."));
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        homePanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Home", homePanel);
    }

    private void createTasksTab() {
        JPanel tasksPanel = new JPanel(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(new JLabel("Task management coming soon..."));
        
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        tasksPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Tasks", tasksPanel);
    }

    private void createHistoryTab() {
        JPanel historyPanel = new JPanel(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(new JLabel("History log coming soon..."));
        
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("History", historyPanel);
    }
}