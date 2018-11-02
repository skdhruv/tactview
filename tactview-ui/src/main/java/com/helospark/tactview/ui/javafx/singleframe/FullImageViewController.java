package com.helospark.tactview.ui.javafx.singleframe;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.repository.ProjectRepository;
import com.helospark.tactview.core.timeline.TimelineManager;
import com.helospark.tactview.core.timeline.TimelineManagerFramesRequest;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.util.logger.Slf4j;
import com.helospark.tactview.ui.javafx.ByteBufferToImageConverter;
import com.helospark.tactview.ui.javafx.UiTimelineManager;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

@Component
public class FullImageViewController {
    private UiTimelineManager uiTimelineManager;
    private TimelineManager timelineManager;
    private ProjectRepository projectRepository;
    private ByteBufferToImageConverter byteBufferToImageConverter;
    @Slf4j
    private Logger logger;

    public FullImageViewController(UiTimelineManager uiTimelineManager, TimelineManager timelineManager, ProjectRepository projectRepository, ByteBufferToImageConverter byteBufferToImageConverter) {
        this.uiTimelineManager = uiTimelineManager;
        this.timelineManager = timelineManager;
        this.projectRepository = projectRepository;
        this.byteBufferToImageConverter = byteBufferToImageConverter;
    }

    public void renderFullScreenAtCurrentLocation() {
        TimelinePosition currentPosition = uiTimelineManager.getCurrentPosition();

        int height = projectRepository.getHeight();
        int width = projectRepository.getWidth();

        TimelineManagerFramesRequest frameRequest = TimelineManagerFramesRequest.builder()
                .withFrameBufferSize(1)
                .withPosition(currentPosition)
                .withScale(1.0)
                .withPreviewWidth(width)
                .withPreviewHeight(height)
                .build();

        CompletableFuture.supplyAsync(() -> {
            ByteBuffer image = timelineManager.getSingleFrame(frameRequest);
            return byteBufferToImageConverter.convertToJavaxImage(image, width, height);
        }).exceptionally(e -> {
            logger.error("Error rendering image", e);
            return null;
        }).thenAccept(image -> {
            Platform.runLater(() -> {
                ImageShowDialog dialog = new ImageShowDialog(image);
                dialog.show();
            });
        });

    }

    static class ImageShowDialog {
        private Stage stage;

        public ImageShowDialog(Image image) {
            ImageView imageView = new ImageView();
            imageView.setImage(image);

            ScrollPane root = new ScrollPane();
            root.setContent(imageView);

            Scene dialog = new Scene(root);
            stage = new Stage();
            stage.setTitle("FullScreenRender");
            stage.setScene(dialog);
            stage.show();
            stage.toFront();
        }

        public void show() {
            stage.show();
        }
    }

}