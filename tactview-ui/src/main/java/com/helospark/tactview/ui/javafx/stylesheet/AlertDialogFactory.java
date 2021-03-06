package com.helospark.tactview.ui.javafx.stylesheet;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.helospark.lightdi.annotation.Component;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

@Component
public class AlertDialogFactory {
    private final StylesheetAdderService stylesheetAdderService;

    public AlertDialogFactory(StylesheetAdderService stylesheetAdderService) {
        this.stylesheetAdderService = stylesheetAdderService;
    }

    public Alert createSimpleAlertWithTitleAndContent(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.setHeaderText(null);
        stylesheetAdderService.addStyleSheets(alert.getDialogPane(), "stylesheet.css");
        return alert;
    }

    // for now same as above, but this could use different UI
    public Alert createErrorAlertWithStackTrace(String title, Throwable ex) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add("stylesheet.css");

        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(ex.getMessage());

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("Full stacktrace");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);
        return alert;
    }

    public void showExceptionDialog(String title, Throwable ex) {
        Platform.runLater(() -> {
            Alert alert = createErrorAlertWithStackTrace(title, ex);

            alert.showAndWait();
        });
    }

}
