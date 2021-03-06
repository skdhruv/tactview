package com.helospark.tactview.core.timeline;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.helospark.tactview.core.clone.CloneRequestMetadata;
import com.helospark.tactview.core.decoder.VisualMediaMetadata;
import com.helospark.tactview.core.decoder.framecache.GlobalMemoryManagerAccessor;
import com.helospark.tactview.core.save.LoadMetadata;
import com.helospark.tactview.core.timeline.alignment.AlignmentValueListElement;
import com.helospark.tactview.core.timeline.blendmode.BlendModeStrategy;
import com.helospark.tactview.core.timeline.blendmode.BlendModeStrategyAccessor;
import com.helospark.tactview.core.timeline.blendmode.BlendModeValueListElement;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.hint.MovementType;
import com.helospark.tactview.core.timeline.effect.interpolation.hint.RenderTypeHint;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.MultiKeyframeBasedDoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.StepStringInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.bezier.BezierDoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.factory.function.impl.StepInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.factory.functional.doubleinterpolator.impl.ConstantInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.BooleanProvider;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.DoubleProvider;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.PointProvider;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.SizeFunction;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.ValueListProvider;
import com.helospark.tactview.core.timeline.image.ClipImage;
import com.helospark.tactview.core.timeline.image.ReadOnlyClipImage;
import com.helospark.tactview.core.util.ReflectionUtil;

public abstract class VisualTimelineClip extends TimelineClip {
    protected VisualMediaMetadata mediaMetadata;

    protected PointProvider translatePointProvider;
    protected DoubleProvider globalClipAlphaProvider;
    protected BooleanProvider changeClipLengthProvider;
    protected DoubleProvider timeScaleProvider;
    protected BooleanProvider enabledProvider;
    protected BooleanProvider reverseTimeProvider;
    protected ValueListProvider<AlignmentValueListElement> verticallyCenteredProvider;
    protected ValueListProvider<AlignmentValueListElement> horizontallyCenteredProvider;

    protected VisualMediaSource backingSource;
    protected ValueListProvider<BlendModeValueListElement> blendModeProvider;

    protected TimelineInterval cachedInterval = null;
    protected BigDecimal lengthCache = null;

    public VisualTimelineClip(VisualMediaMetadata mediaMetadata, TimelineInterval interval, TimelineClipType type) {
        super(interval, type);
        this.mediaMetadata = mediaMetadata;
    }

    public VisualTimelineClip(VisualTimelineClip clip, CloneRequestMetadata cloneRequestMetadata) {
        super(clip, cloneRequestMetadata);
        ReflectionUtil.copyOrCloneFieldFromTo(clip, this, VisualTimelineClip.class);
    }

    public VisualTimelineClip(VisualMediaMetadata metadata, JsonNode savedClip, LoadMetadata loadMetadata) {
        super(savedClip, loadMetadata);
        this.mediaMetadata = metadata;
    }

    public ReadOnlyClipImage getFrame(GetFrameRequest request) {
        return getFrameInternal(request);
    }

    protected ReadOnlyClipImage getFrameInternal(GetFrameRequest request) {
        double scale = request.getScale();
        int width = (int) (mediaMetadata.getWidth() * scale);
        int height = (int) (mediaMetadata.getHeight() * scale);
        TimelinePosition rateAdjustedPosition = calculatePositionToRender(request);

        RequestFrameParameter frameRequest = RequestFrameParameter.builder()
                .withPosition(rateAdjustedPosition)
                .withWidth(width)
                .withHeight(height)
                .withUseApproximatePosition(request.useApproximatePosition())
                .withLowResolutionPreview(request.isLowResolutionPreview())
                .build();

        ByteBuffer frame = requestFrame(frameRequest);
        ClipImage frameResult = new ClipImage(frame, width, height);

        return applyEffects(rateAdjustedPosition, frameResult, request);
    }

    protected TimelinePosition calculatePositionToRender(GetFrameRequest request) {
        TimelinePosition relativePosition = request.calculateRelativePositionFrom(this);

        boolean reverse = reverseTimeProvider.getValueAt(TimelinePosition.ofZero());

        if (reverse) {
            TimelinePosition endPosition = interval.getLength().toPosition().add(renderOffset);
            TimelinePosition unscaledPosition = endPosition.subtract(relativePosition);
            BigDecimal integrated = timeScaleProvider.integrate(unscaledPosition, endPosition);
            return endPosition.subtract(new TimelinePosition(integrated));
        } else {
            TimelinePosition unscaledPosition = relativePosition.add(renderOffset);
            BigDecimal integrated = timeScaleProvider.integrate(renderOffset.toPosition(), unscaledPosition);
            return renderOffset.toPosition().add(integrated);
        }

    }

