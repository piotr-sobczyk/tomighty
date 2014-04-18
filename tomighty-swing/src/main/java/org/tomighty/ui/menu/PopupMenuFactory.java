package org.tomighty.ui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.inject.Inject;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.tomighty.bus.Bus;
import org.tomighty.bus.messages.ui.ProjectChanged;
import org.tomighty.config.Projects;
import org.tomighty.i18n.Messages;
import org.tomighty.projects.Project;

import com.google.inject.Injector;

public class PopupMenuFactory {

    @Inject
    private Injector injector;
    @Inject
    private Messages messages;
    @Inject
    private Projects projects;
    @Inject
    private Bus bus;

    public JPopupMenu create(Action[] items) {
        JPopupMenu menu = new JPopupMenu();

        ButtonGroup projectsGroup = new ButtonGroup();
        for (Project project : projects.getProjects()) {
            JMenuItem item = new JRadioButtonMenuItem(project.getName());

            projectsGroup.add(item);
            item.addActionListener(new SelectProject(project));
            menu.add(item);
        }
        menu.addSeparator();

        if (items != null && items.length > 0) {
            for (Action action : items) {
                injector.injectMembers(action);
                JMenuItem item = new JMenuItem(action);
                menu.add(item);
            }

            menu.addSeparator();
        }

        menu.add(menuItem("Options", injector.getInstance(ShowOptions.class)));
        //TODO: prepare new About page
        //menu.add(menuItem("About", injector.getInstance(ShowAboutWindow.class)));
        menu.addSeparator();
        menu.add(menuItem("Close", new Exit()));

        return menu;
    }

    private JMenuItem menuItem(String text, ActionListener listener) {
        JMenuItem item = new JMenuItem(messages.get(text));
        item.addActionListener(listener);
        return item;
    }

    private class SelectProject implements ActionListener {
        private Project project;

        private SelectProject(Project project) {
            this.project = project;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            bus.publish(new ProjectChanged(project));
        }
    }
}
