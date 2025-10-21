package kr.jjory.jcodex.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * YAML 파일을 쉽게 로드하고 저장하기 위한 유틸리티 클래스입니다.
 */
public class YamlUtil {

    public static FileConfiguration loadYaml(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void saveYaml(JavaPlugin plugin, FileConfiguration config, String fileName) {
        try {
            config.save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException e) {
            plugin.getLogger().severe(fileName + " 파일 저장 중 오류 발생: " + e.getMessage());
        }
    }
}