    protected ReadOnlyClipImage applyEffects(TimelinePosition relativePosition, ReadOnlyClipImage frameResult, GetFrameRequest frameRequest) {
        if (frameRequest.isApplyEffects()) {
            List<StatelessVideoEffect> actualEffects = getEffectsAt(relativePosition, StatelessVideoEffect.class);

            int effectChannelIndex = 0;
            for (StatelessVideoEffect effect : actualEffects) {
                if (frameRequest.getApplyEffectsLessThanEffectChannel().isPresent() && effectChannelIndex >= frameRequest.getApplyEffectsLessThanEffectChannel().get()) {
                    break;
                }

                if (effect.isEnabledAt(frameRequest.getRelativePosition())) {
                    StatelessEffectRequest request = StatelessEffectRequest.builder()
                            .withClipPosition(relativePosition)
                            .withEffectPosition(relativePosition.from(effect.interval.getStartPosition()))
                            .withCurrentFrame(frameResult)
                            .withScale(frameRequest.getScale())
                            .withCanvasWidth(frameRequest.getExpectedWidth())
                            .withCanvasHeight(frameRequest.getExpectedHeight())
                            .withRequestedClips(frameRequest.getRequestedClips())
                            .withRequestedChannelClips(frameRequest.getRequestedChannelClips())
                            .withCurrentTimelineClip(this)
                            .withEffectChannel(effectChannelIndex)
                            .build();

                    ReadOnlyClipImage appliedEffectsResult = effect.createFrameExternal(request);

                    GlobalMemoryManagerAccessor.memoryManager.returnBuffer(frameResult.getBuffer());

                    frameResult = appliedEffectsResult;
                }
                ++effectChannelIndex;
            }
        }
        return frameResult;
    }

    public abstract ByteBuffer requestFrame(RequestFrameParameter request);

    @Override
    public List<NonIntersectingIntervalList<StatelessEffect>> getEffectChannels() {
        return effectChannels;
    }

    public abstract VisualMediaMetadata getMediaMetadata();

    public int getXPosition(TimelinePosition timelinePosition, double scale) {
        return (int) (translatePointProvider.getValueAt(timelinePosition).x * scale);
    }

    public int getYPosition(TimelinePosition timelinePosition, double scale) {
        return (int) (translatePointProvider.getValueAt(timelinePosition).y * scale);
    }

    public BiFunction<Integer, Integer, Integer> getVerticalAlignment(TimelinePosition timelinePosition) {
        return verticallyCenteredProvider.getValueAt(timelinePosition).getFunction();
    }

    public BiFunction<Integer, Integer, Integer> getHorizontalAlignment(TimelinePosition timelinePosition) {
        return horizontallyCenteredProvider.getValueAt(timelinePosition).getFunction();
    }

    @Override
    protected void initializeValueProvider() {
        DoubleProvider translateXProvider = new DoubleProvider(SizeFunction.IMAGE_SIZE, new BezierDoubleInterpolator(0.0));
        DoubleProvider translateYProvider = new DoubleProvider(SizeFunction.IMAGE_SIZE, new BezierDoubleInterpolator(0.0));
        translateXProvider.setScaleDependent();
        translateYProvider.setScaleDependent();

        translatePointProvider = new PointProvider(translateXProvider, translateYProvider);
        globalClipAlphaProvider = new DoubleProvider(0.0, 1.0, new MultiKeyframeBasedDoubleInterpolator(1.0));
        enabledProvider = new BooleanProvider(new MultiKeyframeBasedDoubleInterpolator(1.0, new StepInterpolator()));
        blendModeProvider = new ValueListProvider<>(createBlendModes(), new StepStringInterpolator("normal"));
        horizontallyCenteredProvider = new ValueListProvider<>(createHorizontalAlignments(), new StepStringInterpolator("left"));
        verticallyCenteredProvider = new ValueListProvider<>(createVerticalAlignments(), new StepStringInterpolator("top"));
        timeScaleProvider = new DoubleProvider(0, 5, new MultiKeyframeBasedDoubleInterpolator(1.0));
        changeClipLengthProvider = new BooleanProvider(new ConstantInterpolator(1.0));
        reverseTimeProvider = new BooleanProvider(new ConstantInterpolator(0.0));
    }

    private List<AlignmentValueListElement> createVerticalAlignments() {
        return List.of(
                new AlignmentValueListElement("top", (frameHeight, resultHeight) -> 0),
                new AlignmentValueListElement("center", (frameHeight, resultHeight) -> (resultHeight - frameHeight) / 2),
                new AlignmentValueListElement("bottom", (frameHeight, resultHeight) -> resultHeight - frameHeight));
    }

