package com.kaziflow.views;

import com.kaziflow.database.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class SettingsView {

    private VBox root;
    private Map<String, String> settings = new HashMap<>();

    public SettingsView() {
        loadSettings();
        buildUI();
    }

    public VBox getRoot() { return root; }

    private void loadSettings() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT key, value FROM settings")) {
            while (rs.next()) settings.put(rs.getString("key"), rs.getString("value"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void buildUI() {
        root = new VBox(0);
        root.setStyle("-fx-background-color: #f8fafc;");

        HBox topbar = buildTopBar();
        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        VBox businessCard = buildBusinessCard();
        HBox modulesRow = buildModulesRow();
        HBox systemStatus = buildSystemStatus();

        content.getChildren().addAll(businessCard, modulesRow, systemStatus);
        root.getChildren().addAll(topbar, content);
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 24, 16, 24));
        bar.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        VBox titleBox = new VBox(2);
        Label title = new Label("System Administration"); title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label bc = new Label("Dashboard › Settings › General"); bc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(title, bc);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button viewGuideBtn = new Button("☰ View Guide");
        viewGuideBtn.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-pref-height: 36px; -fx-padding: 0 14; -fx-cursor: hand; -fx-font-size: 13px;");
        viewGuideBtn.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI("https://docs.kaziflow.co.ke")); } catch (Exception ex) { ex.printStackTrace(); } });

        bar.getChildren().addAll(titleBox, sp, viewGuideBtn);
        return bar;
    }

    private VBox buildBusinessCard() {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        HBox inner = new HBox(16);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(24));

        // Logo placeholder
        StackPane logoBox = new StackPane();
        logoBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12; -fx-pref-width: 64; -fx-pref-height: 64;");
        Label logoInitials = new Label(settings.getOrDefault("business_name", "My Business").substring(0, Math.min(2, settings.getOrDefault("business_name","My").length())).toUpperCase());
        logoInitials.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        logoBox.getChildren().add(logoInitials);

        VBox businessInfo = new VBox(4);
        Label businessName = new Label(settings.getOrDefault("business_name", "My Business"));
        businessName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label businessAddr = new Label(settings.getOrDefault("business_address", "Nairobi, Kenya") + "  •  PIN: " + settings.getOrDefault("business_pin", "Not set"));
        businessAddr.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        businessInfo.getChildren().addAll(businessName, businessAddr);
        HBox.setHgrow(businessInfo, Priority.ALWAYS);

        Label activeBadge = new Label("● SUBSCRIPTION ACTIVE");
        activeBadge.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #dcfce7; -fx-background-radius: 20; -fx-padding: 4 12;");

        Button editBtn = new Button("✎ Edit Business Details");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: transparent;");
        editBtn.setOnAction(e -> showEditBusinessDialog());

        inner.getChildren().addAll(logoBox, businessInfo, activeBadge, editBtn);
        card.getChildren().add(inner);
        return card;
    }

    private HBox buildModulesRow() {
        HBox container = new HBox(16);

        // Show current profile name dynamically
        String profileName = com.kaziflow.modules.ModuleRegistry.getInstance()
            .getActiveProfile().displayName;

        boolean smsConfigured = com.kaziflow.services.AfricasTalkingService.getInstance().isConfigured();
        String smsStatus = smsConfigured
            ? (com.kaziflow.services.AfricasTalkingService.getInstance().isSandbox() ? "Sandbox Connected" : "Status: Connected")
            : "Status: Not Set";
        String smsColor = smsConfigured ? "#16a34a" : "#94a3b8";

        boolean encEnabled = com.kaziflow.security.DatabaseEncryption.getInstance(
            System.getProperty("user.home") + "/KaziFlowERP")
            .isEncryptionEnabled(System.getProperty("user.home") + "/KaziFlowERP/kaziflow.db");
        String encStatus = encEnabled ? "Enabled (AES-256)" : "Not Enabled";
        String encColor  = encEnabled ? "#16a34a" : "#94a3b8";

        com.kaziflow.license.LicenseService licenseSvc =
            com.kaziflow.license.LicenseService.getInstance(
                System.getProperty("user.home") + "/KaziFlowERP");
        com.kaziflow.license.LicenseService.Status licStatusEnum = licenseSvc.checkStatus();
        String licenseStatus, licenseColor;
        switch (licStatusEnum) {
            case ACTIVE -> { licenseStatus = licenseSvc.getEdition() + " — Active"; licenseColor = "#16a34a"; }
            case TRIAL_ACTIVE -> { licenseStatus = licenseSvc.trialDaysRemaining() + " trial days left"; licenseColor = "#d97706"; }
            case TRIAL_EXPIRED -> { licenseStatus = "Trial Expired"; licenseColor = "#dc2626"; }
            default -> { licenseStatus = "Not Activated"; licenseColor = "#94a3b8"; }
        }

        String[][] modules = {
            {"◎", "Users & Access",       "Manage staff accounts, password resets, and login activity logs.", "12 Active Users",  "", "module_users"},
            {"◈", "Roles & Perms",        "Configure granular access levels for Cashiers, Managers, Admins.", "5 Roles Defined",  "", "module_roles"},
            {"⊙", "M-Pesa",               "Configure Daraja API, Paybill/Till numbers, and C2B webhooks.",   "Status: Connected","#16a34a","module_mpesa"},
            {"✉", "SMS (Africa's Talking)","Send receipts and reminders via SMS for customers without WhatsApp.",smsStatus,smsColor,"module_sms"},
            {"⊞", "Tax Settings",         "VAT rates (16%), ETIMS configuration, and tax categories.",        "VAT: 16%",         "", "module_tax"},
            {"⚙", "System Modules",       "Enable/Disable Inventory, POS, HR, or Workshop modules.",         "Workshop ON",      "", "module_system"},
            {"🏪", "Industry Profile",    "Change your business type to load the right modules and layout.",  profileName,        "#7c3aed", "module_industry"},
            {"✦",  "AI Assistant",        "Configure Claude API key for AI-powered business insights.",        "Set API Key →",    "#02a870", "module_ai"},
            {"▣", "Data Management",      "Manual backups, restore points, and automated backup schedules.", "Last Backup: 2h",  "", "module_backup"},
            {"▤", "Audit Trail",          "View detailed system logs of all user actions and changes.",       "View Logs →",      "#2563eb","module_audit"},
            {"🌐","Language / Lugha",      "Switch between English and Kiswahili interface.",
                com.kaziflow.utils.I18n.getInstance().isSwahili() ? "Kiswahili" : "English",
                com.kaziflow.utils.I18n.getInstance().isSwahili() ? "#16a34a" : "#2563eb",
                "module_language"},
            {"🔐","DB Encryption",        "AES-256-GCM at-rest encryption. Protects data if device is lost.", encStatus, encColor, "module_security"},
            {"🔑","License",              "View your license status, edition, and activate a key.", licenseStatus, licenseColor, "module_license"},
        };

        HBox hRow1 = new HBox(16); HBox hRow2 = new HBox(16); HBox hRow3 = new HBox(16);

        for (int i=0; i<modules.length; i++) {
            String[] m = modules[i];
            VBox card = buildModuleCard(m[0], m[1], m[2], m[3], m[4], m[5]);
            HBox.setHgrow(card, Priority.ALWAYS);
            HBox target = i < 4 ? hRow1 : (i < 7 ? hRow2 : hRow3);
            target.getChildren().add(card);
        }

        VBox rows = new VBox(16); rows.getChildren().addAll(hRow1, hRow2, hRow3);
        container.getChildren().add(rows); HBox.setHgrow(rows, Priority.ALWAYS);
        return container;
    }

    private VBox buildModuleCard(String icon, String title, String desc, String status, String statusColor, String settingKey) {
        // Read current enabled state from settings
        boolean enabled = !"false".equals(settings.getOrDefault(settingKey + "_enabled", "true"));

        VBox card = new VBox(10);
        String baseStyle = "-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#e2e8f0;-fx-border-radius:12;-fx-border-width:1;-fx-padding:20;-fx-cursor:hand;";
        card.setStyle(baseStyle + (enabled ? "" : "-fx-opacity:0.6;"));

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color:#f1f5f9;-fx-background-radius:10;-fx-padding:12;");
        iconBox.setPrefSize(44,44); iconBox.setMaxSize(44,44);
        Label iconLabel = new Label(icon); iconLabel.setStyle("-fx-text-fill:#475569;-fx-font-size:18px;");
        iconBox.getChildren().add(iconLabel);

        Label titleLabel = new Label(title); titleLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Label descLabel  = new Label(desc);  descLabel.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;-fx-wrap-text:true;"); descLabel.setWrapText(true);

        String statusFg = statusColor.isEmpty() ? "#94a3b8" : statusColor;
        Label statusLabel = new Label(status); statusLabel.setStyle("-fx-font-size:12px;-fx-text-fill:"+statusFg+";-fx-font-weight:bold;");

        // Toggle button for system modules that can be enabled/disabled
        if (settingKey.startsWith("module_") && !settingKey.equals("module_users") && !settingKey.equals("module_roles") && !settingKey.equals("module_audit")) {
            ToggleButton toggle = new ToggleButton(enabled ? "Enabled" : "Disabled");
            toggle.setSelected(enabled);
            toggle.setStyle(enabled
                ? "-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-background-radius:20;-fx-border-color:transparent;-fx-font-size:11px;-fx-font-weight:bold;"
                : "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:20;-fx-border-color:transparent;-fx-font-size:11px;-fx-font-weight:bold;");
            toggle.setOnAction(e -> {
                boolean on = toggle.isSelected();
                toggle.setText(on ? "Enabled" : "Disabled");
                toggle.setStyle(on
                    ? "-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-background-radius:20;-fx-border-color:transparent;-fx-font-size:11px;-fx-font-weight:bold;"
                    : "-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-background-radius:20;-fx-border-color:transparent;-fx-font-size:11px;-fx-font-weight:bold;");
                saveSetting(settingKey + "_enabled", String.valueOf(on));
                settings.put(settingKey + "_enabled", String.valueOf(on));
                card.setStyle(baseStyle + (on ? "" : "-fx-opacity:0.6;"));
            });
            card.getChildren().addAll(iconBox, titleLabel, descLabel, toggle);
        } else {
            card.getChildren().addAll(iconBox, titleLabel, descLabel, statusLabel);
        }

        card.setOnMouseEntered(e -> card.setStyle(baseStyle + "-fx-border-color:#2563eb;" + (enabled?"":"-fx-opacity:0.6;")));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle + (enabled?"":"-fx-opacity:0.6;")));

        // Special click handlers
        if (title.equals("Users & Access")) {
        if (title.equals("Roles & Perms")) {
            card.setOnMouseClicked(e -> showRolesDialog());
        }
        if (title.equals("Tax Settings")) {
            card.setOnMouseClicked(e -> showTaxSettingsDialog());
        }
        if (title.equals("Audit Trail")) {
            card.setOnMouseClicked(e -> showAuditTrailDialog());
        }
            card.setOnMouseClicked(e -> {
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("User Management");
                UserManagementView umv = new UserManagementView();
                javafx.scene.Scene scene = new javafx.scene.Scene(umv.getRoot(), 1200, 720);
                try { scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm()); } catch(Exception ignored) {}
                stage.setScene(scene); stage.show();
            });
        }
        if (title.equals("Data Management")) {
            card.setOnMouseClicked(e -> {
                Alert menu = new Alert(Alert.AlertType.NONE, "Choose an action:",
                    new ButtonType("📦 Backup Now"), new ButtonType("♻ Restore"), ButtonType.CANCEL);
                menu.setTitle("Data Management"); menu.setHeaderText(null);
                menu.showAndWait().ifPresent(btn -> {
                    com.kaziflow.services.BackupService bs = com.kaziflow.services.BackupService.getInstance();
                    if (btn.getText().contains("Backup")) bs.backupToDirectory();
                    else if (btn.getText().contains("Restore")) bs.restoreFromFile();
                });
            });
        }
        if (title.equals("M-Pesa")) {
            card.setOnMouseClicked(e -> showMpesaConfigDialog());
        }
        if (title.equals("SMS (Africa's Talking)")) {
            card.setOnMouseClicked(e -> showSmsConfigDialog());
        }
        if (title.equals("System Modules")) {
            card.setOnMouseClicked(e -> {
                com.kaziflow.views.BranchView bv = new com.kaziflow.views.BranchView();
                bv.show();
            });
        }
        if (title.equals("AI Assistant")) {
            card.setOnMouseClicked(e -> {
                // Access AIService directly — no need to build the full UI panel
                com.kaziflow.services.AIService ai = com.kaziflow.services.AIService.getInstance();
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("AI Assistant — API Key Setup");
                dialog.setHeaderText(null);
                dialog.getDialogPane().setStyle("-fx-background-color:white;");
                dialog.getDialogPane().setPrefWidth(440);
                javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12);
                content.setPadding(new Insets(20));
                Label info = new Label("KaziFlow AI uses Claude by Anthropic.\n" +
                    "Get a free API key at: console.anthropic.com\n\n" +
                    "Your key is stored locally on this device only.");
                info.setWrapText(true); info.setStyle("-fx-text-fill:#475569;-fx-font-size:12px;");
                PasswordField keyField = new PasswordField();
                keyField.setText(ai.loadApiKey());
                keyField.setPromptText("sk-ant-api03-...");
                keyField.setStyle("-fx-pref-height:36px;-fx-background-color:white;" +
                    "-fx-border-color:#e2e8f0;-fx-border-radius:6;-fx-background-radius:6;" +
                    "-fx-font-size:13px;-fx-padding:0 10;");
                content.getChildren().addAll(info, keyField);
                dialog.getDialogPane().setContent(content);
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                dialog.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        ai.saveApiKey(keyField.getText().trim());
                        com.kaziflow.utils.Toast.success(
                            com.kaziflow.utils.SceneManager.getInstance().getStage(),
                            "API key saved", "AI Assistant is ready to use");
                    }
                });
            });
        }
        if (title.equals("Industry Profile")) {
            card.setOnMouseClicked(e -> {
                // Show profile selector in a popup dialog
                javafx.stage.Stage dialog = new javafx.stage.Stage();
                dialog.setTitle("Change Industry Profile");
                dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                com.kaziflow.modules.ProfileSelector selector =
                    new com.kaziflow.modules.ProfileSelector(() -> {
                        dialog.close();
                        // Rebuild nav and refresh
                        com.kaziflow.utils.SceneManager.getInstance().showMainApp();
                    });
                javafx.scene.Scene s = new javafx.scene.Scene(selector.getRoot(), 1100, 680);
                dialog.setScene(s);
                dialog.showAndWait();
            });
        }
        if (title.equals("Language / Lugha")) {
            card.setOnMouseClicked(e -> showLanguageDialog());
        }
        if (title.equals("DB Encryption")) {
            card.setOnMouseClicked(e -> showEncryptionDialog());
        }
        if (title.equals("License")) {
            card.setOnMouseClicked(e -> showLicenseDialog());
        }

        return card;
    }

    private void showRolesDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Roles & Permissions");
        d.getDialogPane().setStyle("-fx-background-color:white;");
        d.getDialogPane().setPrefWidth(520); d.getDialogPane().setPrefHeight(400);
        javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(0);
        javafx.scene.control.TableView<String[]> tv = new javafx.scene.control.TableView<>();
        tv.setStyle("-fx-background-color:white;");
        tv.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        String[] cols = {"Role","Permissions","Created At"};
        for (int i = 0; i < cols.length; i++) {
            final int ci = i;
            javafx.scene.control.TableColumn<String[],String> col = new javafx.scene.control.TableColumn<>(cols[i]);
            col.setCellValueFactory(dd -> new javafx.beans.property.SimpleStringProperty(
                ci < dd.getValue().length ? dd.getValue()[ci] : ""));
            tv.getColumns().add(col);
        }
        javafx.collections.ObservableList<String[]> data = javafx.collections.FXCollections.observableArrayList();
        com.kaziflow.utils.AsyncTask.run(() -> {
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            try (java.sql.Connection c = com.kaziflow.database.DatabaseManager.getInstance().getConnection();
                 java.sql.Statement s = c.createStatement()) {
                java.sql.ResultSet rs = s.executeQuery("SELECT name, permissions, created_at FROM roles ORDER BY id");
                while (rs.next()) rows.add(new String[]{rs.getString("name"),
                    rs.getString("permissions"), rs.getString("created_at")});
            } catch (Exception e) { e.printStackTrace(); }
            return rows;
        }, data::setAll, err -> {});
        tv.setItems(data);
        VBox.setVgrow(tv, Priority.ALWAYS);
        layout.getChildren().add(tv);
        d.getDialogPane().setContent(layout);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private void showTaxSettingsDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Tax Settings");
        d.getDialogPane().setStyle("-fx-background-color:white;");
        d.getDialogPane().setPrefWidth(420);
        GridPane f = new GridPane(); f.setHgap(12); f.setVgap(12); f.setPadding(new Insets(20));

        String vatRate = "", pin = "";
        try (java.sql.Connection c = com.kaziflow.database.DatabaseManager.getInstance().getConnection();
             java.sql.Statement s = c.createStatement()) {
            java.sql.ResultSet rs = s.executeQuery("SELECT key, value FROM settings WHERE key IN ('vat_rate','kra_pin')");
            while (rs.next()) {
                if ("vat_rate".equals(rs.getString("key"))) vatRate = rs.getString("value");
                if ("kra_pin".equals(rs.getString("key")))  pin     = rs.getString("value");
            }
        } catch (Exception ignored) {}

        javafx.scene.control.TextField vatField = field("VAT Rate (%)", vatRate.isEmpty() ? "16" : vatRate);
        javafx.scene.control.TextField pinField = field("KRA PIN", pin);
        Label vatNote = new Label("Standard Kenya VAT rate is 16%. Changing this affects all new receipts.");
        vatNote.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;"); vatNote.setWrapText(true);

        f.addRow(0, lbl("VAT Rate (%)"), vatField);
        f.addRow(1, vatNote);
        f.addRow(2, lbl("KRA PIN"), pinField);

        d.getDialogPane().setContent(f);
        ButtonType saveBtn = new ButtonType("Save Tax Settings");
        d.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        d.showAndWait().ifPresent(btn -> {
            if (btn != saveBtn) return;
            try (java.sql.Connection c = com.kaziflow.database.DatabaseManager.getInstance().getConnection();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                     "INSERT OR REPLACE INTO settings (key, value) VALUES (?,?)")) {
                ps.setString(1, "vat_rate"); ps.setString(2, vatField.getText().trim());
                ps.executeUpdate();
                ps.setString(1, "kra_pin"); ps.setString(2, pinField.getText().trim());
                ps.executeUpdate();
                com.kaziflow.utils.Toast.success(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                    "Saved", "Tax settings updated");
            } catch (Exception ex) {
                com.kaziflow.utils.Toast.error(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                    "Error", ex.getMessage());
            }
        });
    }

    private void showAuditTrailDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Audit Trail — Recent Activity");
        d.getDialogPane().setStyle("-fx-background-color:white;");
        d.getDialogPane().setPrefWidth(700); d.getDialogPane().setPrefHeight(500);
        javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(0);

        HBox bar = new HBox(10); bar.setPadding(new Insets(10,16,10,16)); bar.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.control.TextField searchF = new javafx.scene.control.TextField();
        searchF.setPromptText("Filter by user, action, or module...");
        searchF.setPrefWidth(300);
        searchF.setStyle("-fx-pref-height:34px;-fx-border-color:#e2e8f0;-fx-border-radius:6;-fx-background-radius:6;");
        Label totalLbl = new Label();
        totalLbl.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;");
        bar.getChildren().addAll(searchF, totalLbl);

        javafx.scene.control.TableView<String[]> tv = new javafx.scene.control.TableView<>();
        tv.setStyle("-fx-background-color:white;");
        tv.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        String[] hdrs = {"User","Action","Description","Module","Timestamp"};
        for (int i = 0; i < hdrs.length; i++) {
            final int ci = i;
            javafx.scene.control.TableColumn<String[],String> col = new javafx.scene.control.TableColumn<>(hdrs[i]);
            col.setCellValueFactory(dd -> new javafx.beans.property.SimpleStringProperty(
                ci < dd.getValue().length ? dd.getValue()[ci] : ""));
            tv.getColumns().add(col);
        }
        javafx.collections.ObservableList<String[]> allData = javafx.collections.FXCollections.observableArrayList();
        javafx.collections.ObservableList<String[]> filteredData = javafx.collections.FXCollections.observableArrayList();
        tv.setItems(filteredData);

        searchF.textProperty().addListener((obs, old, val) -> {
            filteredData.clear();
            String q = val.toLowerCase();
            for (String[] row : allData)
                if (q.isEmpty() || java.util.Arrays.stream(row).anyMatch(c -> c != null && c.toLowerCase().contains(q)))
                    filteredData.add(row);
            totalLbl.setText(filteredData.size() + " of " + allData.size() + " entries");
        });

        com.kaziflow.utils.AsyncTask.run(() -> com.kaziflow.services.AuditLog.getRecentLogs(500),
            rows -> {
                allData.setAll(rows); filteredData.setAll(rows);
                totalLbl.setText(rows.size() + " entries");
            }, err -> {});

        VBox.setVgrow(tv, Priority.ALWAYS);
        layout.getChildren().addAll(bar, tv);
        d.getDialogPane().setContent(layout);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }


    private void showLanguageDialog() {
        com.kaziflow.utils.I18n i18n = com.kaziflow.utils.I18n.getInstance();
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Language / Lugha");
        d.getDialogPane().setStyle("-fx-background-color:white;");
        d.getDialogPane().setPrefWidth(380);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(16);
        content.setPadding(new Insets(20));

        Label info = new Label("Select your preferred interface language:\n" +
                               "Chagua lugha unayopenda:");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill:#475569;-fx-font-size:13px;");

        javafx.scene.control.ToggleGroup tg = new javafx.scene.control.ToggleGroup();
        javafx.scene.layout.VBox options = new javafx.scene.layout.VBox(10);

        for (Map.Entry<String, String> entry : i18n.getAvailableLocales().entrySet()) {
            javafx.scene.control.RadioButton rb = new javafx.scene.control.RadioButton(entry.getValue());
            rb.setUserData(entry.getKey());
            rb.setToggleGroup(tg);
            rb.setSelected(entry.getKey().equals(i18n.getLocale()));
            rb.setStyle("-fx-font-size:14px;-fx-text-fill:#1e293b;");
            options.getChildren().add(rb);
        }
        content.getChildren().addAll(info, options);
        d.getDialogPane().setContent(content);

        ButtonType applyBtn = new ButtonType("Apply / Tekeleza");
        d.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);
        d.showAndWait().ifPresent(btn -> {
            if (btn != applyBtn) return;
            javafx.scene.control.RadioButton selected =
                (javafx.scene.control.RadioButton) tg.getSelectedToggle();
            if (selected == null) return;
            String code = (String) selected.getUserData();
            i18n.setLocale(code);
            com.kaziflow.utils.Toast.success(
                com.kaziflow.utils.SceneManager.getInstance().getStage(),
                "Language changed",
                selected.getText() + " — navigate to refresh views");
        });
    }

    private void showLicenseDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("License — KaziFlow ERP");
        d.getDialogPane().setStyle("-fx-background-color:white;");
        d.getDialogPane().setPrefWidth(560);
        d.getDialogPane().setPrefHeight(620);

        com.kaziflow.views.LicenseActivationView activationView =
            new com.kaziflow.views.LicenseActivationView(
                System.getProperty("user.home") + "/KaziFlowERP");
        d.getDialogPane().setContent(activationView.getRoot());
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private void showEncryptionDialog() {
        String dbDir  = System.getProperty("user.home") + "/KaziFlowERP";
        String dbPath = dbDir + "/kaziflow.db";
        com.kaziflow.security.DatabaseEncryption enc =
            com.kaziflow.security.DatabaseEncryption.getInstance(dbDir);
        boolean enabled = enc.isEncryptionEnabled(dbPath);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Database Encryption — AES-256-GCM");
        d.getDialogPane().setStyle("-fx-background-color:white;");
        d.getDialogPane().setPrefWidth(460);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(14);
        content.setPadding(new Insets(20));

        Label statusLbl = new Label(enabled
            ? "Status: Enabled (AES-256-GCM) ✓"
            : "Status: Not enabled — database is stored unencrypted.");
        statusLbl.setStyle("-fx-font-weight:bold;-fx-text-fill:" + (enabled ? "#16a34a" : "#dc2626") + ";-fx-font-size:13px;");

        Label info = new Label(enabled
            ? "To change your passphrase, enter your current passphrase and a new one below."
            : "Set a passphrase to encrypt this database with AES-256-GCM.\n" +
              "You will be prompted for this passphrase every time KaziFlow starts.\n\n" +
              "⚠ If you forget your passphrase, data cannot be recovered. Store it securely.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill:#475569;-fx-font-size:12px;");

        javafx.scene.control.PasswordField passF  = new javafx.scene.control.PasswordField();
        passF.setPromptText(enabled ? "Current passphrase" : "New passphrase");
        passF.setStyle("-fx-pref-height:34px;-fx-border-color:#e2e8f0;-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:13px;");

        javafx.scene.control.PasswordField confirmF = new javafx.scene.control.PasswordField();
        confirmF.setPromptText(enabled ? "New passphrase" : "Confirm passphrase");
        confirmF.setStyle(passF.getStyle());

        if (enabled) content.getChildren().addAll(statusLbl, info, new Label("Current:"), passF, new Label("New passphrase:"), confirmF);
        else         content.getChildren().addAll(statusLbl, info, new Label("Passphrase:"), passF, new Label("Confirm:"), confirmF);

        ButtonType enableBtn = new ButtonType(enabled ? "Change Passphrase" : "Enable Encryption");
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(enableBtn, ButtonType.CANCEL);
        d.showAndWait().ifPresent(btn -> {
            if (btn != enableBtn) return;
            String pass = passF.getText();
            String confirm = confirmF.getText();
            if (pass.length() < 8) {
                com.kaziflow.utils.Toast.error(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                    "Too short", "Passphrase must be at least 8 characters.");
                return;
            }
            if (!pass.equals(confirm) && !enabled) {
                com.kaziflow.utils.Toast.error(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                    "Mismatch", "Passphrases do not match.");
                return;
            }
            try {
                if (enabled) {
                    enc.changePassphrase(passF.getText().toCharArray(), confirmF.getText().toCharArray(), dbPath);
                    com.kaziflow.utils.Toast.success(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                        "Passphrase changed", "Database re-encrypted with new passphrase.");
                } else {
                    enc.enableEncryption(pass.toCharArray(), dbPath);
                    com.kaziflow.utils.Toast.success(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                        "Encryption enabled",
                        "Database is now encrypted. You will be prompted at next startup.");
                }
            } catch (Exception ex) {
                com.kaziflow.utils.Toast.error(com.kaziflow.utils.SceneManager.getInstance().getStage(),
                    "Error", ex.getMessage());
            }
        });
    }

    private void showMpesaConfigDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("M-Pesa Integration Configuration"); dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color:white;"); dialog.getDialogPane().setPrefWidth(460);

        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(12); form.setPadding(new Insets(20));
        TextField keyField     = field("Consumer Key",    settings.getOrDefault("mpesa_consumer_key",""));
        TextField secretField  = field("Consumer Secret", settings.getOrDefault("mpesa_consumer_secret",""));
        TextField tillField    = field("Till Number",     settings.getOrDefault("mpesa_till_number",""));
        TextField paybillField = field("Paybill Number",  settings.getOrDefault("mpesa_paybill",""));
        TextField passkeyField = field("Passkey (leave blank for sandbox)", settings.getOrDefault("mpesa_passkey",""));
        ToggleButton sandboxToggle = new ToggleButton(!"false".equals(settings.getOrDefault("mpesa_sandbox","true"))?"Sandbox Mode":"Production Mode");
        sandboxToggle.setSelected(!"false".equals(settings.getOrDefault("mpesa_sandbox","true")));
        sandboxToggle.setStyle("-fx-background-color:#fef3c7;-fx-text-fill:#d97706;-fx-background-radius:6;-fx-border-color:transparent;");
        sandboxToggle.setOnAction(e -> {
            boolean sb = sandboxToggle.isSelected();
            sandboxToggle.setText(sb?"Sandbox Mode":"Production Mode");
            sandboxToggle.setStyle(sb?"-fx-background-color:#fef3c7;-fx-text-fill:#d97706;-fx-background-radius:6;-fx-border-color:transparent;":"-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-background-radius:6;-fx-border-color:transparent;");
        });

        form.addRow(0, lbl("Consumer Key"),    keyField,     lbl("Consumer Secret"), secretField);
        form.addRow(1, lbl("Till Number"),     tillField,    lbl("Paybill Number"),  paybillField);
        form.addRow(2, lbl("Passkey"),         passkeyField, lbl("Environment"),     sandboxToggle);

        VBox info = new VBox(4);
        Label hint = new Label("ℹ Get your credentials from Safaricom Developer Portal (developer.safaricom.co.ke)");
        hint.setStyle("-fx-text-fill:#2563eb;-fx-font-size:11px;-fx-wrap-text:true;"); hint.setWrapText(true);
        info.getChildren().add(hint);
        form.add(info, 0, 3, 4, 1);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                saveSetting("mpesa_consumer_key",    keyField.getText().trim());
                saveSetting("mpesa_consumer_secret", secretField.getText().trim());
                saveSetting("mpesa_till_number",     tillField.getText().trim());
                saveSetting("mpesa_paybill",         paybillField.getText().trim());
                saveSetting("mpesa_passkey",         passkeyField.getText().trim());
                saveSetting("mpesa_sandbox",         String.valueOf(sandboxToggle.isSelected()));
                // Reload credentials in service
                com.kaziflow.services.MpesaService.getInstance().loadCredentials();
            }
        });
    }

    private void showSmsConfigDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("SMS Integration — Africa's Talking"); dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color:white;"); dialog.getDialogPane().setPrefWidth(460);

        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(12); form.setPadding(new Insets(20));
        TextField usernameField = field("Username (e.g. your app username)", settings.getOrDefault("at_username",""));
        TextField apiKeyField   = field("API Key", settings.getOrDefault("at_api_key",""));
        TextField senderField   = field("Sender ID / Shortcode (optional)", settings.getOrDefault("at_sender_id",""));
        ToggleButton sandboxToggle = new ToggleButton(!"false".equals(settings.getOrDefault("at_sandbox","true"))?"Sandbox Mode":"Production Mode");
        sandboxToggle.setSelected(!"false".equals(settings.getOrDefault("at_sandbox","true")));
        sandboxToggle.setStyle("-fx-background-color:#fef3c7;-fx-text-fill:#d97706;-fx-background-radius:6;-fx-border-color:transparent;");
        sandboxToggle.setOnAction(e -> {
            boolean sb = sandboxToggle.isSelected();
            sandboxToggle.setText(sb?"Sandbox Mode":"Production Mode");
            sandboxToggle.setStyle(sb?"-fx-background-color:#fef3c7;-fx-text-fill:#d97706;-fx-background-radius:6;-fx-border-color:transparent;":"-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-background-radius:6;-fx-border-color:transparent;");
            usernameField.setDisable(sb);
            if (sb) usernameField.setText("sandbox");
        });
        if (sandboxToggle.isSelected()) { usernameField.setText("sandbox"); usernameField.setDisable(true); }

        form.addRow(0, lbl("Username"),  usernameField, lbl("Environment"), sandboxToggle);
        form.addRow(1, lbl("API Key"),   apiKeyField,   lbl("Sender ID"),   senderField);

        VBox info = new VBox(4);
        Label hint = new Label("ℹ Get your credentials from africastalking.com — sandbox username is always \"sandbox\" " +
            "and works without registering a Sender ID. SMS is used as a fallback for receipts and reminders when " +
            "a customer's number isn't on WhatsApp.");
        hint.setStyle("-fx-text-fill:#2563eb;-fx-font-size:11px;"); hint.setWrapText(true);
        info.getChildren().add(hint);
        form.add(info, 0, 2, 4, 1);

        Button testBtn = new Button("Send Test SMS");
        testBtn.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#475569;-fx-background-radius:6;-fx-pref-height:32px;-fx-padding:0 12;-fx-cursor:hand;-fx-font-size:12px;");
        TextField testPhoneField = field("Test phone (07xx...)", "");
        testPhoneField.setPrefWidth(160);
        Label testResult = new Label(""); testResult.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        testBtn.setOnAction(e -> {
            // Save current values first so the service picks them up
            saveSetting("at_username", usernameField.getText().trim());
            saveSetting("at_api_key",  apiKeyField.getText().trim());
            saveSetting("at_sender_id", senderField.getText().trim());
            saveSetting("at_sandbox",  String.valueOf(sandboxToggle.isSelected()));
            com.kaziflow.services.AfricasTalkingService.getInstance().loadCredentials();
            testResult.setText("Sending...");
            com.kaziflow.utils.AsyncTask.run(
                () -> com.kaziflow.services.AfricasTalkingService.getInstance()
                        .send(testPhoneField.getText().trim(), "KaziFlow ERP: this is a test SMS. If you received this, SMS is configured correctly!"),
                result -> testResult.setText(result),
                err -> testResult.setText("⚠️ " + err)
            );
        });
        HBox testRow = new HBox(8, testPhoneField, testBtn, testResult);
        testRow.setAlignment(Pos.CENTER_LEFT);
        form.add(testRow, 0, 3, 4, 1);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                saveSetting("at_username", usernameField.getText().trim());
                saveSetting("at_api_key",  apiKeyField.getText().trim());
                saveSetting("at_sender_id", senderField.getText().trim());
                saveSetting("at_sandbox",  String.valueOf(sandboxToggle.isSelected()));
                com.kaziflow.services.AfricasTalkingService.getInstance().loadCredentials();
                com.kaziflow.utils.Toast.success(
                    com.kaziflow.utils.SceneManager.getInstance().getStage(),
                    "SMS settings saved", "Africa's Talking is ready to use");
            }
        });
    }

    private HBox buildSystemStatus() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 16 24;");

        Label version = new Label("● System Version: v2.4.0 (Stable)");
        version.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label serverStatus = new Label("● Server Status: Operational");
        serverStatus.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label dbUsage = new Label("Database Usage:  4.5GB / 10GB");
        dbUsage.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        ProgressBar dbProgress = new ProgressBar(0.45);
        dbProgress.setPrefWidth(100);
        dbProgress.setPrefHeight(8);

        bar.getChildren().addAll(version, serverStatus, sp, dbUsage, dbProgress);
        return bar;
    }

    private void showEditBusinessDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Business Details");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.getDialogPane().setPrefWidth(440);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12); form.setPadding(new Insets(20));

        TextField nameField = field("Business Name", settings.getOrDefault("business_name", ""));
        TextField addrField = field("Address", settings.getOrDefault("business_address", ""));
        TextField pinField  = field("KRA PIN", settings.getOrDefault("business_pin", ""));
        TextField vatField  = field("VAT Rate (%)", settings.getOrDefault("vat_rate", "16"));

        form.addRow(0, lbl("Business Name"), nameField);
        form.addRow(1, lbl("Address"), addrField);
        form.addRow(2, lbl("KRA PIN"), pinField);
        form.addRow(3, lbl("VAT Rate"), vatField);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                saveSetting("business_name", nameField.getText());
                saveSetting("business_address", addrField.getText());
                saveSetting("business_pin", pinField.getText());
                saveSetting("vat_rate", vatField.getText());
                buildUI(); // Rebuild to show changes
            }
        });
    }

    private void saveSetting(String key, String value) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private TextField field(String prompt, String val) {
        TextField tf = new TextField(val); tf.setPromptText(prompt); tf.setPrefWidth(280);
        tf.setStyle("-fx-pref-height: 34px; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-padding: 0 8;");
        return tf;
    }

    private Label lbl(String text) {
        Label l = new Label(text); l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;"); return l;
    }
}
