package com.helospark.tactview.ui.javafx.uicomponents;

import static com.helospark.tactview.ui.javafx.commands.impl.CreateChannelCommand.LAST_INDEX;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.TimelineManager;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.util.messaging.MessagingService;
import com.helospark.tactview.ui.javafx.UiCommandInterpreterService;
import com.helospark.tactview.ui.javafx.UiTimelineManager;
import com.helospark.tactview.ui.javafx.commands.impl.CreateChannelCommand;

import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

@Component
public class UiTimeline {

    private TimeLineZoomCallback timeLineZoomCallback;
    private TimelineState timelineState;
    private UiCommandInterpreterService commandInterpreter;
    private TimelineManager timelineManager;
    private UiTimelineManager uiTimelineManager;

    private Line positionIndicatorLine;

    private ScrollPane timeLineScrollPane;
    private BorderPane borderPane;

    public UiTimeline(TimeLineZoomCallback timeLineZoomCallback, MessagingService messagingService,
            TimelineState timelineState, UiCommandInterpreterService commandInterpreter,
            TimelineManager timelineManager, UiTimelineManager uiTimelineManager) {
        this.timeLineZoomCallback = timeLineZoomCallback;
        this.timelineState = timelineState;
        this.commandInterpreter = commandInterpreter;
        this.timelineManager = timelineManager;
        this.uiTimelineManager = uiTimelineManager;
    }

    public Node createTimeline() {
        borderPane = new BorderPane();

        Button addChannelButton = new Button("Channel", new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
        addChannelButton.setOnMouseClicked(event -> {
            commandInterpreter.sendWithResult(new CreateChannelCommand(timelineManager, LAST_INDEX));
        });

        HBox titleBarTop = new HBox();
        titleBarTop.getChildren().add(addChannelButton);

        borderPane.setTop(titleBarTop);

        timeLineScrollPane = new ScrollPane();
        GridPane gridPane = new GridPane();

        Group timelineGroup = new Group();
        Group zoomGroup = new Group();
        VBox timelineBoxes = new VBox();
        timelineBoxes.setPrefWidth(2000);
        zoomGroup.getChildren().add(timelineBoxes);

        positionIndicatorLine = new Line();
        positionIndicatorLine.setLayoutX(6.0); // TODO: Layout need to be fixed
        positionIndicatorLine.setStartY(0);
        positionIndicatorLine.endYProperty().bind(timelineBoxes.heightProperty());
        positionIndicatorLine.startXProperty().bind(timelineState.getLinePosition());
        positionIndicatorLine.endXProperty().bind(timelineState.getLinePosition());
        positionIndicatorLine.setId("timeline-position-line");
        zoomGroup.getChildren().add(positionIndicatorLine);

        Line specialPositionLine = new Line();
        specialPositionLine.setLayoutX(6.0); // TODO: Layout need to be fixed
        specialPositionLine.layoutXProperty().bind(timelineState.getMoveSpecialPointLineProperties().getStartX());
        specialPositionLine.startYProperty().bind(timelineState.getMoveSpecialPointLineProperties().getStartY());
        specialPositionLine.visibleProperty().bind(timelineState.getMoveSpecialPointLineProperties().getEnabledProperty());
        specialPositionLine.endYProperty().bind(timelineState.getMoveSpecialPointLineProperties().getEndY());
        specialPositionLine.setId("special-position-line");
        zoomGroup.getChildren().add(specialPositionLine);

        Bindings.bindContentBidirectional(timelineState.getChannelsAsNodes(), timelineBoxes.getChildren());

        VBox timelineTitles = new VBox();
        Bindings.bindContentBidirectional(timelineState.getChannelTitlesAsNodes(), timelineTitles.getChildren());

        timelineGroup.getChildren().add(zoomGroup);

        zoomGroup.addEventFilter(ScrollEvent.SCROLL, e -> {
            timeLineZoomCallback.onScroll(e, timeLineScrollPane);
            e.consume();
        });

        titleBarTop.setOnMouseClicked(e -> {
            double xPosition = e.getX() - timelineTitles.getWidth();
            jumpTo(xPosition);
        });
        titleBarTop.setOnMouseDragged(e -> {
            double xPosition = e.getX() - timelineTitles.getWidth();
            jumpTo(xPosition);
        });

        timeLineScrollPane.setContent(timelineGroup);

        gridPane.add(timelineTitles, 0, 0);
        gridPane.add(timeLineScrollPane, 1, 0);

        borderPane.setCenter(gridPane);

        return borderPane;
    }

    private void jumpTo(double xPosition) {
        TimelinePosition position = timelineState.pixelsToSeconds(xPosition);
        uiTimelineManager.jumpAbsolute(position.getSeconds());
    }

    public void updateLine(TimelinePosition position) {
        timelineState.setLinePosition(position);
    }

}
