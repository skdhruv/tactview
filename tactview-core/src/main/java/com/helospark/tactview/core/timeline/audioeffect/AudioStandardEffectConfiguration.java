package com.helospark.tactview.core.timeline.audioeffect;

import java.util.List;

import com.helospark.lightdi.annotation.Bean;
import com.helospark.lightdi.annotation.Configuration;
import com.helospark.tactview.core.repository.ProjectRepository;
import com.helospark.tactview.core.timeline.TimelineClipType;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.TimelineLength;
import com.helospark.tactview.core.timeline.audioeffect.balance.ChannelBalanceAudioEffect;
import com.helospark.tactview.core.timeline.audioeffect.normalize.NormalizeAudioEffect;
import com.helospark.tactview.core.timeline.audioeffect.volume.VolumeAudioEffect;
import com.helospark.tactview.core.timeline.effect.StandardEffectFactory;
import com.helospark.tactview.core.timeline.effect.TimelineEffectType;
import com.helospark.tactview.core.timeline.effect.blur.opencv.OpenCVBasedGaussianBlur;
import com.helospark.tactview.core.util.messaging.MessagingService;

@Configuration
public class AudioStandardEffectConfiguration {

    @Bean
    public StandardEffectFactory volumeEffect(OpenCVBasedGaussianBlur gaussianBlur, MessagingService messagingService) {
        return StandardEffectFactory.builder()
                .withFactory(request -> new VolumeAudioEffect(new TimelineInterval(request.getPosition(), TimelineLength.ofMillis(10000))))
                .withRestoreFactory((node, loadMetadata) -> new VolumeAudioEffect(node, loadMetadata))
                .withName("Volume")
                .withSupportedEffectId("volume")
                .withSupportedClipTypes(List.of(TimelineClipType.AUDIO))
                .withEffectType(TimelineEffectType.AUDIO_EFFECT)
                .build();
    }

    @Bean
    public StandardEffectFactory channelBalanceEffect(ProjectRepository projectRepository) {
        return StandardEffectFactory.builder()
                .withFactory(request -> new ChannelBalanceAudioEffect(new TimelineInterval(request.getPosition(), TimelineLength.ofMillis(10000)), projectRepository))
                .withRestoreFactory((node, loadMetadata) -> new ChannelBalanceAudioEffect(node, loadMetadata, projectRepository))
                .withName("Channel balance")
                .withSupportedEffectId("channelbalance")
                .withSupportedClipTypes(List.of(TimelineClipType.AUDIO))
                .withEffectType(TimelineEffectType.AUDIO_EFFECT)
                .build();
    }

    @Bean
    public StandardEffectFactory normalizeAudioEffect() {
        return StandardEffectFactory.builder()
                .withFactory(request -> new NormalizeAudioEffect(new TimelineInterval(request.getPosition(), TimelineLength.ofMillis(10000))))
                .withRestoreFactory((node, loadMetadata) -> new NormalizeAudioEffect(node, loadMetadata))
                .withName("Normalize")
                .withSupportedEffectId("normalize")
                .withSupportedClipTypes(List.of(TimelineClipType.AUDIO))
                .withEffectType(TimelineEffectType.AUDIO_EFFECT)
                .withIsFullWidth(true)
                .build();
    }
}
