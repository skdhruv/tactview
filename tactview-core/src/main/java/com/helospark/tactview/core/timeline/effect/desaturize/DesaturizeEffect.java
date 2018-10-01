package com.helospark.tactview.core.timeline.effect.desaturize;

import java.util.Collections;
import java.util.List;

import com.helospark.tactview.core.timeline.ClipFrameResult;
import com.helospark.tactview.core.timeline.StatelessVideoEffect;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;

public class DesaturizeEffect extends StatelessVideoEffect {

    public DesaturizeEffect(TimelineInterval interval) {
        super(interval);
    }

    @Override
    public void fillFrame(ClipFrameResult result, StatelessEffectRequest request) {
        ClipFrameResult currentFrame = request.getCurrentFrame();
        int[] pixel = new int[4];
        int[] resultPixel = new int[4];
        for (int i = 0; i < currentFrame.getHeight(); ++i) {
            for (int j = 0; j < currentFrame.getWidth(); ++j) {
                currentFrame.getPixelComponents(pixel, j, i);
                int desaturized = (pixel[0] + pixel[1] + pixel[2]) / 3;
                resultPixel[0] = desaturized;
                resultPixel[1] = desaturized;
                resultPixel[2] = desaturized;
                resultPixel[3] = pixel[3];
                result.setPixel(resultPixel, j, i);
            }
        }
    }

    @Override
    public List<ValueProviderDescriptor> getValueProviders() {
        return Collections.emptyList();
    }

}
