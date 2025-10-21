package kr.jjory.jcodex;

import kr.jjory.jcodex.command.*;
import kr.jjory.jcodex.config.ConfigManager;
import kr.jjory.jcodex.gui.GuiManager;
import kr.jjory.jcodex.listener.GUIInteractionListener;
import kr.jjory.jcodex.loader.CodexDataLoader;
import kr.jjory.jcodex.service.*;
import kr.jjory.jcodex.storage.DatabaseManager;
import kr.jjory.jcodex.storage.RedisClient;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * 플러그인의 메인 클래스입니다.
 */
public final class JCodexPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RedisClient redisClient;
    private GuiManager guiManager;

    private CodexService codexService;
    private RewardService rewardService;
    private ProgressService progressService;
    private MilestoneService milestoneService;
    private SyncService syncService;

    private static Economy econ = null;

    @Override
    public void onEnable() {
        // 설정 로드
        configManager = new ConfigManager(this);

        // 의존성 체크
        if (!setupEconomy()) {
            getLogger().severe("Vault 의존성을 찾을 수 없어 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("MMOItems") == null) {
            getLogger().warning("MMOItems 플러그인을 찾을 수 없습니다. 스탯 보상 기능이 비활성화됩니다.");
        }
        if (getServer().getPluginManager().getPlugin("ItemsAdder") == null) {
            getLogger().warning("ItemsAdder 플러그인을 찾을 수 없습니다. ItemsAdder 아이템 지원이 비활성화됩니다.");
        }

        // 데이터베이스 및 Redis 초기화
        databaseManager = new DatabaseManager(this, configManager.getMainConfig());
        redisClient = new RedisClient(this, configManager.getMainConfig());

        // 매니저 초기화
        guiManager = new GuiManager(this);

        // 서비스 초기화
        syncService = new SyncService(this);
        rewardService = new RewardService(this);
        progressService = new ProgressService(this);
        milestoneService = new MilestoneService(this);
        codexService = new CodexService(this);

        // 리스너 및 명령어 등록
        getServer().getPluginManager().registerEvents(new GUIInteractionListener(this), this);
        Objects.requireNonNull(getCommand("도감")).setExecutor(new CodexCommand(this));
        Objects.requireNonNull(getCommand("도감설정")).setExecutor(new CodexAdminCommand(this));
        Objects.requireNonNull(getCommand("도감리로드")).setExecutor(new CodexReloadCommand(this));
        Objects.requireNonNull(getCommand("도감내보내기")).setExecutor(new CodexExportCommand(this));

        // MAIN 서버일 경우, YAML -> DB 동기화 실행
        if (configManager.getMainConfig().getServerMode().equalsIgnoreCase("MAIN")) {
            getLogger().info("MAIN 서버로 설정되어 YAML 데이터베이스 동기화를 시작합니다.");
            new CodexDataLoader(this).loadAllToDatabase();
        }

        getLogger().info("JCodex 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (redisClient != null) {
            redisClient.close();
        }
        getLogger().info("JCodex 플러그인이 비활성화되었습니다.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    // Getters
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public RedisClient getRedisClient() { return redisClient; }
    public GuiManager getGuiManager() { return guiManager; }
    public CodexService getCodexService() { return codexService; }
    public RewardService getRewardService() { return rewardService; }
    public ProgressService getProgressService() { return progressService; }
    public MilestoneService getMilestoneService() { return milestoneService; }
    public SyncService getSyncService() { return syncService; }
}
