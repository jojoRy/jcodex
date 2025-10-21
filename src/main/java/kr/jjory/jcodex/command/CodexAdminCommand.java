package kr.jjory.jcodex.command;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.gui.CodexAdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 관리자용 도감 설정 명령어 클래스입니다.
 */
public class CodexAdminCommand implements CommandExecutor {

    private final JCodexPlugin plugin;

    public CodexAdminCommand(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        // GUI 열기
        plugin.getGuiManager().openGui(new CodexAdminGUI(player, plugin, null));
        return true;
    }
}
