package kr.jjory.jcodex.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Lang {
    private static YamlConfiguration enConfig;
    private static YamlConfiguration koConfig;
    private static JavaPlugin plugin;

    public static void init(JavaPlugin pl) {
        plugin = pl;
        File dataFolder = plugin.getDataFolder();

        File koFile = new File(dataFolder, "items_KO.yml");
        File enFile = new File(dataFolder, "items_EN.yml");

        if (!koFile.exists()) plugin.saveResource("items_KO.yml", false);
        if (!enFile.exists()) plugin.saveResource("items_EN.yml", false);

        koConfig = YamlConfiguration.loadConfiguration(koFile);
        enConfig = YamlConfiguration.loadConfiguration(enFile);
    }

    /**
     * 주어진 키에 대한 번역을 가져옵니다.
     * - 한국어 로케일이고 KO.yml에 키가 있으면 → 한국어 반환
     * - EN.yml에 키가 있으면 → 영어 반환
     * - 둘 다 없으면 → null 반환 (즉, 번역하지 않음)
     */
    public static String translate(Player player, String key) {
        if (key == null) return null;

        // Paper API 사용 (더 안정적)
        String locale = player.getLocale().toLowerCase();
        boolean isKorean = locale.startsWith("ko");

        if (isKorean && koConfig.contains(key)) {
            return koConfig.getString(key);
        }
        // 한국어가 아니거나 한국어 번역이 없을 때 영어 시도
        if (enConfig.contains(key)) {
            return enConfig.getString(key);
        }
        return null; // 번역 없음
    }
}
