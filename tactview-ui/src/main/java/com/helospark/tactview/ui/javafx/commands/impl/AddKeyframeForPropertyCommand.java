package com.helospark.tactview.ui.javafx.commands.impl;

import java.util.Optional;

import com.helospark.tactview.core.timeline.effect.EffectParametersRepository;
import com.helospark.tactview.core.timeline.message.KeyframeAddedRequest;
import com.helospark.tactview.ui.javafx.commands.UiCommand;

public class AddKeyframeForPropertyCommand implements UiCommand {
    private EffectParametersRepository effectParametersRepository;
    private KeyframeAddedRequest request;
    private Optional<Object> previousValue;

    public AddKeyframeForPropertyCommand(EffectParametersRepository effectParametersRepository, KeyframeAddedRequest request) {
        this.effectParametersRepository = effectParametersRepository;
        this.request = request;
    }

    @Override
    public void execute() {
        previousValue = effectParametersRepository.getKeyframeableEffectValue(request.getDescriptorId(), request.getGlobalTimelinePosition());
        effectParametersRepository.keyframeAdded(request);
    }

    @Override
    public void revert() {
        if (previousValue.isPresent()) {
            KeyframeAddedRequest keyframeAddedRequest = KeyframeAddedRequest.builder()
                    .withDescriptorId(request.getDescriptorId())
                    .withGlobalTimelinePosition(request.getGlobalTimelinePosition())
                    .withValue(previousValue.map(String::valueOf).get())
                    .build();
            effectParametersRepository.keyframeAdded(keyframeAddedRequest);
        }
    }

    @Override
    public String toString() {
        return "AddKeyframeForPropertyCommand [request=" + request + "]";
    }

}
