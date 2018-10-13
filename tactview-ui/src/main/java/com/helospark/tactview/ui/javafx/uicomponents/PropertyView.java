package com.helospark.tactview.ui.javafx.uicomponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.EffectParametersRepository;
import com.helospark.tactview.core.timeline.effect.interpolation.KeyframeableEffect;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.DoubleProvider;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.IntegerProvider;
import com.helospark.tactview.core.timeline.message.ClipDescriptorsAdded;
import com.helospark.tactview.core.timeline.message.EffectDescriptorsAdded;
import com.helospark.tactview.core.timeline.message.KeyframeAddedRequest;
import com.helospark.tactview.core.util.messaging.MessagingService;
import com.helospark.tactview.ui.javafx.UiCommandInterpreterService;
import com.helospark.tactview.ui.javafx.UiTimelineManager;
import com.helospark.tactview.ui.javafx.commands.impl.AddKeyframeForProperty;
import com.helospark.tactview.ui.javafx.uicomponents.EffectPropertyPage.Builder;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

@Component
public class PropertyView {
    private FlowPane propertyWindow;
    private Map<String, EffectPropertyPage> effectProperties = new HashMap<>();
    private Map<String, EffectPropertyPage> clipProperties = new HashMap<>();

    private MessagingService messagingService;
    private UiCommandInterpreterService commandInterpreter;
    private UiTimelineManager uiTimelineManager;
    private EffectPropertyPage shownEntries;
    private EffectParametersRepository effectParametersRepository;

    public PropertyView(MessagingService messagingService, UiCommandInterpreterService commandInterpreter, UiTimelineManager uiTimelineManager,
            EffectParametersRepository effectParametersRepository) {
        this.messagingService = messagingService;
        this.commandInterpreter = commandInterpreter;
        this.uiTimelineManager = uiTimelineManager;
        this.effectParametersRepository = effectParametersRepository;
    }

    @PostConstruct
    public void init() {
        propertyWindow = new FlowPane();
        propertyWindow.setId("property-view");
        propertyWindow.setPrefWidth(200);

        messagingService.register(EffectDescriptorsAdded.class,
                message -> Platform.runLater(() -> {
                    EffectPropertyPage asd = createBox(message.getDescriptors());
                    effectProperties.put(message.getEffectId(), asd);
                }));
        messagingService.register(ClipDescriptorsAdded.class,
                message -> Platform.runLater(() -> {
                    EffectPropertyPage asd = createBox(message.getDescriptors());
                    clipProperties.put(message.getClipId(), asd);
                }));
    }

    private EffectPropertyPage createBox(List<ValueProviderDescriptor> descriptors) {
        Builder result = EffectPropertyPage.builder()
                .withBox(new GridPane());
        for (int i = 0; i < descriptors.size(); ++i) {
            addElement(descriptors.get(i), result, i);
        }
        return result.build();
    }

    private void addElement(ValueProviderDescriptor descriptor, Builder result, int line) {
        Label label = new Label(descriptor.getName());
        EffectLine keyframeChange = keyframeStuff(descriptor.getKeyframeableEffect());

        Node key = keyframeChange.visibleNode;
        key.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.I)) {
                KeyframeAddedRequest keyframeRequest = KeyframeAddedRequest.builder()
                        .withDescriptorId(descriptor.getKeyframeableEffect().getId())
                        .withGlobalTimelinePosition(uiTimelineManager.getCurrentPosition())
                        .withValue(keyframeChange.currentValueProvider.get())
                        .build();

                commandInterpreter.sendWithResult(new AddKeyframeForProperty(effectParametersRepository, keyframeRequest));

                event.consume();
            }
        });

        result.getBox().add(label, 0, line);
        result.getBox().add(key, 1, line);

        result.addUpdateFunctions(keyframeChange.updateFunction);
    }

    private EffectLine keyframeStuff(KeyframeableEffect keyframeableEffect) {
        // TODO: chain here
        EffectLine effectLine = new EffectLine();
        if (keyframeableEffect instanceof IntegerProvider) {
            IntegerProvider integerProvider = (IntegerProvider) keyframeableEffect;
            if (integerProvider.getMax() - integerProvider.getMin() < 1000) {
                Slider slider = new Slider();
                slider.setMin(integerProvider.getMin());
                slider.setMax(integerProvider.getMax());
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(true);
                slider.setValue(integerProvider.getValueAt(TimelinePosition.ofZero()));
                effectLine.visibleNode = slider;
                effectLine.updateFunction = position -> slider.setValue(integerProvider.getValueAt(position));
                effectLine.currentValueProvider = () -> String.valueOf(slider.getValue());
                return effectLine;
            } else {
                TextField textField = new TextField();
                effectLine.visibleNode = textField;
                effectLine.updateFunction = position -> textField.setText(integerProvider.getValueAt(position).toString());
                effectLine.currentValueProvider = () -> textField.getText();
                return effectLine;
            }
        } else if (keyframeableEffect instanceof DoubleProvider) {
            DoubleProvider doubleProvider = (DoubleProvider) keyframeableEffect;
            TextField textField = new TextField();
            effectLine.visibleNode = textField;
            effectLine.updateFunction = position -> textField.setText(doubleProvider.getValueAt(position).toString());
            effectLine.currentValueProvider = () -> textField.getText();
            return effectLine;
        }
        System.out.println("LAter...");
        throw new IllegalArgumentException("Later...");
    }

    public FlowPane getPropertyWindow() {
        return propertyWindow;
    }

    public void showEffectProperties(String effectId) {
        showProperties(effectProperties.get(effectId));
    }

    public void showClipProperties(String clipId) {
        showProperties(clipProperties.get(clipId));
    }

    private void showProperties(EffectPropertyPage shownEntries2) {
        shownEntries = shownEntries2;
        propertyWindow.getChildren().clear();
        if (shownEntries2 != null) {
            propertyWindow.getChildren().add(shownEntries.getBox());
            shownEntries2.getUpdateFunctions()
                    .stream()
                    .forEach(a -> a.accept(uiTimelineManager.getCurrentPosition()));
        } else {
            System.out.println("Effect not found, should not happen");
        }
    }

    public void clearProperties() {
        shownEntries = null;
    }

    public void updateValues(TimelinePosition position) {
        if (shownEntries != null) {
            shownEntries.getUpdateFunctions()
                    .stream()
                    .forEach(updateFunction -> updateFunction.accept(position));
        }
    }

    static class EffectLine {
        public Node visibleNode;
        public Consumer<TimelinePosition> updateFunction;
        public Supplier<String> currentValueProvider;
    }
}