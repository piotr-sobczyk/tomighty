/*
 * Copyright (c) 2010-2012 Célio Cidral Junior.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.dailywork;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.UIManager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.dailywork.bus.messages.ui.ChangeUiState;
import org.dailywork.bus.messages.ui.TrayClick;
import org.dailywork.bus.messages.ui.UiStateChanged;
import org.dailywork.config.Directories;
import org.dailywork.config.Options;
import org.dailywork.inject.DailyWorkModule;
import org.dailywork.quartz.Quartz;
import org.dailywork.ui.UiState;
import org.dailywork.ui.Window;
import org.dailywork.ui.state.InitialState;
import org.dailywork.ui.tray.TrayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mycila.inject.jsr250.Jsr250;

public class DailyWork implements Runnable {

    @Inject
    private Window window;
    @Inject
    private Options options;
    @Inject
    private EventBus eventBus;
    @Inject
    private Injector injector;
    @Inject
    private Directories directories;
    @Inject
    private Quartz quartz;

    private UiState currentState;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Injector injector = Guice.createInjector(new DailyWorkModule(), Jsr250.newJsr250Module());

        DailyWork dailyWork = injector.getInstance(DailyWork.class);
        invokeLater(dailyWork);
        TrayManager trayManager = injector.getInstance(TrayManager.class);
        invokeLater(trayManager);
    }

    @PostConstruct
    public void initialize() {
        eventBus.register(this);
        eventBus.register(this);
    }

    @Override
    public void run() {
        render(InitialState.class);
    }

    private void render(Class<? extends UiState> stateClass) {
        if (currentState != null) {
            currentState.beforeDetaching();
        }
        currentState = injector.getInstance(stateClass);
        Component component;
        try {
            component = currentState.render();
        } catch (Exception error) {
            logger.error("Failed to render state: " + currentState, error);
            return;
        }
        window.setViewportView(component);
        currentState.afterRendering();
    }

    @Subscribe
    public void switchState(final ChangeUiState message) {
        invokeLater(new Runnable() {
            @Override
            public void run() {
                Class<? extends UiState> stateClass = message.getStateClass();
                render(stateClass);
                window.show(null);
                eventBus.post(new UiStateChanged(currentState));
            }
        });
    }

    @Subscribe
    public void showWindow(final TrayClick message) {
        invokeLater(new Runnable() {
            @Override
            public void run() {
                if (options.ui().autoHideWindow() || !window.isVisible()) {
                    window.show(message.mouseLocation());
                } else {
                    window.setVisible(false);
                }
            }
        });
    }

}
