package kr.jjory.jcodex.command;

import kr.jjory.jcodex.JCodexPlugin;
import kr.jjory.jcodex.gui.CodexMainGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 일반 유저용 도감 명령어 클래스입니다.
 */
public class CodexCommand implements CommandExecutor {

    private final JCodexPlugin plugin;

    public CodexCommand(JCodexPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        // GUI 열기
        plugin.getGuiManager().openGui(new CodexMainGUI(player, plugin));
        return true;
    }
}
