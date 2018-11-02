package com.helospark.tactview.ui.javafx.uicomponents;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.AddClipRequest;
import com.helospark.tactview.core.timeline.TimelineLength;
import com.helospark.tactview.core.timeline.TimelineManager;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.util.logger.Slf4j;
import com.helospark.tactview.ui.javafx.UiCommandInterpreterService;
import com.helospark.tactview.ui.javafx.commands.impl.AddClipsCommand;
import com.helospark.tactview.ui.javafx.commands.impl.ClipMovedCommand;
import com.helospark.tactview.ui.javafx.commands.impl.ClipResizedCommand;
import com.helospark.tactview.ui.javafx.commands.impl.EffectResizedCommand;
import com.helospark.tactview.ui.javafx.repository.DragRepository;
import com.helospark.tactview.ui.javafx.repository.DragRepository.DragDirection;
import com.helospark.tactview.ui.javafx.repository.drag.ClipDragInformation;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;

@Component
public class TimelineDragAndDropHandler {
    private TimelineManager timelineManager;
    private UiCommandInterpreterService commandInterpreter;
    private TimelineState timelineState;
    private DragRepository dragRepository;

    @Slf4j
    private Logger logger;

    private boolean isLoadingInprogress = false;

    public TimelineDragAndDropHandler(TimelineManager timelineManager, UiCommandInterpreterService commandInterpreter, TimelineState timelineState,
            DragRepository dragRepository) {
        this.timelineManager = timelineManager;
        this.commandInterpreter = commandInterpreter;
        this.timelineState = timelineState;
        this.dragRepository = dragRepository;
    }

    public void addDragAndDrop(Node timeline, Pane timelineRow, String channelId) {
        timeline.setOnDragEntered(event -> {
            Dragboard db = event.getDragboard();

            List<File> dbFiles = db.getFiles();
            String dbString = db.getString();
            double currentX = event.getX();
            AddClipRequest addClipRequest = addClipRequest(channelId, dbFiles, dbString, currentX);

            if (!isLoadingInprogress && dragRepository.currentlyDraggedClip() == null && ((dbFiles != null && !dbFiles.isEmpty()) || isStringClip(db))) {
                isLoadingInprogress = true;
                commandInterpreter.sendWithResult(new AddClipsCommand(addClipRequest, timelineManager))
                        .exceptionally(e -> {
                            logger.warn("Error while adding clip", e);
                            isLoadingInprogress = false;
                            return null;
                        })
                        .thenAccept(res -> {
                            Platform.runLater(() -> {
                                try {
                                    String addedClipId = res.getAddedClipId();
                                    logger.debug("Clip added " + addedClipId);
                                    Pane addedClip = timelineState.findClipById(addedClipId).orElseThrow(() -> new RuntimeException("Not found"));
                                    ClipDragInformation clipDragInformation = new ClipDragInformation(addedClip, res.getRequestedPosition(), addedClipId, channelId, 0);
                                    dragRepository.onClipDragged(clipDragInformation);
                                } catch (Exception e1) {
                                    logger.warn("Error while adding clip", e1);
                                } finally {
                                    isLoadingInprogress = false;
                                }
                            });
                        });
            }
        });

        timeline.setOnDragOver(event -> {
            if (dragRepository.currentlyDraggedClip() != null) {
                event.acceptTransferModes(TransferMode.MOVE);
                if (dragRepository.isResizing()) {
                    resizeClip(event, false);
                } else {
                    moveClip(event, channelId, false);
                }
            }
            if (dragRepository.currentEffectDragInformation() != null) {
                if (dragRepository.isResizing()) {
                    resizeEffect(event, false);
                } else {
                    moveEffect(event, false);
                }
                event.acceptTransferModes(TransferMode.LINK);
            }
        });

        timeline.setOnDragDropped(event -> {
            if (dragRepository.currentlyDraggedClip() != null) {
                if (dragRepository.isResizing()) {
                    resizeClip(event, true);
                } else {
                    moveClip(event, channelId, true);
                }
                event.getDragboard().clear();
                dragRepository.clearClipDrag();
            }
            if (dragRepository.currentEffectDragInformation() != null) {
                if (dragRepository.isResizing()) {
                    resizeEffect(event, true);
                } else {
                    moveEffect(event, true);
                }
                event.getDragboard().clear();
                dragRepository.clearEffectDrag();
            }
        });

    }

