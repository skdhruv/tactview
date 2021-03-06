package com.helospark.tactview.core.timeline.effect.interpolation.provider;

import java.util.Arrays;
import java.util.List;

import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.interpolation.KeyframeableEffect;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.MultiKeyframeBasedDoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.pojo.DoubleRange;
import com.helospark.tactview.core.util.DesSerFactory;

public class DoubleRangeProvider extends CompositeKeyframeableEffect {
    DoubleProvider lowEndProvider;
    DoubleProvider highEndProvider;

    public DoubleRangeProvider(DoubleProvider lowEnd, DoubleProvider highEnd) {
        super(List.of(lowEnd, highEnd));
        this.lowEndProvider = lowEnd;
        this.highEndProvider = highEnd;
    }

    public DoubleProvider getLowEnd() {
        return lowEndProvider;
    }

    public DoubleProvider getHighEnd() {
        return highEndProvider;
    }

    public double getMin() {
        return lowEndProvider.getMin();
    }

    public double getMax() {
        return lowEndProvider.getMax();
    }

    @Override
    public Class<? extends DesSerFactory<? extends KeyframeableEffect>> generateSerializableContent() {
        return DoubleRangeProviderFactory.class;
    }

    @Override
    public DoubleRange getValueAt(TimelinePosition position) {
        return new DoubleRange(lowEndProvider.getValueAt(position), highEndProvider.getValueAt(position));
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public KeyframeableEffect deepClone() {
        return new DoubleRangeProvider(lowEndProvider.deepClone(), highEndProvider.deepClone());
    }

    @Override
    public List<KeyframeableEffect> getChildren() {
        return Arrays.asList(lowEndProvider, highEndProvider);
    }

    public static DoubleRangeProvider createDefaultDoubleRangeProvider(double min, double max, double defaultLow, double defaultHigh) {
        DoubleProvider valueMapperMin = new DoubleProvider(min, max, new MultiKeyframeBasedDoubleInterpolator(defaultLow));
        DoubleProvider valueMapperMax = new DoubleProvider(min, max, new MultiKeyframeBasedDoubleInterpolator(defaultHigh));

        return new DoubleRangeProvider(valueMapperMin, valueMapperMax);
    }
}
