package com.helospark.tactview.ui.javafx.uicomponents.propertyvalue;

import java.util.function.Consumer;

import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.EffectParametersRepository;
import com.helospark.tactview.ui.javafx.UiCommandInterpreterService;

import javafx.scene.Node;

public abstract class EffectLine {
    protected UiCommandInterpreterService commandInterpreter;
    protected EffectParametersRepository effectParametersRepository;
    protected Node visibleNode;
    protected String descriptorId;
    protected Consumer<Object> updateFromValue;

    public void setCommandInterpreter(UiCommandInterpreterService commandInterpreter, EffectParametersRepository effectParametersRepository) {
        this.commandInterpreter = commandInterpreter;
        this.effectParametersRepository = effectParametersRepository;
    }

    public abstract void sendKeyframe(TimelinePosition position);

    public abstract void updateUi(TimelinePosition position);

    public Node getVisibleNode() {
        return visibleNode;
    }

    public Consumer<Object> getUpdateFromValue() {
        return updateFromValue;
    }

}