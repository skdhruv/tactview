package com.helospark.tactview.ui.javafx.render;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.render.RenderServiceChain;
import com.helospark.tactview.core.repository.ProjectRepository;
import com.helospark.tactview.core.timeline.TimelineManagerAccessor;
import com.helospark.tactview.ui.javafx.UiMessagingService;
import com.helospark.tactview.ui.javafx.stylesheet.AlertDialogFactory;
import com.helospark.tactview.ui.javafx.stylesheet.StylesheetAdderService;

@Component
public class RenderDialogOpener {
    private RenderServiceChain renderService;
    private ProjectRepository projectRepository;
    private UiMessagingService messagingService;
    private TimelineManagerAccessor timelineManager;
    private StylesheetAdderService stylesheetAdderService;
    private AlertDialogFactory alertDialogFactory;

    public RenderDialogOpener(RenderServiceChain renderService, ProjectRepository projectRepository, UiMessagingService messagingService, TimelineManagerAccessor timelineManager,
            StylesheetAdderService stylesheetAdderService, AlertDialogFactory alertDialogFactory) {
        this.renderService = renderService;
        this.projectRepository = projectRepository;
        this.messagingService = messagingService;
        this.timelineManager = timelineManager;
        this.stylesheetAdderService = stylesheetAdderService;
        this.alertDialogFactory = alertDialogFactory;
    }

    public void render() {
        RenderDialog renderDialog = new RenderDialog(renderService, projectRepository, messagingService, timelineManager, stylesheetAdderService, alertDialogFactory);
        renderDialog.show();
    }

}
