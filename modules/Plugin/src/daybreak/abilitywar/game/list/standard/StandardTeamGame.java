package daybreak.abilitywar.game.list.standard;

import com.google.common.base.Strings;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.serializable.SpawnLocation;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.game.team.TeamGame;
import daybreak.abilitywar.game.team.interfaces.Members;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;
import java.util.List;


public class StandardTeamGame extends TeamGame implements DefaultKitHandler, TeamGame.Winnable, AbstractGame.Observer {

	public StandardTeamGame(final String[] args) {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS(), args);
		setRestricted(Settings.InvincibilitySettings.isEnabled());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}

	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				preStartGame();
				List<String> lines = Messager.asList("§6==== §e게임 참여자 목록 §6====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§a" + count + ". §f" + p.getTeam().getDisplayName() + ChatColor.RESET + (p.isSuperPlayer() ? ChatColor.UNDERLINE : "") + p.getPlayer().getName());
				}
				lines.add("§e총 인원수 : " + count + "명");
				lines.add("§6===========================");

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}

				if (getParticipants().size() < 1) {
					stop();
					Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§71명§8)");
				}
				break;
			case 3:
				lines = Messager.asList(
						"§cAbilityWar §f- §6능력자 전쟁",
						"§e버전 §7: §f" + AbilityWar.getPlugin().getDescription().getVersion() + "-ludongbu",
						"§b개발자 §7: §fDaybreak 새벽, Eunhak Lee 고구마",
						"§9디스코드 §7: §fsaebyeog"
				);

				GameCreditEvent event = new GameCreditEvent(this);
				Bukkit.getPluginManager().callEvent(event);
				lines.addAll(event.getCredits());

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				if (Settings.getDrawAbility()) {
					for (String line : Messager.asList(
							"§f플러그인에 총 §b" + AbilityList.nameValues().size() + "개§f의 능력이 등록되어 있습니다.",
							"§7능력을 무작위로 할당합니다...")) {
						Bukkit.broadcastMessage(line);
					}
					try {
						startAbilitySelect();
					} catch (OperationNotSupportedException ignored) {
					}
				}
				break;
			case 6:
				if (Settings.getDrawAbility()) {
					Bukkit.broadcastMessage("§f모든 참가자가 능력을 §b확정§f했습니다.");
				} else {
					Bukkit.broadcastMessage("§f능력자 게임 설정에 따라 §b능력§f을 추첨하지 않습니다.");
				}
				break;
			case 8:
				Bukkit.broadcastMessage("§e잠시 후 게임이 시작됩니다.");
				break;
			case 10:
				Bukkit.broadcastMessage("§e게임이 §c5§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage("§e게임이 §c4§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage("§e게임이 §c3§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				Bukkit.broadcastMessage("§e게임이 §c2§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 14:
				Bukkit.broadcastMessage("§e게임이 §c1§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 15:
				if (Seasons.isChristmas()) {
					final String blocks = Strings.repeat("§c■§2■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f            §cAbilityWar §f- §2능력자 전쟁  ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					for (String line : Messager.asList(
							"§e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
							"§f             §cAbilityWar §f- §6능력자 전쟁  ",
							"§f                    게임 시작                ",
							"§e■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
						Bukkit.broadcastMessage(line);
					}
				}

				giveDefaultKit(getParticipants());

				if (Settings.getNoHunger()) {
					Bukkit.broadcastMessage("§2배고픔 무제한§a이 적용됩니다.");
				} else {
					Bukkit.broadcastMessage("§4배고픔 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getInfiniteDurability()) {
					addModule(new InfiniteDurability());
				} else {
					Bukkit.broadcastMessage("§4내구도 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getClearWeather()) {
					for (World world : Bukkit.getWorlds()) world.setStorm(false);
				}

				if (isRestricted()) {
					getInvincibility().start(false);
				} else {
					Bukkit.broadcastMessage("§4초반 무적§c이 적용되지 않습니다.");
					setRestricted(false);
				}

				ScriptManager.runAll(this);

				startGame();
				break;
			case 16:
				checkWinner();
				break;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onRespawn(PlayerRespawnEvent event) {
		if (!isParticipating(event.getPlayer())) return;

		Participant participant = getParticipant(event.getPlayer());
		Members team = participant.getTeam();
		if (team == null) return;

		SpawnLocation loc = team.getSpawn();
		if (loc == null) return;

		event.setRespawnLocation(loc.toBukkitLocation());
	}

	@EventHandler
	private void onInteractAtDiamondBlock(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) return;
		else if (!isGameStarted()) return;
		else if (!isParticipating(event.getPlayer())) return;
		else if (clickedBlock.getType() != Material.DIAMOND_BLOCK) return;

		EquipmentSlot hand = event.getHand();
		if (hand == null) return;

		ItemStack itemInHand = event.getPlayer().getInventory().getItem(hand);
		if (!itemInHand.getType().getKey().toString().toLowerCase().contains("pickaxe")) return;

		SoundLib.BLOCK_NOTE_BLOCK_BELL.playSound(event.getPlayer());
		NMS.sendActionbar(event.getPlayer(), ChatColor.RED + "" + ChatColor.BOLD + "다이아몬드 블록은 맨 손으로만 캘 수있습니다", 20, 40, 20);
		event.setCancelled(true);
	}

	@EventHandler
	private void onBlockBreak(BlockBreakEvent event) {
		if (!isGameStarted()) return;
		else if (event.getBlock().getType() != Material.DIAMOND_BLOCK) return;
		else if (!isParticipating(event.getPlayer())) return;

		Members nearestTeam = getNearestTeam(event);
		SoundLib.ENTITY_ENDER_DRAGON_GROWL.broadcastSound(0.5f);
		Bukkit.broadcastMessage("");
		Bukkit.broadcastMessage("§l§f   " + nearestTeam.getDisplayName() + ChatColor.GOLD + " 팀 탈락");
		Bukkit.broadcastMessage("");

		for (AbstractGame.Participant member : nearestTeam.getMembers()) {
			((ManuallyExcludableDeathManager) getDeathManager()).excludePlayer(member);
			member.getPlayer().setGameMode(GameMode.SPECTATOR);
		}
		if (isGameStarted()) checkWinner();
	}

	private @NotNull Members getNearestTeam(BlockBreakEvent event) {
		Location loc = event.getBlock().getLocation();

		double minDistance = Double.MAX_VALUE;
		Members nearestTeam = null;

		for (Members team : getTeams()) {
			if (team.isExcluded()) continue;

			SpawnLocation spawnLoc = team.getSpawn();
			double distanceSquared = Math.pow(loc.getX() - spawnLoc.x, 2) + Math.pow(loc.getY() - spawnLoc.y, 2) + Math.pow(loc.getZ() - spawnLoc.z, 2);

			if (distanceSquared < minDistance) {
				nearestTeam = team;
				minDistance = distanceSquared;
			}
		}

		assert nearestTeam != null;
		return nearestTeam;
	}

	private void checkWinner() {
		Members winTeam = null;
		for (Members team : getTeams()) {
			if (!team.isExcluded()) {
				if (winTeam == null) {
					winTeam = team;
				} else {
					return;
				}
			}
		}
		if (winTeam != null) {
			Win(winTeam);
		} else {
			Win("§f없음 §8(§7무승부§8)");
		}
	}

	@Override
	public void update(GameUpdate update) {
		if (update == GameUpdate.END) {
			HandlerList.unregisterAll(this);
		}
	}

	@Override
	protected @NotNull DeathManager newDeathManager() {
		return new ManuallyExcludableDeathManager(this);
	}

	private static class ManuallyExcludableDeathManager extends DeathManager {
		public ManuallyExcludableDeathManager(Game game) {
			super(game);
		}

		protected void excludePlayer(final AbstractGame.Participant participant) {
			excludedPlayers.add(participant.getPlayer().getUniqueId());
		}
	}
}
