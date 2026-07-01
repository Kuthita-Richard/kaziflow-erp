package com.kaziflow.views;

import com.kaziflow.dao.*;
import com.kaziflow.models.Product;
import com.kaziflow.utils.AsyncTask;
import com.kaziflow.utils.KESFormatter;
import com.kaziflow.utils.SceneManager;
import com.kaziflow.utils.Toast;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import com.kaziflow.dao.DeniDAO;
import java.util.List;

public class DashboardView {

    private VBox root;
    private final SaleDAO saleDAO = new SaleDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final TransactionDAO transDAO = new TransactionDAO();

    public DashboardView() {
        buildUI();
    }

    public VBox getRoot() { return root; }

    private void buildUI() {
        root = new VBox(0);
        root.setStyle("-fx-background-color: #f8fafc;");

        // ── Top bar ──
        HBox topbar = buildTopBar();

        // ── Scrollable content ──
        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        // ── Header ──
        VBox header = new VBox(4);
        Label title = new Label("Overview");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label subtitle = new Label("Here's what's happening in your business today.");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        header.getChildren().addAll(title, subtitle);

        // ── Stat Cards Row ──
        HBox statsRow = buildStatsRow();

        // ── Middle row: Chart + Payments ──
        HBox middleRow = new HBox(20);
        VBox chartCard = buildSalesChart();
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        VBox paymentsCard = buildOutstandingPayments();
        paymentsCard.setPrefWidth(300);

        middleRow.getChildren().addAll(chartCard, paymentsCard);

        // ── Bottom row: Low Stock + Top Selling ──
        HBox bottomRow = new HBox(20);
        VBox lowStockCard  = buildLowStockCard();
        HBox.setHgrow(lowStockCard, Priority.ALWAYS);
        VBox topSellingCard = buildTopSellingCard();
        HBox.setHgrow(topSellingCard, Priority.ALWAYS);
        VBox analyticsCard  = buildAnalyticsCard();
        HBox.setHgrow(analyticsCard, Priority.ALWAYS);
        bottomRow.getChildren().addAll(lowStockCard, topSellingCard, analyticsCard);

        content.getChildren().addAll(header, statsRow, middleRow, bottomRow);
        root.getChildren().addAll(topbar, content);
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setPrefHeight(60);
        bar.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        TextField search = new TextField();
        search.setPromptText("Search products, orders...");
        search.getStyleClass().add("search-box");
        search.setPrefWidth(300);
        HBox.setHgrow(search, Priority.NEVER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Toggle buttons
        ToggleGroup tg = new ToggleGroup();
        ToggleButton daily = new ToggleButton("Daily"); daily.setToggleGroup(tg); daily.setSelected(true);
        ToggleButton weekly = new ToggleButton("Weekly"); weekly.setToggleGroup(tg);
        ToggleButton monthly = new ToggleButton("Monthly"); monthly.setToggleGroup(tg);
        for (ToggleButton tb : new ToggleButton[]{daily, weekly, monthly}) {
            tb.setStyle("-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-pref-height: 32px; -fx-padding: 0 12;");
        }

        Button addSaleBtn = new Button("+ Add Sale");
        addSaleBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-pref-height: 36px; -fx-padding: 0 16; -fx-cursor: hand;");
        addSaleBtn.setOnAction(e -> SceneManager.getInstance().navigateTo("sales"));

        Button addPurchBtn = new Button("Add Purchase");
        addPurchBtn.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px; -fx-pref-height: 36px; -fx-padding: 0 14; -fx-cursor: hand;");
        addPurchBtn.setOnAction(e -> SceneManager.getInstance().navigateTo("purchases"));

        bar.getChildren().addAll(search, spacer, daily, weekly, monthly, addPurchBtn, addSaleBtn);
        return bar;
    }

    private HBox buildStatsRow() {
        HBox row = new HBox(16);

        double todayRevenue = saleDAO.getTodayRevenue();
        int todayCount      = saleDAO.getTodayCount();
        int lowStock        = productDAO.getLowStockCount();

        // Expiry alert count — best-effort, silently 0 if batches table not yet created
        int expiringSoon = 0;
        try {
            com.kaziflow.dao.BatchDAO batchDAO = new com.kaziflow.dao.BatchDAO();
            batchDAO.updateExpiredBatches();
            expiringSoon = batchDAO.getExpiringSoonCount(batchDAO.getAlertDays());
        } catch (Exception ignored) {}

        row.getChildren().addAll(
            statCard("Today's Revenue",   KESFormatter.format(todayRevenue), null, "Total sales today", "#2563eb"),
            statCard("Today's Sales",     String.valueOf(todayCount),        null, "Transactions", "#16a34a"),
            statCard("Low Stock",         lowStock + " Items",               null, "Below minimum level", "#dc2626"),
            statCard("Expiring Soon",     expiringSoon + " Batches",         null, "Within alert window", "#d97706")
        );

        for (var child : row.getChildren()) {
            HBox.setHgrow((Region) child, Priority.ALWAYS);
        }
        return row;
    }

    private VBox statCard(String label, String value, String change, String note, String accentColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");
        val.setWrapText(true);

        HBox bottomRow = new HBox(4);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        if (change != null) {
            Label chg = new Label(change);
            chg.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px; -fx-font-weight: bold;");
            bottomRow.getChildren().add(chg);
        }
        if (note != null) {
            Label noteLabel = new Label(note);
            noteLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            bottomRow.getChildren().add(noteLabel);
        }

        card.getChildren().addAll(lbl, val, bottomRow);
        return card;
    }

    private VBox buildSalesChart() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 20;");

        HBox header = new HBox();
        VBox titleBox = new VBox(2);
        Label title = new Label("Sales Analytics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleBox.getChildren().add(title);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().add(titleBox);

        // Bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("KES");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(240);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setBarGap(2);
        chart.setCategoryGap(10);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        // Load real daily sale revenue for current week from DB
        java.util.Map<String, Integer> counts = saleDAO.getDailyCountsThisWeek();
        // Also get revenue — reuse monthly revenue logic scaled to daily
        String[] orderedDays = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        // Map weekday name → revenue from DB (approximate via count * avg)
        // Use count for bar height; multiply by avg sale value for KES axis
        double todayRev = saleDAO.getTodayRevenue();
        int todayCount  = saleDAO.getTodayCount();
        double avgSale  = (todayCount > 0) ? todayRev / todayCount : 5000;
        for (String day : orderedDays) {
            int cnt = counts.getOrDefault(day, 0);
            series.getData().add(new XYChart.Data<>(day, cnt * avgSale));
        }
        chart.getData().add(series);
        chart.lookup(".default-color0.chart-bar").setStyle("-fx-bar-fill: #2563eb;");

        card.getChildren().addAll(header, chart);
        return card;
    }

    private VBox buildOutstandingPayments() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 20;");

        Label title = new Label("Outstanding Payments");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        card.getChildren().add(title);

        // Sample outstanding items
        String[][] items = {
            {"Mombasa Cement Ltd", "KES 245,000", "OVERDUE", "#dc2626"},
            {"Crown Paints Kenya", "KES 112,500", "DUE SOON", "#d97706"},
            {"Total Energies", "KES 45,200", "PENDING", "#94a3b8"},
            {"KenPlastics Groups", "KES 18,900", "PENDING", "#94a3b8"}
        };

        for (String[] item : items) {
            VBox row = new VBox(4);
            row.setPadding(new Insets(10, 0, 10, 0));
            row.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            HBox topRow = new HBox();
            Label name = new Label(item[0]);
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
            HBox.setHgrow(name, Priority.ALWAYS);
            Label status = new Label(item[2]);
            status.setStyle("-fx-text-fill: " + item[3] + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            topRow.getChildren().addAll(name, status);

            Label amtLabel = new Label("Amount Due");
            amtLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            Label amount = new Label(item[1]);
            amount.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

            row.getChildren().addAll(topRow, amtLabel, amount);
            card.getChildren().add(row);
        }

        Hyperlink viewAll = new Hyperlink("View All Payables");
        viewAll.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 13px;");
        viewAll.setOnAction(e -> SceneManager.getInstance().navigateTo("purchases"));
        card.getChildren().add(viewAll);

        return card;
    }

    private VBox buildLowStockCard() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 20;");

        HBox header = new HBox();
        Label title = new Label("Low Stock Alerts");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(title, Priority.ALWAYS);
        Hyperlink viewAll = new Hyperlink("View All");
        viewAll.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 13px;");
        viewAll.setOnAction(e -> SceneManager.getInstance().navigateTo("inventory"));
        header.getChildren().addAll(title, viewAll);

        List<Product> lowStock = productDAO.findLowStock();
        if (lowStock.isEmpty()) {
            Label ok = new Label("All stock levels are healthy ✓");
            ok.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13px;");
            card.getChildren().addAll(header, ok);
            return card;
        }

        card.getChildren().add(header);
        int limit = Math.min(4, lowStock.size());
        for (Product p : lowStock.subList(0, limit)) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));
            row.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            StackPane dot = new StackPane();
            dot.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 8; -fx-padding: 8;");
            Label dotL = new Label("▲");
            dotL.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            dot.getChildren().add(dotL);

            VBox info = new VBox(2);
            Label name = new Label(p.getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
            Label cat = new Label(p.getCategoryName() != null ? p.getCategoryName() : "—");
            cat.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            info.getChildren().addAll(name, cat);
            HBox.setHgrow(info, Priority.ALWAYS);

            String qtyColor = p.isOutOfStock() ? "#dc2626" : "#d97706";
            Label qty = new Label((int) p.getStockQuantity() + " " + (p.getUnit() != null ? p.getUnit() : "pcs"));
            qty.setStyle("-fx-text-fill: " + qtyColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");

            row.getChildren().addAll(dot, info, qty);
            card.getChildren().add(row);
        }
        return card;
    }

    private VBox buildTopSellingCard() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 20;");

        HBox header = new HBox();
        Label title = new Label("Top Selling");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(title, Priority.ALWAYS);
        Label period = new Label("This Week ▾");
        period.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand;");
        header.getChildren().addAll(title, period);

        String[][] topSelling = {
            {"1", "Dulux Paint 20L",      "124 sales", "KES 458,800"},
            {"2", "Safety Boots",          "89 sales",  "KES 267,000"},
            {"3", "Solar Panel 200W",      "45 sales",  "KES 675,000"}
        };

        card.getChildren().add(header);
        for (String[] item : topSelling) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 0, 8, 0));
            row.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            Label rank = new Label("#" + item[0]);
            rank.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-min-width: 24;");

            StackPane imgPlaceholder = new StackPane();
            imgPlaceholder.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 6; -fx-min-width: 36; -fx-min-height: 36;");
            Label imgL = new Label("⊞");
            imgL.setStyle("-fx-font-size: 14px; -fx-text-fill: #2563eb;");
            imgPlaceholder.getChildren().add(imgL);

            VBox info = new VBox(2);
            Label name = new Label(item[1]);
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
            Label sales = new Label(item[2]);
            sales.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            info.getChildren().addAll(name, sales);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label revenue = new Label(item[3]);
            revenue.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

            row.getChildren().addAll(rank, imgPlaceholder, info, revenue);
            card.getChildren().add(row);
        }
        return card;
    }

    // ── Predictive Analytics Card ─────────────────────────────────────────

    private VBox buildAnalyticsCard() {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
            "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 20;");

        HBox header = new HBox();
        Label title = new Label("✦ AI Insights");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(title, Priority.ALWAYS);
        Label badge = new Label("LIVE");
        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; " +
            "-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 2 8;");
        header.getChildren().addAll(title, badge);
        card.getChildren().add(header);

        // Generate insights from live DAO data asynchronously
        VBox insightsBox = new VBox(8);
        Label loading = new Label("Analysing business data...");
        loading.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        insightsBox.getChildren().add(loading);
        card.getChildren().add(insightsBox);

        AsyncTask.run(() -> generateInsights(), insights -> {
            insightsBox.getChildren().clear();
            for (String[] insight : insights) {
                insightsBox.getChildren().add(insightRow(insight[0], insight[1], insight[2]));
            }
        }, err -> {
            insightsBox.getChildren().clear();
            insightsBox.getChildren().add(new Label("Could not load insights."));
        });

        return card;
    }

    private List<String[]> generateInsights() {
        List<String[]> insights = new java.util.ArrayList<>();
        try {
            // Insight 1: Stock forecast
            List<Product> lowStock = productDAO.findLowStock();
            if (!lowStock.isEmpty()) {
                insights.add(new String[]{"⚠️", lowStock.size() + " products below reorder level",
                    lowStock.stream().limit(2).map(Product::getName)
                        .collect(java.util.stream.Collectors.joining(", "))});
            }

            // Insight 2: Revenue trend
            double todayRev = saleDAO.getTodayRevenue();
            double weekRev  = saleDAO.getWeekRevenue();
            double avgDaily = weekRev / 7;
            if (todayRev > 0 && avgDaily > 0) {
                double pct = ((todayRev - avgDaily) / avgDaily) * 100;
                String dir = pct >= 0 ? "📈" : "📉";
                insights.add(new String[]{dir,
                    String.format("Today %.1f%% vs 7-day average", Math.abs(pct)),
                    String.format("Today KES %.0f | Avg KES %.0f", todayRev, avgDaily)});
            }

            // Insight 3: Deni outstanding
            try {
                DeniDAO deniDAO = new DeniDAO();
                deniDAO.ensureTables();
                double outstanding = deniDAO.getTotalOutstanding();
                int debtors        = deniDAO.getDebtorCount();
                int overdue        = deniDAO.getOverdueCount();
                if (outstanding > 0) {
                    insights.add(new String[]{"💰",
                        String.format("KES %.0f credit outstanding", outstanding),
                        debtors + " debtors" + (overdue > 0 ? " · " + overdue + " overdue" : "")});
                }
            } catch (Exception ignored) {}

            // Insight 4: Slow movers (products with stock but no recent sales)
            List<Product> all = productDAO.findAll();
            long overstocked = all.stream()
                .filter(p -> p.getStockQuantity() > p.getMinStockLevel() * 3)
                .count();
            if (overstocked > 0) {
                insights.add(new String[]{"🔄",
                    overstocked + " products may be overstocked",
                    "Consider promotions or reducing reorder quantities"});
            }

            if (insights.isEmpty()) {
                insights.add(new String[]{"✅", "All systems normal", "No anomalies detected today"});
            }
        } catch (Exception e) {
            insights.add(new String[]{"ℹ️", "Insights unavailable", e.getMessage()});
        }
        return insights;
    }

    private HBox insightRow(String icon, String main, String sub) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 16px; -fx-min-width: 24;");

        VBox text = new VBox(2);
        Label mainLbl = new Label(main);
        mainLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1e293b;");
        mainLbl.setWrapText(true);
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        subLbl.setWrapText(true);
        text.getChildren().addAll(mainLbl, subLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        row.getChildren().addAll(ic, text);
        return row;
    }
}