    private List<AlignmentValueListElement> createHorizontalAlignments() {
        return List.of(
                new AlignmentValueListElement("left", (frameWidth, resultWidth) -> 0),
                new AlignmentValueListElement("center", (frameWidth, resultWidth) -> (resultWidth - frameWidth) / 2),
                new AlignmentValueListElement("right", (frameWidth, resultWidth) -> resultWidth - frameWidth));
    }

    @Override
    public List<ValueProviderDescriptor> getDescriptorsInternal() {
        List<ValueProviderDescriptor> result = new ArrayList<>();

        ValueProviderDescriptor translateDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(translatePointProvider)
                .withRenderHints(Map.of(RenderTypeHint.TYPE, MovementType.RELATIVE))
                .withName("translate")
                .withGroup("common")
                .build();
        ValueProviderDescriptor centerVerticallyDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(verticallyCenteredProvider)
                .withName("vertical alignment")
                .withGroup("common")
                .build();
        ValueProviderDescriptor centerHorizontallyDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(horizontallyCenteredProvider)
                .withName("horizontal alignment")
                .withGroup("common")
                .build();

        ValueProviderDescriptor globalClipAlphaDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(globalClipAlphaProvider)
                .withName("Global clip alpha")
                .withGroup("common")
                .build();

        ValueProviderDescriptor enabledDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(enabledProvider)
                .withName("Enabled")
                .withGroup("common")
                .build();
        ValueProviderDescriptor blendModeDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(blendModeProvider)
                .withName("Blend mode")
                .withGroup("common")
                .build();
        ValueProviderDescriptor timeScaleProviderDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(timeScaleProvider)
                .withName("clip speed")
                .withGroup("speed")
                .build();
        ValueProviderDescriptor changeClipLengthProviderDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(changeClipLengthProvider)
                .withName("change clip length")
                .withGroup("speed")
                .build();
        ValueProviderDescriptor reverseTimeProviderDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(reverseTimeProvider)
                .withName("Reverse clip")
                .withGroup("speed")
                .build();

        result.add(translateDescriptor);
        result.add(centerHorizontallyDescriptor);
        result.add(centerVerticallyDescriptor);
        result.add(globalClipAlphaDescriptor);
        result.add(enabledDescriptor);
        result.add(blendModeDescriptor);
        result.add(timeScaleProviderDescriptor);
        result.add(changeClipLengthProviderDescriptor);
        result.add(reverseTimeProviderDescriptor);

        return result;
    }

    private List<BlendModeValueListElement> createBlendModes() {
        return BlendModeStrategyAccessor.getStrategies()
                .stream()
                .map(blendMode -> new BlendModeValueListElement(blendMode.getId(), blendMode.getId(), blendMode))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabled(TimelinePosition position) {
        return enabledProvider.getValueAt(position);
    }

    public double getAlpha(TimelinePosition position) {
        return globalClipAlphaProvider.getValueAt(position);
    }

    public BlendModeStrategy getBlendModeAt(TimelinePosition position) {
        TimelinePosition relativePosition = position.from(this.interval.getStartPosition());
        relativePosition = relativePosition.add(renderOffset);
        return blendModeProvider.getValueAt(relativePosition).getBlendMode();
    }

    @Override
    protected void generateSavedContentInternal(Map<String, Object> savedContent) {
        savedContent.put("backingFile", backingSource.getBackingFile());
    }

    @Override
    public TimelineInterval getInterval() {
        Boolean changeClipLength = changeClipLengthProvider.getValueAt(TimelinePosition.ofZero());
        if (changeClipLength) {
            TimelineInterval originalInterval = this.interval;
            TimelinePosition originalStartPosition = originalInterval.getStartPosition();
            if (lengthCache == null || !this.interval.equals(cachedInterval)) {
                lengthCache = timeScaleProvider.integrateUntil(TimelinePosition.ofZero(), originalInterval.getLength(), new BigDecimal("10000"));
                cachedInterval = originalInterval;
            }
            return new TimelineInterval(originalStartPosition, new TimelineLength(lengthCache));
        } else {
            return this.interval;
        }
    }

    @Override
    public void effectChanged(EffectChangedRequest request) {
        super.effectChanged(request);
        if (request.id.equals(timeScaleProvider.getId())) {
            lengthCache = null;
            //                    ((PercentAwareMultiKeyframeBasedDoubleInterpolator) timeScaleProvider.getInterpolator()).resizeTo(getInterval().getLength());
        }
    }
}
