package kr.jjory.jcodex.command;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.loader.CodexDataLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * 설정 파일을 리로드하는 명령어 클래스입니다.
 */
public class CodexReloadCommand implements CommandExecutor {

    private final JCodexPlugin plugin;

    public CodexReloadCommand(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("jcodex.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        // MAIN 서버에서만 실행 가능
        if (!plugin.getConfigManager().getMainConfig().getServerMode().equalsIgnoreCase("MAIN")) {
            sender.sendMessage("§c이 명령어는 MAIN 서버에서만 사용할 수 있습니다.");
            return true;
        }

        sender.sendMessage("§aJCodex 플러그인 설정을 리로드합니다...");
        plugin.getConfigManager().reloadConfigs();

        sender.sendMessage("§aYAML 파일을 데이터베이스와 동기화합니다...");
        new CodexDataLoader(plugin).loadAllToDatabase().thenRun(() -> {
            sender.sendMessage("§a리로드 및 동기화가 완료되었습니다.");
            plugin.getSyncService().publishReload();
        });

        return true;
    }
}
