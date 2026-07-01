package com.kaziflow.modules;

import com.kaziflow.database.DatabaseManager;
import com.kaziflow.modules.core.*;
import javafx.scene.layout.Region;

import java.sql.*;
import java.util.*;

/**
 * ModuleRegistry — the central plug-in/plug-out engine for KaziFlow ERP.
 *
 * Responsibilities:
 *   1. Loads the active IndustryProfile from the database on startup
 *   2. Initialises enabled Module instances and calls onLoad()
 *   3. Provides the ordered list of nav items for MainLayout
 *   4. Builds module views on demand (used by SceneManager)
 *   5. Handles enable/disable of individual modules at runtime
 *
 * Usage:
 *   ModuleRegistry reg = ModuleRegistry.getInstance();
 *   reg.init();  // call once in App.start()
 *
 *   List<Module> navModules = reg.getNavModules();
 *   Region view = reg.buildView("inventory");
 *   IndustryProfile profile = reg.getActiveProfile();
 */
public class ModuleRegistry {

    private static ModuleRegistry instance;

    private IndustryProfile activeProfile = IndustryProfile.GENERAL_RETAIL;
    private final Map<String, Module> activeModules = new LinkedHashMap<>();
    private boolean initialized = false;

    // ── All available core modules (always present in codebase) ──────────
    private static final List<Module> CORE_MODULES = List.of(
        new DashboardModule(),
        new InventoryModule(),
        new BatchModule(),
        new SalesModule(),
        new PurchasesModule(),
        new AppointmentModule(),
        new PatientModule(),
        new RestaurantModule(),
        new WorkshopModule(),
        new GymModule(),
        new LaundryModule(),
        new HotelModule(),
        new FuelStationModule(),
        new SchoolCanteenModule(),
        new EmployeesModule(),
        new PayrollModule(),
        new FinanceModule(),
        new ReportsModule(),
        new DeniModule(),
        new QuotationModule(),
        new SettingsModule(),
        new HelpModule()
    );

    private ModuleRegistry() {}

    public static ModuleRegistry getInstance() {
        if (instance == null) instance = new ModuleRegistry();
        return instance;
    }

    // ── Initialisation ────────────────────────────────────────────────────

    /**
     * Load the active profile from DB and initialise all enabled modules.
     * Call once in App.start() after DatabaseManager.initialize().
     */
    public void init() {
        if (initialized) return;
        IndustryProfile fromDb = loadProfileFromDb();
        // On first run loadProfileFromDb() returns null — default to GENERAL_RETAIL
        activeProfile = (fromDb != null) ? fromDb : IndustryProfile.GENERAL_RETAIL;
        loadModules();
        initialized = true;
        System.out.println("[ModuleRegistry] Profile: " + activeProfile.displayName
            + " | Modules: " + activeModules.keySet());
    }

    private void loadModules() {
        activeModules.clear();
        for (Module m : CORE_MODULES) {
            if (activeProfile.hasModule(m.getId())) {
                try {
                    m.onLoad();
                    activeModules.put(m.getId(), m);
                } catch (Exception e) {
                    System.err.println("[ModuleRegistry] Failed to load module: " + m.getId());
                    e.printStackTrace();
                }
            }
        }
    }

    // ── Profile management ────────────────────────────────────────────────

    public IndustryProfile getActiveProfile() { return activeProfile; }

    public boolean isFirstRun() {
        return loadProfileFromDb() == null;
    }

    public void setProfile(IndustryProfile profile) {
        this.activeProfile = profile;
        saveProfileToDb(profile);
        loadModules(); // reload modules for new profile
    }

    public String term(String key, String defaultLabel) {
        return activeProfile.term(key, defaultLabel);
    }

    // ── Module access ─────────────────────────────────────────────────────

    public boolean isEnabled(String moduleId) {
        return activeModules.containsKey(moduleId);
    }

    /**
     * Returns modules for the main sidebar nav (top section),
     * in the order they were registered.
     */
    public List<Module> getNavModules() {
        return activeModules.values().stream()
            .filter(m -> !m.isBottomNav())
            .toList();
    }

    /**
     * Returns modules for the bottom sidebar section (Settings, Help).
     */
    public List<Module> getBottomNavModules() {
        return activeModules.values().stream()
            .filter(Module::isBottomNav)
            .toList();
    }

    /**
     * Build and return the view for a module.
     * Returns an error pane if the module is not enabled or not found.
     */
    public Region buildView(String moduleId) {
        Module m = activeModules.get(moduleId);
        if (m != null) return m.buildView();
        // Fallback to dashboard
        Module dash = activeModules.get("dashboard");
        return dash != null ? dash.buildView() : new javafx.scene.layout.StackPane();
    }

    /**
     * Returns all module IDs that should NOT be cached in SceneManager
     * (i.e. modules that declared noCache() = true).
     */
    public Set<String> getNoCacheModules() {
        Set<String> result = new HashSet<>();
        activeModules.forEach((id, m) -> { if (m.noCache()) result.add(id); });
        return result;
    }

    // ── DB persistence ────────────────────────────────────────────────────

    private IndustryProfile loadProfileFromDb() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM settings WHERE key='industry_profile'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return IndustryProfile.fromKey(rs.getString("value"));
        } catch (Exception e) { /* table may not exist yet on first run */ }
        return null; // null = first run, profile not set
    }

    private void saveProfileToDb(IndustryProfile profile) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO settings (key, value) VALUES ('industry_profile', ?)")) {
            ps.setString(1, profile.name());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
