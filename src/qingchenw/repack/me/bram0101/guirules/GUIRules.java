package qingchenw.repack.me.bram0101.guirules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class GUIRules extends JavaPlugin
{
	private static GUIRules instance;
	private List<UUID> uuids = new ArrayList<UUID>();
	private Inventory rulegui;

	public void onEnable()
	{
		instance = this;
		if(!new File(this.getDataFolder(), "config.yml").exists()) this.saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(new RulesListener(), this);
		if(hasAuthme()) Bukkit.getPluginManager().registerEvents(new AuthMeListener(), this);
		this.reloadConfig();
		this.getLogger().info("Wc's Gui Rules Plugin has been enabled! The authors were bram0101 and MarvanCZ, reload by QingChenW");
	}

	public void onDisable()
	{
		this.saveConfig();
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
    	if (command.getName().equalsIgnoreCase("guirules"))
    	{
    		if(sender instanceof ConsoleCommandSender)
    		{
    			this.reloadConfig();
    			sender.sendMessage("[WCGUIRules] 插件配置重载完成!");
    		}
    		else
    		{
    			sender.sendMessage("[WCGUIRules] 此命令仅允许在控制台执行!");
    		}
    	}
		return false;
	}
	
	public void reloadConfig()
	{
		super.reloadConfig();
		for(String uuid : this.getConfig().getStringList("players")) uuids.add(UUID.fromString(uuid));
		String title = this.getConfig().getString("gui.name");
		title = ChatColor.translateAlternateColorCodes('&', title.substring(0, title.length() >= 32 ? 31 : title.length()));
		rulegui = Bukkit.createInventory(null, this.getConfig().getInt("gui.rows") * 9, title);
		for(int slot = 0; slot < rulegui.getSize(); ++slot)
		{
			this.setInventorySlot(rulegui, String.valueOf(slot));
		}
		this.setInventorySlot(rulegui, "agree");
		this.setInventorySlot(rulegui, "disagree");
	}
	
	public void saveConfig()
	{
		List<String> uuidstring = new ArrayList<String>();
		for(UUID uuid : uuids) uuidstring.add(uuid.toString());
		this.getConfig().set("players", uuidstring);
		super.saveConfig();
	}
	
	public void setInventorySlot(Inventory gui, String name)
	{
		try
		{
			@SuppressWarnings("deprecation")
			ItemStack item = new ItemStack(this.getConfig().getInt("gui." + name + ".id"), this.getConfig().getInt("gui." + name + ".amount"), (short) this.getConfig().getInt("gui." + name + ".data"));
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("gui." + name + ".name")));
			ArrayList<String> lores = new ArrayList<String>();
			for(String lore : this.getConfig().getStringList("gui." + name + ".lores"))
			{
				lores.add(ChatColor.translateAlternateColorCodes('&', lore));
			}
			meta.setLore(lores);
			item.setItemMeta(meta);
			try
			{
				for(String enchantment : this.getConfig().getStringList("gui." + name + ".enchantments"))
				{
					item.addEnchantment(Enchantment.getByName(enchantment.split(":")[0]), Integer.parseInt(enchantment.split(":")[1]));
				}
			}
			catch (Exception e) {}
			gui.setItem(this.getConfig().getInt("gui." + name + ".slot"), item);
		} catch (Exception e) {}
	}

	public static boolean hasAuthme()
	{
		return Bukkit.getPluginManager().getPlugin("AuthMe") != null;
	}

	public static String getBukkitVersion()
	{
		Matcher matcher = Pattern.compile("v\\d+_\\d+_R\\d+").matcher(Bukkit.getServer().getClass().getPackage().getName());
		if (matcher.find())
		{
			return matcher.group();
		}
		return null;
	}

	public static GUIRules getInstance()
	{
		return instance;
	}

	public class RulesListener implements Listener
	{
		@EventHandler
		public void onJoin(PlayerJoinEvent event)
		{
			if(!hasAuthme())
			{
				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(instance, new Runnable()
				{
					public void run()
					{
						Player eventPlayer = event.getPlayer();
						if(!instance.uuids.contains(eventPlayer.getUniqueId()))
						{
							if(instance.getConfig().getBoolean("hidePlayer"))
							{
								for(Player player : Bukkit.getOnlinePlayers())
								{
									if(!player.getName().equalsIgnoreCase(eventPlayer.getName()))
									{
										player.hidePlayer(eventPlayer);
										eventPlayer.hidePlayer(player);
									}
								}
							}
							eventPlayer.openInventory(rulegui);
						}
					}
				}, 40L);
			}
		}

		@EventHandler
		public void onInventoryClose(InventoryCloseEvent event)
		{
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(instance, new Runnable()
			{
				public void run()
				{
					if(!instance.uuids.contains(event.getPlayer().getUniqueId()))
					{
						event.getPlayer().openInventory(rulegui);
					}
				}
			}, 5L);
		}

		@EventHandler
		public void onInventoryClick(InventoryClickEvent event)
		{
			if(event.getInventory().getName().equalsIgnoreCase(rulegui.getName()))
			{
				event.setCancelled(true);
				event.setResult(Result.DENY);
				Player clickPlayer = Bukkit.getPlayer(event.getWhoClicked().getUniqueId());
				if(event.getRawSlot() == instance.getConfig().getInt("gui.agree.slot"))
				{
					instance.uuids.add(clickPlayer.getUniqueId());
					instance.saveConfig();
					event.getView().close();
					if(instance.getConfig().getBoolean("hidePlayer"))
					{
						for(Player player : Bukkit.getOnlinePlayers())
						{
							if(!player.getName().equalsIgnoreCase(clickPlayer.getName()))
							{
								player.showPlayer(clickPlayer);
								clickPlayer.showPlayer(player);
							}
						}
					}
					for(String command : instance.getConfig().getStringList("agreecommands"))
					{
						instance.getServer().dispatchCommand(instance.getServer().getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command.replace("%player%", clickPlayer.getName())));
					}
				}

				if(event.getRawSlot() == instance.getConfig().getInt("gui.disagree.slot"))
				{
					event.getView().close();
					for(String command : instance.getConfig().getStringList("disagreecommands"))
					{
						instance.getServer().dispatchCommand(instance.getServer().getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command.replace("%player%", clickPlayer.getName())));
					}
				}
			}
		}

		@EventHandler
		public void onEntityDamage(EntityDamageEvent event)
		{
			Entity entity = event.getEntity();
			if(entity instanceof Player && !uuids.contains(entity.getUniqueId()))
			{
				event.setCancelled(true);
				event.setDamage(0.0D);
			}
		}

		@EventHandler
		public void onEntityRegainHealth(EntityRegainHealthEvent event)
		{
			Entity entity = event.getEntity();
			if(entity instanceof Player && !uuids.contains(entity.getUniqueId()))
			{
				event.setCancelled(true);
			}
		}

		@EventHandler
		public void onFoodLevelChange(FoodLevelChangeEvent event)
		{
			Entity entity = event.getEntity();
			if(entity instanceof Player && !uuids.contains(entity.getUniqueId()))
			{
				event.setCancelled(true);
			}
		}
	}

	public class AuthMeListener implements Listener
	{
		@SuppressWarnings("deprecation")
		@EventHandler
		public void onLogin(fr.xephi.authme.events.LoginEvent event)
		{
			if(event.isLogin())
			{
				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(instance, new Runnable()
				{
					public void run()
					{
						Player eventPlayer = event.getPlayer();
						if(!instance.uuids.contains(eventPlayer.getUniqueId()))
						{
							if(instance.getConfig().getBoolean("hidePlayer"))
							{
								for(Player player : Bukkit.getOnlinePlayers())
								{
									if(!player.getName().equalsIgnoreCase(eventPlayer.getName()))
									{
										player.hidePlayer(eventPlayer);
										eventPlayer.hidePlayer(player);
									}
								}
							}
							eventPlayer.openInventory(rulegui);
						}
					}
				}, 20L);
			}
		}
	}
}
