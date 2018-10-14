package com.helospark.tactview.core.timeline;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.helospark.tactview.core.decoder.VisualMediaMetadata;
import com.helospark.tactview.core.decoder.framecache.GlobalMemoryManagerAccessor;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.DoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.DoubleProvider;

public abstract class VisualTimelineClip extends TimelineClip {
    protected VisualMediaMetadata mediaMetadata;

    protected DoubleProvider translateXProvider;
    protected DoubleProvider translateYProvider;

    public VisualTimelineClip(VisualMediaMetadata visualMediaMetadata, TimelineInterval interval, TimelineClipType type) {
        super(interval, type);
        this.mediaMetadata = visualMediaMetadata;
    }

    public ClipFrameResult getFrame(GetFrameRequest request) {
        double scale = request.getScale();
        TimelinePosition position = request.getPosition().add(renderOffset);
        int width = (int) (mediaMetadata.getWidth() * scale);
        int height = (int) (mediaMetadata.getHeight() * scale);
        TimelinePosition relativePosition = position.from(getInterval().getStartPosition());

        ByteBuffer frame = requestFrame(relativePosition, width, height);
        ClipFrameResult frameResult = new ClipFrameResult(frame, width, height);

        return applyEffects(relativePosition, frameResult);
    }

    protected ClipFrameResult applyEffects(TimelinePosition relativePosition, ClipFrameResult frameResult) {
        List<StatelessVideoEffect> actualEffects = getEffectsAt(relativePosition, StatelessVideoEffect.class);

        for (StatelessVideoEffect effect : actualEffects) {
            StatelessEffectRequest request = StatelessEffectRequest.builder()
                    .withClipPosition(relativePosition)
                    .withEffectPosition(relativePosition.from(effect.interval.getStartPosition()))
                    .withCurrentFrame(frameResult)
                    .build();

            ClipFrameResult appliedEffectsResult = effect.createFrame(request);

            GlobalMemoryManagerAccessor.memoryManager.returnBuffer(request.getCurrentFrame().getBuffer());

            frameResult = appliedEffectsResult; // todo: free up bytebuffer
        }
        return frameResult;
    }

    public abstract ByteBuffer requestFrame(TimelinePosition position, int width, int height);

    public void addEffect(StatelessEffect effect) {
        if (effectChannels.isEmpty()) {
            NonIntersectingIntervalList<StatelessEffect> newList = new NonIntersectingIntervalList<>();
            effectChannels.add(newList);
            newList.addInterval(effect);
        } else {
            effectChannels.get(0).addInterval(effect);
        }
    }

    public List<NonIntersectingIntervalList<StatelessEffect>> getEffectChannels() {
        return effectChannels;
    }

    public abstract VisualMediaMetadata getMediaMetadata();

    public int getXPosition(TimelinePosition timelinePosition) {
        return (int) (translateXProvider.getValueAt(timelinePosition) * mediaMetadata.getWidth());
    }

    public int getYPosition(TimelinePosition timelinePosition) {
        return (int) (translateYProvider.getValueAt(timelinePosition) * mediaMetadata.getHeight());
    }

    @Override
    public List<ValueProviderDescriptor> getDescriptors() {
        List<ValueProviderDescriptor> result = new ArrayList<>();
        translateXProvider = new DoubleProvider(-10, 10, new DoubleInterpolator(TimelinePosition.ofZero(), 0.0));
        translateYProvider = new DoubleProvider(-10, 10, new DoubleInterpolator(TimelinePosition.ofZero(), 0.0));

        ValueProviderDescriptor translateXDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(translateXProvider)
                .withName("translateX")
                .build();

        ValueProviderDescriptor translateYDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(translateYProvider)
                .withName("translateY")
                .build();

        result.add(translateXDescriptor);
        result.add(translateYDescriptor);

        return result;
    }

}
