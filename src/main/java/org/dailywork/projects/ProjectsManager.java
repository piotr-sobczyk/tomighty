package org.dailywork.projects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.dailywork.bus.Bus;
import org.dailywork.bus.Subscriber;
import org.dailywork.bus.messages.ui.ChangeUiState;
import org.dailywork.bus.timer.TimerFinished;
import org.dailywork.bus.timer.TimerTick;
import org.dailywork.config.ProjectsStore;
import org.dailywork.time.Time;
import org.dailywork.time.Timer;
import org.dailywork.ui.PopupMenu;
import org.dailywork.ui.Window;
import org.dailywork.ui.state.bursts.BurstPaused;

public class ProjectsManager {

    @Inject
    private Timer timer;
    @Inject
    private Window window;
    @Inject
    private PopupMenu popupMenu;

    private ProjectsStore projectsStore;
    private Bus bus;

    private List<Project> projects;
    private Map<Project, ProjectProgress> projectStatuses = new HashMap<>();

    private Project currentProject;
    private ProjectProgress currentProjectProgress;

    public static final String INITIAL_PROJECT_NAME = "Select a project";

    public ProjectProgress getStatusForProject(Project project) {
        return projectStatuses.get(project);
    }

    @Inject
    public ProjectsManager(ProjectsStore projectsStore, Bus bus) {
        this.projectsStore = projectsStore;
        this.bus = bus;

        reloadProjects();

        for (Project project : projects) {
            projectStatuses.put(project, createProjectStatus(project));
        }

        bus.subscribe(new FinishProject(), TimerFinished.class);
        bus.subscribe(new UpdateTime(), TimerTick.class);
    }

    private void reloadProjects(){
        projects = projectsStore.loadProjects();
    }


    @PostConstruct
    public void initialize() {
        window.setProjectName(INITIAL_PROJECT_NAME);
    }

    public void addProject(String projectName, int dailyTimeInMins) {
        Project project = new Project(projectName, dailyTimeInMins);
        //add checking if doesn't exist
        projectsStore.saveProject(project);

        ProjectProgress projectProgress = createProjectStatus(project);

        reloadProjects();
        projectStatuses.put(project, projectProgress);

        popupMenu.reloadMenu();
    }

    private ProjectProgress createProjectStatus(Project project) {
        return new ProjectProgress(project, project.getDailyTimeMins(), bus);
    }


    public void removeProject(Project project) {
        projectsStore.removeProject(project);

        reloadProjects();
        projectStatuses.remove(project);

        popupMenu.reloadMenu();
    }

    public void updateProjectMetadata(Project project) {
        projectsStore.saveProject(project);
        //TODO: handle this!
        reloadProjects();
        popupMenu.reloadMenu();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void resetProjects() {
        for (ProjectProgress projectProgress : projectStatuses.values()) {
            projectProgress.reset();
        }
    }

    private class UpdateTime implements Subscriber<TimerTick> {
        @Override
        public void receive(TimerTick tick) {
            final Time time = tick.getTime();
            if (currentProjectProgress != null) {
                currentProjectProgress.updateTime(time);
            }
        }
    }

    private class FinishProject implements Subscriber<TimerFinished> {
        @Override
        public void receive(TimerFinished message) {
            currentProjectProgress.markFinished();
        }
    }

    public void changeProject(Project newProject) {
        currentProject = newProject;
        currentProjectProgress = projectStatuses.get(newProject);

        updateTimer(currentProjectProgress);

        bus.publish(new ChangeUiState(BurstPaused.class));
        window.setProjectName(newProject.getDisplayName());
    }

    private void updateTimer(ProjectProgress newProjectProgress) {
        Time time = newProjectProgress.getTime();
        timer.setTime(time);
        timer.pause();
    }

}
