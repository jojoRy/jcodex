package kr.jjory.jcodex.command;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.loader.CodexDataLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * 데이터베이스의 도감 데이터를 YAML 파일로 내보내는 명령어 클래스입니다.
 */
public class CodexExportCommand implements CommandExecutor {
    private final JCodexPlugin plugin;

    public CodexExportCommand(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("jcodex.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (!plugin.getConfigManager().getMainConfig().getServerMode().equalsIgnoreCase("MAIN")) {
            sender.sendMessage("§c이 명령어는 MAIN 서버에서만 사용할 수 있습니다.");
            return true;
        }

        sender.sendMessage("§a데이터베이스의 도감 데이터를 YAML 파일로 내보내기를 시작합니다...");
        new CodexDataLoader(plugin).exportAllFromDatabase().thenRun(() -> {
            sender.sendMessage("§a내보내기가 완료되었습니다. codex_items_export.yml, milestones_export.yml 파일을 확인하세요.");
        }).exceptionally(ex -> {
            sender.sendMessage("§c내보내기 중 오류가 발생했습니다: " + ex.getMessage());
            return null;
        });

        return true;
    }
}
