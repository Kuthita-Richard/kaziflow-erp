package com.kaziflow.services;

import com.kaziflow.database.DatabaseManager;
import com.kaziflow.utils.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit logging service.
 * Records all significant user actions to the audit_log table.
 * Call AuditLog.log("action", "description") anywhere in the app.
 */
public class AuditLog {

    private static final String INSERT_SQL =
        "INSERT INTO audit_log (user_id, user_name, action, description, module, record_id, ip_address) VALUES (?,?,?,?,?,?,?)";

    // ─── Log Methods ─────────────────────────────────────────────────────────

    public static void log(String action, String description) {
        log(action, description, null, null);
    }

    public static void log(String action, String description, String module, Integer recordId) {
        int userId = 0;
        String userName = "system";
        try {
            var user = SessionManager.getInstance().getCurrentUser();
            if (user != null) { userId = user.getId(); userName = user.getName(); }
        } catch (Exception ignored) {}

        final int fUserId = userId;
        final String fUserName = userName;
        // Write asynchronously to avoid blocking the UI thread
        new Thread(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                ps.setInt(1, fUserId);
                ps.setString(2, fUserName);
                ps.setString(3, action);
                ps.setString(4, description);
                ps.setString(5, module);
                if (recordId != null) ps.setInt(6, recordId); else ps.setNull(6, Types.INTEGER);
                ps.setString(7, "127.0.0.1");
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[AuditLog] Failed to write: " + e.getMessage());
            }
        }).start();
    }

    // Convenience methods for common actions
    public static void logSale(int saleId, double total) {
        log("SALE_COMPLETED", "Sale completed. Total: KES " + String.format("%.2f", total), "sales", saleId);
    }

    public static void logLogin(String email) {
        log("USER_LOGIN", "User logged in: " + email, "auth", null);
    }

    public static void logLogout(String email) {
        log("USER_LOGOUT", "User logged out: " + email, "auth", null);
    }

    public static void logProductEdit(int productId, String productName) {
        log("PRODUCT_UPDATED", "Product updated: " + productName, "inventory", productId);
    }

    public static void logExpenseAdded(double amount, String category) {
        log("EXPENSE_ADDED", "Expense added: KES " + String.format("%.2f", amount) + " (" + category + ")", "finance", null);
    }

    public static void logPurchaseOrder(int poId, String poNumber, double total) {
        log("PURCHASE_ORDER_CREATED", "PO created: " + poNumber + " — KES " + String.format("%.2f", total), "purchases", poId);
    }

    public static void logUserCreated(String userName) {
        log("USER_CREATED", "New user created: " + userName, "settings", null);
    }

    public static void logBackup(String filePath) {
        log("BACKUP_CREATED", "Database backed up to: " + filePath, "settings", null);
    }

    // ─── Read Methods ─────────────────────────────────────────────────────────

    public static List<String[]> getRecentLogs(int limit) {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(new String[]{
                    rs.getString("user_name"),
                    rs.getString("action"),
                    rs.getString("description"),
                    rs.getString("module") != null ? rs.getString("module") : "system",
                    rs.getString("created_at")
                });
            }
        } catch (Exception e) {
            System.err.println("[AuditLog] Read failed: " + e.getMessage());
        }
        return logs;
    }

    public static int getTodayCount() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM audit_log WHERE DATE(created_at) = DATE('now')");
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}
