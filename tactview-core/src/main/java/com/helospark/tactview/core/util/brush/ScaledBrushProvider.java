package com.helospark.tactview.core.util.brush;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.decoder.framecache.GlobalMemoryManagerAccessor;
import com.helospark.tactview.core.timeline.effect.scale.service.ScaleRequest;
import com.helospark.tactview.core.timeline.effect.scale.service.ScaleService;
import com.helospark.tactview.core.timeline.image.ClipImage;
import com.helospark.tactview.core.timeline.image.ReadOnlyClipImage;
import com.helospark.tactview.core.util.cacheable.Cacheable;

@Component
public class ScaledBrushProvider {
    private static final Logger logger = LoggerFactory.getLogger(ScaledBrushProvider.class);
    private RawBrushProvider rawBrushProvider;
    private ScaleService scaleService;

    public ScaledBrushProvider(RawBrushProvider rawBrushProvider, ScaleService scaleService) {
        this.rawBrushProvider = rawBrushProvider;
        this.scaleService = scaleService;
    }

    @Cacheable(cacheTimeInMilliseconds = 600000, size = 100)
    public ClipImage getBrushImage(GetBrushRequest brushRequest) {
        logger.info("Loading brush ", brushRequest);
        rawBrushProvider.getBrush(brushRequest.getFilename());

        GimpBrush brush = rawBrushProvider.getBrush(brushRequest.getFilename());

        return createScaledBrush(brush, brushRequest);
    }

    private ClipImage createScaledBrush(GimpBrush brush, GetBrushRequest brushRequest) {
        ReadOnlyClipImage brushImage = createBrushImage(brush);

        ScaleRequest scaleRequest = ScaleRequest.builder()
                .withImage(brushImage)
                .withNewWidth(brushRequest.getWidth())
                .withNewHeight(brushRequest.getHeight())
                .build();

        ClipImage scaledBrush = scaleService.createScaledImage(scaleRequest);

        GlobalMemoryManagerAccessor.memoryManager.returnBuffer(brushImage.getBuffer());

        return scaledBrush;
    }

    private ReadOnlyClipImage createBrushImage(GimpBrush brush) {
        ClipImage resultBrush = ClipImage.fromSize(brush.width, brush.height);

        for (int y = 0; y < brush.height; ++y) {
            for (int x = 0; x < brush.width; ++x) {
                resultBrush.setAlpha(ClipImage.signedToUnsignedByte(brush.data[y * brush.width + x]), x, y);
            }
        }

        return resultBrush;
    }

}