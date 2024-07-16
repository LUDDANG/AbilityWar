package daybreak.abilitywar.config.serializable.team;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.serializable.SpawnLocation;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.team.interfaces.Members;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class TeamPreset implements ConfigurationSerializable {

	private final Map<String, TeamScheme> schemes;
	private final String name;
	private DivisionType divisionType;

	public TeamPreset(String name, DivisionType divisionType) {
		this.schemes = new HashMap<>();
		this.name = name;
		this.divisionType = Preconditions.checkNotNull(divisionType);
	}

	public TeamPreset(String name, DivisionType divisionType, TeamScheme... schemes) {
		this.schemes = new HashMap<>();
		this.name = name;
		this.divisionType = Preconditions.checkNotNull(divisionType);
		for (TeamScheme scheme : schemes) {
			addScheme(scheme);
		}
	}

	public TeamPreset(Map<String, Object> args) {
		final Map<String, TeamScheme> schemeList = new HashMap<>();
		for (Object o : (List<?>) args.get("schemes")) {
			if (o instanceof TeamScheme) {
				TeamScheme scheme = (TeamScheme) o;
				schemeList.put(scheme.getName(), scheme);
			}
		}
		this.schemes = schemeList;
		this.name = args.get("name").toString();
		this.divisionType = Enums.getIfPresent(DivisionType.class, args.get("divisionType").toString()).or(DivisionType.EQUAL);
	}

	public String getName() {
		return name;
	}

	public DivisionType getDivisionType() {
		return divisionType;
	}

	public void setDivisionType(DivisionType divisionType) {
		this.divisionType = divisionType;
	}

	public Collection<TeamScheme> getSchemes() {
		return schemes.values();
	}

	public boolean addScheme(TeamScheme scheme) {
		if (schemes.get(scheme.getName()) == null) {
			schemes.put(scheme.getName(), scheme);
			return true;
		} else return false;
	}

	public boolean hasScheme(String name) {
		return schemes.containsKey(name);
	}

	public void removeScheme(String name) {
		schemes.remove(name);
	}

	public TeamScheme getScheme(String name) {
		return schemes.get(name);
	}

	public boolean isValid() {
		return schemes.size() > 0;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);
		map.put("divisionType", divisionType.name());
		map.put("schemes", new ArrayList<>(schemes.values()));
		return map;
	}

	public enum DivisionType {

		EQUAL("모든 팀 동일", "모든 팀에 같은 수의 플레이어가 있도록 팀원을 분배합니다.", "약 1명 정도 차이날 수 있습니다.") {
			public void divide(Teamable game, TeamPreset preset) {
				final LinkedList<Participant> participants = new LinkedList<>(game.getParticipants());
				Collections.shuffle(participants);
				final List<TeamScheme> schemes = new ArrayList<>(preset.getSchemes());
				for (int i = 0, remain = game.getParticipants().size(), partsLeft = schemes.size(); partsLeft > 0; i++, partsLeft--) {
					int size = (remain + partsLeft - 1) / partsLeft;
					final TeamScheme scheme = schemes.get(i);
					final Members team = game.newTeam(scheme.getName(), scheme.getDisplayName());
					team.setSpawn(scheme.getSpawn());
					for (int j = 0; j < size && participants.size() > 0; j++) {
						game.setTeam(participants.removeFirst(), team);
					}
					remain -= size;
				}
			}

			public DivisionType next() {
				return FIXED;
			}
		},
		FIXED("P4GE 진행", "P4GE 서버에서 팀 분배 정보를 받아옵니다") {
			@Override
			public void divide(Teamable game, TeamPreset preset) {
				JsonObject map;

				try {
					URL url = new URL("https://mirror.enak.kr/meechu/page/" + preset.name + ".json");
					URLConnection conn = url.openConnection();
					conn.connect();

					BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					map = JsonParser.parseReader(reader).getAsJsonObject();
				} catch (IOException e) {
					Bukkit.getLogger().warning("플레이어 분배 정보를 받아오는 데 문제가 발생했습니다. 팀 분배는 무작위로 배정합니다.");
					e.printStackTrace(System.err);

					EQUAL.divide(game, preset);
					return;
				}

				try {
					for (String teamName : map.keySet()) {
						final TeamScheme scheme = preset.getScheme(teamName);
						if (scheme == null) {
							Bukkit.getLogger().warning(String.format("팀 %s 을 찾을 수 없습니다.", teamName));
							continue;
						}

						final Members team = game.newTeam(scheme.getName(), scheme.getDisplayName());
						team.setSpawn(scheme.getSpawn());

						boolean isLeaderPopped = false;

						for (JsonElement jsonElement : map.getAsJsonArray(teamName)) {
							String playerName = jsonElement.getAsString();
							Player player = Bukkit.getPlayer(playerName);

							if (player == null) {
								Bukkit.getLogger().warning(String.format("플레이어 %s 를 찾을 수 없어서 팀을 %s 로 설정을 못 했습니다.", playerName, teamName));
								continue;
							}

							Participant participant = game.getParticipant(player);
							if (!isLeaderPopped) {
								participant.attributes().SUPER_PLAYER.setValue(true);
								isLeaderPopped = true;
							}
							game.setTeam(participant, team);
						}
					}
				} catch (Exception e) {
					Bukkit.getLogger().warning("플레이어를 분배하는 중에 오류가 발생했습니다. 팀 분배는 무작위로 배정합니다.");
					e.printStackTrace(System.err);

					EQUAL.divide(game, preset);
					return;
				}
			}

			@Override
			public DivisionType next() {
				return EQUAL;
			}
		};

		public final String name;
		public final List<String> lore;

		DivisionType(String name, String... lore) {
			this.name = name;
			this.lore = Arrays.asList(lore);
		}

		public abstract void divide(Teamable game, TeamPreset preset);

		public abstract DivisionType next();

	}

	public static class TeamScheme implements ConfigurationSerializable {

		private final String name;
		private String displayName;
		private SpawnLocation spawn;

		public TeamScheme(String name, String displayName) {
			this.name = name;
			this.displayName = displayName;
			this.spawn = null;
		}

		public TeamScheme(Map<String, Object> args) {
			this.name = args.get("name").toString();
			this.displayName = args.get("displayName").toString();
			if (args.containsKey("spawn")) {
				this.spawn = new SpawnLocation((LinkedHashMap<?, ?>) args.get("spawn"));
			} else this.spawn = null;
		}

		public String getName() {
			return name;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		@NotNull
		public SpawnLocation getSpawn() {
			return spawn != null ? spawn : Settings.getSpawnLocation();
		}

		public void setSpawn(final @Nullable Location location) {
			this.spawn = location != null ? new SpawnLocation(location) : null;
		}

		@Override
		public Map<String, Object> serialize() {
			Map<String, Object> map = new HashMap<>();
			map.put("name", name);
			map.put("displayName", displayName);
			if (spawn != null) {
				map.put("spawn", spawn.toMap());
			}
			return map;
		}

	}

}
