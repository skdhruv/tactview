package com.helospark.tactview.ui.javafx.uicomponents.propertyvalue;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Generated;

import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.EffectParametersRepository;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.message.KeyframeAddedRequest;
import com.helospark.tactview.core.timeline.message.KeyframeRemovedRequest;
import com.helospark.tactview.ui.javafx.UiCommandInterpreterService;
import com.helospark.tactview.ui.javafx.commands.impl.AddKeyframeForPropertyCommand;
import com.helospark.tactview.ui.javafx.commands.impl.CompositeCommand;
import com.helospark.tactview.ui.javafx.commands.impl.RemoveKeyframeCommand;

import javafx.scene.Node;

public class PrimitiveEffectLine extends EffectLine {
    public Consumer<TimelinePosition> updateFunction;
    public Supplier<String> currentValueProvider;

    @Generated("SparkTools")
    private PrimitiveEffectLine(Builder builder) {
        this.commandInterpreter = builder.commandInterpreter;
        this.effectParametersRepository = builder.effectParametersRepository;
        this.visibleNode = builder.visibleNode;
        this.descriptorId = builder.descriptorId;
        this.updateFromValue = builder.updateFromValue;
        this.updateFunction = builder.updateFunction;
        this.currentValueProvider = builder.currentValueProvider;
        this.disabledUpdater = builder.disabledUpdater;
        this.descriptor = builder.descriptor;
    }

    @Override
    public void sendKeyframe(TimelinePosition position) {
        KeyframeAddedRequest keyframeRequest = KeyframeAddedRequest.builder()
                .withDescriptorId(descriptorId)
                .withGlobalTimelinePosition(position)
                .withValue(currentValueProvider.get()).build();

        commandInterpreter.sendWithResult(new AddKeyframeForPropertyCommand(effectParametersRepository, keyframeRequest));
    }

    public void sendKeyframeWithValue(TimelinePosition position, String value) {
        sendKeyframeWithValueAndRevertable(position, value, true);
    }

    public void sendKeyframeWithValueAndRevertable(TimelinePosition position, String value, boolean revertable) {
        KeyframeAddedRequest keyframeRequest = KeyframeAddedRequest.builder()
                .withDescriptorId(descriptorId)
                .withGlobalTimelinePosition(position)
                .withValue(value)
                .withRevertable(revertable)
                .build();

        commandInterpreter.sendWithResult(new AddKeyframeForPropertyCommand(effectParametersRepository, keyframeRequest));
    }

    @Override
    public void removeAllAndSetKeyframe(TimelinePosition currentPosition) {
        RemoveAllKeyframeCommand removeAllKeyFrameCommand = new RemoveAllKeyframeCommand(effectParametersRepository, descriptorId);

        KeyframeAddedRequest keyframeRequest = KeyframeAddedRequest.builder()
                .withDescriptorId(descriptorId)
                .withGlobalTimelinePosition(currentPosition)
                .withValue(currentValueProvider.get())
                .build();
        AddKeyframeForPropertyCommand addKeyframeCommand = new AddKeyframeForPropertyCommand(effectParametersRepository, keyframeRequest);

        commandInterpreter.sendWithResult(new CompositeCommand(removeAllKeyFrameCommand, addKeyframeCommand));
    }

    @Override
    public void removeKeyframe(TimelinePosition currentPosition) {
        KeyframeRemovedRequest removeKeyframeRequest = KeyframeRemovedRequest.builder()
                .withDescriptorId(descriptorId)
                .withGlobalTimelinePosition(currentPosition)
                .build();
        RemoveKeyframeCommand command = new RemoveKeyframeCommand(effectParametersRepository, removeKeyframeRequest);
        commandInterpreter.sendWithResult(command);
    }

    @Override
    public void updateUi(TimelinePosition position) {
        updateFunction.accept(position);
        disableUiIfNeeded(position);
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {
        private UiCommandInterpreterService commandInterpreter;
        private EffectParametersRepository effectParametersRepository;
        private Node visibleNode;
        private String descriptorId;
        private Consumer<Object> updateFromValue;
        private Consumer<Boolean> disabledUpdater;
        private Consumer<TimelinePosition> updateFunction;
        private Supplier<String> currentValueProvider;
        private ValueProviderDescriptor descriptor;

        private Builder() {
        }

        public Builder withCommandInterpreter(UiCommandInterpreterService commandInterpreter) {
            this.commandInterpreter = commandInterpreter;
            return this;
        }

        public Builder withEffectParametersRepository(EffectParametersRepository effectParametersRepository) {
            this.effectParametersRepository = effectParametersRepository;
            return this;
        }

        public Builder withVisibleNode(Node visibleNode) {
            this.visibleNode = visibleNode;
            return this;
        }

        public Builder withDescriptorId(String descriptorId) {
            this.descriptorId = descriptorId;
            return this;
        }

        public Builder withUpdateFromValue(Consumer<Object> updateFromValue) {
            this.updateFromValue = updateFromValue;
            return this;
        }

        public Builder withUpdateFunction(Consumer<TimelinePosition> updateFunction) {
            this.updateFunction = updateFunction;
            return this;
        }

        public Builder withCurrentValueProvider(Supplier<String> currentValueProvider) {
            this.currentValueProvider = currentValueProvider;
            return this;
        }

        public Builder withDisabledUpdater(Consumer<Boolean> disabledUpdater) {
            this.disabledUpdater = disabledUpdater;
            return this;
        }

        public Builder withDescriptor(ValueProviderDescriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public PrimitiveEffectLine build() {
            return new PrimitiveEffectLine(this);
        }
    }

}
