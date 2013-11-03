package net.miblue.MiBluePvP;

import org.bukkit.plugin.java.*;
import java.util.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import net.milkbowl.vault.economy.*;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.*;
import org.bukkit.event.block.*;
import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public final class MiBluePvP extends JavaPlugin implements Listener{
	
	SettingsManager settings = SettingsManager.getInstance();

    public static final HashMap<Player, Integer> killstreak;
    public static final HashMap<Player, String> latestkill;
    public static final HashMap<Player, Player> latesthitby;
    public static Economy econ = null;
    public static Permission perms = null;
   
    static {
        killstreak = new HashMap<Player, Integer>();
        latestkill = new HashMap<Player, String>();
        latesthitby = new HashMap<Player, Player>();
        MiBluePvP.econ = null;
    }
   // Enable Setup
    public void onEnable() {
    	if(!setupEconomy()){
			getLogger().severe(String.format("[%s] - Disabled due to: no Vault dependency found!",getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
    	}
    	settings.setup(this);
        Bukkit.getLogger().info(getDescription().getName() + " has been enabled!");
        Bukkit.getLogger().info(getDescription().getName() + " " + getDescription().getVersion() + "written by " + getDescription().getAuthors() + " successfully enabled!");
        this.setupEconomy();
        Bukkit.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        for (Player p : Bukkit.getOnlinePlayers()){
            MiBluePvP.killstreak.put(p, 0);
            MiBluePvP.latestkill.put(p, "no kills");
        }
        setupPermissions();
    }
   // Economy Setup
    private boolean setupEconomy(){
		if (getServer().getPluginManager().getPlugin("Vault") == null){
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null){
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
    // Permission Setup
    private boolean setupPermissions(){
    	RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
    	perms = rsp.getProvider();
    	return perms != null;
    }
   // Disable Setup
    public void onDisable() {
        Bukkit.getLogger().info(getDescription().getName() + " has been disabled! Cya later!");
    }
   // String Builder
    public String getPrefix() {
        final String prefix = new StringBuilder().append(ChatColor.GOLD).append(ChatColor.ITALIC).toString();
        return prefix;
    }
   // Events Setup
    @EventHandler
    public void onRightClick(final PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Player player = event.getPlayer();
            if (player.getItemInHand().getType() == Material.BLAZE_ROD) {
                player.sendMessage(new StringBuilder().append(ChatColor.GOLD).append(ChatColor.BOLD).append("-------------[MiBluePvP]-------------").toString());
                player.sendMessage(String.valueOf(this.getPrefix()) + "Latest kill: " + ChatColor.GRAY + ChatColor.BOLD + MiBluePvP.latestkill.get(player));
                player.sendMessage(String.valueOf(this.getPrefix()) + "Killstreak: " + ChatColor.GRAY + ChatColor.BOLD + MiBluePvP.killstreak.get(player));
                player.sendMessage(new StringBuilder().append(ChatColor.GOLD).append(ChatColor.BOLD).append("-------------[MiBluePvP]-------------").toString());
            }
        }
    }
   
    @EventHandler
    public void onPlayerHit(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            MiBluePvP.latesthitby.put((Player)event.getEntity(), (Player)event.getDamager());
        } else if (event.getDamager() instanceof Arrow && event.getEntity() instanceof Player) {
            Arrow arrow = (Arrow) event.getDamager();
            if(arrow.getShooter() instanceof Player){
                MiBluePvP.latesthitby.put((Player)event.getEntity(), (Player) arrow.getShooter());
            }
        }
    }
   
    @EventHandler
    public void onPlayerKill(final PlayerDeathEvent event) {
        try {
            final Player killed = event.getEntity();
            final Player killer = MiBluePvP.latesthitby.get(killed);
            MiBluePvP.latestkill.put(killer, killed.getName());
            MiBluePvP.econ.depositPlayer(killer.getName(), getConfig().getInt("on-kill"));
            killer.sendMessage(String.valueOf(this.getPrefix()) + "You have earned 50 Tokens for your kill.");
            this.killStreak(killer, "add", 1);
            event.setDeathMessage(String.valueOf(this.getPrefix()) + killed.getName() + ChatColor.DARK_GRAY + " (Killsteak: " + MiBluePvP.killstreak.get(killed) + ") was killed by " + this.getPrefix() + killer.getName() + ChatColor.DARK_GRAY + " (Killstreak: " + MiBluePvP.killstreak.get(killer) + ")");
            if (MiBluePvP.killstreak.get(killed) > 4) {
                this.getServer().broadcastMessage(new StringBuilder().append(ChatColor.DARK_AQUA).append(ChatColor.BOLD).append(killer.getName()).append(" just ended ").append(killed.getName()).append("'s ").append(MiBluePvP.killstreak.get(killed)).append(" kills killstreak!").toString());
            }
            this.killStreak(killed, "set", 0);
        }
        catch (NullPointerException ex) {}
    }
   //Killstreak Setup
    public void killStreak(final Player p, final String action, final int amount) {
        if (action.equals("set")) {
            MiBluePvP.killstreak.put(p, amount);
        }
        else if (action.equals("add")) {
            MiBluePvP.killstreak.put(p, MiBluePvP.killstreak.get(p) + amount);
        }
        if (killstreak.get(p) % 5 == 0) {
            this.getServer().broadcastMessage(String.valueOf(this.getPrefix()) + p.getName() + " now has a killstreak of " + MiBluePvP.killstreak.get(p) + "!");
        }
        if(killstreak.get(p) % getConfig().getInt("killstreak.every") == 0){
        	econ.depositPlayer(p.getName(), getConfig().getInt("killstreak.every.give"));
        }
        for(int num : getConfig().getIntList("killstreak.special")){
        	if(amount == num){
        		getServer().broadcastMessage(ChatColor.GOLD + ChatColor.ITALIC + p.getName() + " now has an AMAZING killstreak of " + num + "!");
        		p.sendMessage(ChatColor.GREEN + "Since you reached a massive killstreak of " + killstreak.get(p) + ", you have been given " + getConfig().getInt("killstreak.special.on-special") + ".");
        		econ.depositPlayer(p.getName(), getConfig().getInt("killstreak.special.on-special"));
        	}
        }
    }
   
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        MiBluePvP.killstreak.put(event.getPlayer(), 0);
        MiBluePvP.latestkill.put(event.getPlayer(), "no kills");
    }
    // Command Setup
    public boolean onCommand(CommandSender sender,Command cmd,String Label,String[] args){
    	if(Label.equalsIgnoreCase("mpvp")){
    		if(args.length == 0){
    			if(perms.has(sender, "mpvp.help") || sender.isOp()){
    				sender.sendMessage(ChatColor.DARK_AQUA + "Please use /mpvp help for a list of commands!" );
    				return true;
    			}else{
    				sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
    				return true;
    			}
    		}
    		if(args[0].equalsIgnoreCase("help")){
    			if(perms.has(sender, "mpvp.help") || sender.isOp()){
    				sender.sendMessage(ChatColor.DARK_AQUA + "*----MiBluePvP Help----*");
    				sender.sendMessage(ChatColor.GOLD + "/mpvp help ---> Shows list of commands");
    				sender.sendMessage(ChatColor.GOLD + "/mpvp info ---> Shows pluign version information");
    				sender.sendMessage(ChatColor.GOLD + "/mpvp reload ---> Reloads MiBluePvP config.yml");
    				return true;
    			}else{
    				sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
    				return true;
    			}
    		}else if(args[0].equalsIgnoreCase("info")){
    			if(perms.has(sender, "mpvp.admin") || sender.isOp()){
    				sender.sendMessage(ChatColor.GREEN + "[" + getDescription().getName() + "] " + getDescription().getVersion() + " written by " + getDescription().getAuthors());
    				return true;
    			}else{
    				sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
    				return true;
    			}
    		}else if(args[0].equalsIgnoreCase("reload")){
    			if(perms.has(sender, "mpvp.admin") || sender.isOp()){
    				settings.reloadConfig();
    				settings.saveConfig();
    				settings.getConfig();
    				sender.sendMessage(ChatColor.DARK_PURPLE + "MiBluePvP has been reloaded!");
    				return true;
    			}else{
    				sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
    				return true;
    			}
    		}
    	}
    	return false;
    }
}
