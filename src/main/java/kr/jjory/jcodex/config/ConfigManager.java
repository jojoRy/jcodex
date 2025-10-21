package kr.jjory.jcodex.config;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.util.YamlUtil;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 플러그인의 설정 파일을 관리하는 클래스입니다.
 */
public class ConfigManager {

    private final JCodexPlugin plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration codexItemsConfig;
    private FileConfiguration milestonesConfig;

    private MainConfig configWrapper;

    public ConfigManager(JCodexPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        // config.yml 로드
        plugin.saveDefaultConfig();
        mainConfig = plugin.getConfig();
        configWrapper = new MainConfig(mainConfig);

        // codex_items.yml 로드
        codexItemsConfig = YamlUtil.loadYaml(plugin, "codex_items.yml");
        // milestones.yml 로드
        milestonesConfig = YamlUtil.loadYaml(plugin, "milestones.yml");
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        configWrapper = new MainConfig(mainConfig);

        codexItemsConfig = YamlUtil.loadYaml(plugin, "codex_items.yml");
        milestonesConfig = YamlUtil.loadYaml(plugin, "milestones.yml");
    }

    public MainConfig getMainConfig() {
        return configWrapper;
    }

    public FileConfiguration getCodexItemsConfig() {
        return codexItemsConfig;
    }

    public FileConfiguration getMilestonesConfig() {
        return milestonesConfig;
    }
}
