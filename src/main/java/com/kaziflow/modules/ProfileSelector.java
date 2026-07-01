package com.kaziflow.modules;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * ProfileSelector — first-run industry picker wizard.
 *
 * Shown once when no industry_profile exists in the settings table.
 * The user picks their industry and the selection is saved via ModuleRegistry.
 *
 * Can also be accessed later via Settings → Change Industry Profile.
 */
public class ProfileSelector {

    private BorderPane root;
    private IndustryProfile selected = IndustryProfile.GENERAL_RETAIL;
    private Runnable onComplete;

    public ProfileSelector(Runnable onComplete) {
        this.onComplete = onComplete;
        buildUI();
    }

    public BorderPane getRoot() { return root; }

    private void buildUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f8fafc;");

        // ── Header ──────────────────────────────────────────────────────
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(40, 40, 24, 40));
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Welcome to KaziFlow ERP");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #0d1b2a;");

        Label subtitle = new Label("What kind of business are you setting up?");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #64748b;");

        Label hint = new Label("This helps us show the right features for your business. You can change this later in Settings.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        header.getChildren().addAll(title, subtitle, hint);

        // ── Profile Grid ─────────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        FlowPane grid = new FlowPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(24, 32, 24, 32));
        grid.setAlignment(Pos.CENTER);

        ToggleGroup group = new ToggleGroup();

        for (IndustryProfile profile : IndustryProfile.values()) {
            grid.getChildren().add(profileCard(profile, group));
        }

        scroll.setContent(grid);

        // ── Footer ──────────────────────────────────────────────────────
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 32, 24, 32));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Label selectedLabel = new Label("Selected: " + selected.displayName);
        selectedLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button continueBtn = new Button("Continue →");
        continueBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8; -fx-pref-height: 40px; -fx-pref-width: 160px; -fx-font-size: 14px; -fx-cursor: hand;");

        continueBtn.setOnAction(e -> {
            // Save the selected profile
            ModuleRegistry.getInstance().setProfile(selected);
            if (onComplete != null) onComplete.run();
        });

        // Update selected label when toggle changes
        group.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw != null && nw.getUserData() instanceof IndustryProfile p) {
                selected = p;
                selectedLabel.setText("Selected: " + p.displayName);
            }
        });

        footer.getChildren().addAll(selectedLabel, sp, continueBtn);

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);
    }

    private ToggleButton profileCard(IndustryProfile profile, ToggleGroup group) {
        boolean isDefault = profile == IndustryProfile.GENERAL_RETAIL;

        VBox content = new VBox(8);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(16));
        content.setPrefWidth(220);
        content.setMinHeight(120);

        Label icon = new Label(profile.icon);
        icon.setStyle("-fx-font-size: 26px;");

        Label name = new Label(profile.displayName);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-wrap-text: true;");
        name.setMaxWidth(190);

        Label desc = new Label(profile.description);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-wrap-text: true;");
        desc.setMaxWidth(190);

        content.getChildren().addAll(icon, name, desc);

        ToggleButton btn = new ToggleButton();
        btn.setGraphic(content);
        btn.setToggleGroup(group);
        btn.setUserData(profile);
        btn.setSelected(isDefault);

        String styleBase = "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 2; " +
            "-fx-cursor: hand; -fx-pref-width: 220px; -fx-alignment: top-left;";
        String styleNormal   = styleBase + "-fx-background-color: white; -fx-border-color: #e2e8f0;";
        String styleSelected = styleBase + "-fx-background-color: #eff6ff; -fx-border-color: #2563eb;";

        btn.setStyle(isDefault ? styleSelected : styleNormal);

        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            btn.setStyle(isSelected ? styleSelected : styleNormal);
            if (isSelected) selected = profile;
        });

        return btn;
    }
}