    private AddClipRequest addClipRequest(String channelId, List<File> dbFiles, String dbString, double currentX) {
        String filePath = extractFilePathOrNull(dbFiles);
        String proceduralClipId = extractProceduralEffectOrNull(dbString);
        TimelinePosition position = timelineState.pixelsToSeconds(currentX);

        return AddClipRequest.builder()
                .withChannelId(channelId)
                .withPosition(position)
                .withFilePath(filePath)
                .withProceduralClipId(proceduralClipId)
                .build();
    }

    private CompletableFuture<ClipMovedCommand> moveClip(DragEvent event, String channelId, boolean revertable) {
        ClipDragInformation currentlyDraggedEffect = dragRepository.currentlyDraggedClip();
        if (currentlyDraggedEffect != null) {
            String clipId = currentlyDraggedEffect.getClipId();

            TimelinePosition position = timelineState.pixelsToSeconds(event.getX() - currentlyDraggedEffect.getAnchorPointX());

            ClipMovedCommand command = ClipMovedCommand.builder()
                    .withIsRevertable(revertable)
                    .withClipId(clipId)
                    .withNewPosition(position)
                    .withPreviousPosition(currentlyDraggedEffect.getOriginalPosition())
                    .withOriginalChannelId(currentlyDraggedEffect.getOriginalChannelId())
                    .withNewChannelId(channelId)
                    .withTimelineManager(timelineManager)
                    .withEnableJumpingToSpecialPosition(true)
                    .withMaximumJumpLength(new TimelineLength(timelineState.pixelsToSeconds(30).getSeconds()))
                    .build();

            return commandInterpreter.sendWithResult(command);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void resizeClip(DragEvent event, boolean b) {
        ClipDragInformation currentlyDraggedEffect = dragRepository.currentlyDraggedClip();
        if (currentlyDraggedEffect != null) {
            String clipId = currentlyDraggedEffect.getClipId();
            TimelinePosition position = timelineState.pixelsToSeconds(event.getX());

            ClipResizedCommand command = ClipResizedCommand.builder()
                    .withClipId(clipId)
                    .withLeft(dragRepository.getDragDirection().equals(DragRepository.DragDirection.LEFT))
                    .withPosition(position)
                    .withRevertable(b)
                    .withTimelineManager(timelineManager)
                    .build();
            commandInterpreter.sendWithResult(command);
        }
    }

    private boolean isStringClip(Dragboard db) {
        String id = db.getString();
        if (id != null) {
            return id.startsWith("clip:");
        }
        return false;
    }

    private String extractProceduralEffectOrNull(String dbString) {
        String proceduralClipId = dbString;
        if (proceduralClipId != null && proceduralClipId.startsWith("clip:")) {
            proceduralClipId = proceduralClipId.replaceFirst("clip:", "");
        } else {
            proceduralClipId = null;
        }
        return proceduralClipId;
    }

    private String extractFilePathOrNull(List<File> dbFiles) {
        if (dbFiles == null) {
            return null;
        }
        return dbFiles.stream().findFirst().map(f -> f.getAbsolutePath()).orElse(null);
    }

    private void resizeEffect(DragEvent event, boolean revertable) {
        EffectDragInformation draggedEffect = dragRepository.currentEffectDragInformation();

        double x = event.getX();

        System.out.println("Move to " + x);

        EffectResizedCommand resizedCommand = EffectResizedCommand.builder()
                .withEffectId(draggedEffect.getEffectId())
                .withLeft(dragRepository.getDragDirection().equals(DragDirection.LEFT))
                .withGlobalPosition(timelineState.pixelsToSeconds(x))
                .withRevertable(revertable)
                .withTimelineManager(timelineManager)
                .build();

        commandInterpreter.sendWithResult(resizedCommand);
    }

    private void moveEffect(DragEvent event, boolean revertable) {
        EffectDragInformation draggedEffect = dragRepository.currentEffectDragInformation();
        TimelinePosition position = timelineState.pixelsToSeconds(event.getX());

        EffectMovedCommand command = EffectMovedCommand.builder()
                .withEffectId(draggedEffect.getEffectId())
                .withOriginalClipId(draggedEffect.getClipId())
                .withNewClipId("0") // TODO
                .withGlobalNewPosition(position)
                .withRevertable(revertable)
                .withOriginalPosition(draggedEffect.getOriginalPosition())
                .withTimelineManager(timelineManager)
                .build();
        commandInterpreter.sendWithResult(command);
    }

}
