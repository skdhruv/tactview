package com.helospark.tactview.ui.javafx;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.repository.ProjectRepository;
import com.helospark.tactview.core.timeline.AudioVideoFragment;
import com.helospark.tactview.core.timeline.TimelineManagerFramesRequest;
import com.helospark.tactview.core.timeline.TimelineManagerRenderService;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.ui.javafx.audio.JavaByteArrayConverter;
import com.helospark.tactview.ui.javafx.repository.UiProjectRepository;
import com.helospark.tactview.ui.javafx.util.ByteBufferToJavaFxImageConverter;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

@Component
public class PlaybackController {
    public static final int CHANNELS = 2;
    public static final int FREQUENCY = 44100;
    public static final int BYTES = 2;
    private final TimelineManagerRenderService timelineManager;
    private final UiProjectRepository uiProjectRepository;
    private final ProjectRepository projectRepository;
    private final ByteBufferToJavaFxImageConverter byteBufferToImageConverter;
    private final JavaByteArrayConverter javaByteArrayConverter;
    private final UiPlaybackPreferenceRepository uiPlaybackPreferenceRepository;

    public PlaybackController(TimelineManagerRenderService timelineManager, UiProjectRepository uiProjectRepository, ProjectRepository projectRepository,
            ByteBufferToJavaFxImageConverter byteBufferToImageConverter, JavaByteArrayConverter javaByteArrayConverter,
            UiPlaybackPreferenceRepository uiPlaybackPreferenceRepository) {
        this.timelineManager = timelineManager;
        this.uiProjectRepository = uiProjectRepository;
        this.byteBufferToImageConverter = byteBufferToImageConverter;
        this.javaByteArrayConverter = javaByteArrayConverter;
        this.projectRepository = projectRepository;
        this.uiPlaybackPreferenceRepository = uiPlaybackPreferenceRepository;
    }

    public JavaDisplayableAudioVideoFragment getVideoFrameAt(TimelinePosition position) {
        Image imageWithEffects = getImageWithEffectEnabled(position, true);

        Image result;
        if (uiPlaybackPreferenceRepository.isHalfEffect()) {
            Image javafxImageWithoutEffects = getImageWithEffectEnabled(position, false);
            result = mergeImages(imageWithEffects, javafxImageWithoutEffects);
        } else {
            result = imageWithEffects;
        }

        return new JavaDisplayableAudioVideoFragment(result, new byte[0]);
    }

    private Image mergeImages(Image javafxImage, Image javafxImageWithoutEffects) {
        int width = (int) javafxImageWithoutEffects.getWidth();
        int height = (int) javafxImage.getHeight();
        WritableImage result = new WritableImage(width, height);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width / 2; ++x) {
                Color color = javafxImage.getPixelReader().getColor(x, y);
                result.getPixelWriter().setColor(x, y, color);
            }
            for (int x = width / 2; x < width; ++x) {
                Color color = javafxImageWithoutEffects.getPixelReader().getColor(x, y);
                result.getPixelWriter().setColor(x, y, color);
            }
        }

        return result;
    }

    private Image getImageWithEffectEnabled(TimelinePosition position, boolean enableEffect) {
        Integer width = uiProjectRepository.getPreviewWidth();
        Integer height = uiProjectRepository.getPreviewHeight();
        TimelineManagerFramesRequest request = TimelineManagerFramesRequest.builder()
                .withPosition(position)
                .withScale(uiProjectRepository.getScaleFactor())
                .withPreviewWidth(width)
                .withPreviewHeight(height)
                .withNeedSound(false)
                .withNeedVideo(true)
                .withLowResolutionPreview(true)
                .withEffectsEnabled(enableEffect)
                .build();
        AudioVideoFragment frame = timelineManager.getFrame(request);
        Image javafxImage = byteBufferToImageConverter.convertToJavafxImage(frame.getVideoResult().getBuffer(), width, height);
        frame.free();
        return javafxImage;
    }

    public byte[] getAudioFrameAt(TimelinePosition position, int samples) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < samples; ++i) {
                AudioVideoFragment frame = getSingleAudioFrameAtPosition(position.add(projectRepository.getFrameTime().multiply(BigDecimal.valueOf(i))));
                byte[] buffer = javaByteArrayConverter.convert(frame.getAudioResult(), BYTES, FREQUENCY, CHANNELS); // move data to repository

                frame.free();

                baos.write(buffer);
            }

            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public AudioVideoFragment getSingleAudioFrameAtPosition(TimelinePosition position) {
        Integer width = uiProjectRepository.getPreviewWidth();
        Integer height = uiProjectRepository.getPreviewHeight();
        TimelineManagerFramesRequest request = TimelineManagerFramesRequest.builder()
                .withPosition(position)
                .withScale(uiProjectRepository.getScaleFactor())
                .withPreviewWidth(width)
                .withPreviewHeight(height)
                .withNeedSound(true)
                .withNeedVideo(false)
                .build();
        AudioVideoFragment frame = timelineManager.getFrame(request);
        return frame;
    }
}
