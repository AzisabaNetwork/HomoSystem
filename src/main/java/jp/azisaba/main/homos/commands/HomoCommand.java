package jp.azisaba.main.homos.commands;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.JSONMessage;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.PlayerDataManager;
import jp.azisaba.main.homos.database.TicketManager;
import jp.azisaba.main.homos.ticketvalue.TicketValueUtils;
import net.md_5.bungee.api.ChatColor;

public class HomoCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player) {
			executeForPlayer((Player) sender, cmd, label, args);
		} else {
			executeForConsole(sender, cmd, label, args);
		}
		return true;
	}

	private void executeForPlayer(Player p, Command cmd, String label, String[] args) {

		if (args.length <= 0) {
			getMainHelpMSG(label).send(p);
			return;
		}

		if (args[0].equalsIgnoreCase("reload")) {
			Homos.reloadPlugin();
			p.sendMessage(ChatColor.GREEN + "リロードしました。");
			return;
		}

		if (args[0].equalsIgnoreCase("median")) {

			new Thread() {
				Player player = p;

				public void run() {

					if (player != null)
						player.sendMessage(ChatColor.GREEN + "取得しています...");

					Entry<String, BigDecimal> entry = TicketValueUtils.getMedianPlayer();

					if (entry == null)
						if (player != null) {
							player.sendMessage(ChatColor.RED + "取得に失敗しました。");
						}

					if (player != null) {
						player.sendMessage(ChatColor.GREEN + "中央値のプレイヤー: ");
						player.sendMessage(ChatColor.YELLOW + "Player" + ChatColor.GREEN + ": " + ChatColor.RED
								+ entry.getKey().replace(",", ChatColor.GRAY + "," + ChatColor.RED));
						player.sendMessage(ChatColor.YELLOW + "Value" + ChatColor.GREEN + ": " + ChatColor.RED
								+ entry.getValue().toString());
					}
				}
			}.start();
			return;
		}

		if (args[0].equalsIgnoreCase("ticketvalue")) {

			if (args.length <= 1) {
				getTicketValueHelpMSG(label).send(p);
				return;
			}

			if (args[1].equalsIgnoreCase("view")) {
				p.sendMessage(ChatColor.YELLOW + "現在のチケット1枚当たりの値段: " + ChatColor.GREEN + ""
						+ Homos.getTicketValueManager().getCurrentTicketValue());
				return;
			}

			if (args[1].equalsIgnoreCase("lock")) {

				BigInteger value = BigInteger.ZERO;

				if (args.length <= 2) {
					value = Homos.getTicketValueManager().getCurrentTicketValue();
				} else {
					try {
						value = new BigInteger(args[2]);
					} catch (Exception e) {
						p.sendMessage(ChatColor.RED + "整数を入力してください。");
						return;
					}
				}

				if (value.compareTo(BigInteger.ZERO) <= 0) {
					p.sendMessage(ChatColor.RED + "値は0より大きい必要があります。");
					return;
				}

				Homos.getTicketValueManager().updateTicketValue(value, true);
				Homos.getTicketValueManager().lock(true);

				p.sendMessage(ChatColor.GREEN + "" + value + ChatColor.LIGHT_PURPLE + "に固定しました。");
				return;
			}

			if (args[1].equalsIgnoreCase("unlock")) {
				if (!Homos.getTicketValueManager().isLocked()) {
					p.sendMessage(ChatColor.RED + "中央値は現在固定されていません。");
					return;
				}

				Homos.getTicketValueManager().lock(false);
				Homos.getTicketValueManager().updateTicketValue();

				p.sendMessage(ChatColor.GREEN + "固定を解除しました。現在の値は" + ChatColor.LIGHT_PURPLE
						+ Homos.getTicketValueManager().getCurrentTicketValue() + ChatColor.GREEN + "です。");
				return;
			}

			if (args[1].equalsIgnoreCase("update")) {

				new Thread() {
					@Override
					public void run() {
						p.sendMessage(ChatColor.GREEN + "更新しています...");
						Homos.getTicketValueManager().updateTicketValue();
						p.sendMessage(ChatColor.GREEN + "完了！ 現在のチケット価格は " + ChatColor.RED
								+ Homos.getTicketValueManager().getCurrentTicketValue()
								+ ChatColor.GREEN + " です。");
					}
				}.start();

				return;
			}

			getTicketValueHelpMSG(label).send(p);

			return;
		}

		//		if (args[0].equalsIgnoreCase("importAll")) {
		//
		//			new Thread() {
		//				private Player player = p;
		//
		//				public void run() {
		//					Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
		//
		//					UserMap map = ess.getUserMap();
		//					Set<UUID> uuids = map.getAllUniqueUsers();
		//
		//					int size = uuids.size();
		//					int i = 0;
		//
		//					for (UUID uuid : uuids) {
		//						User user = map.getUser(uuid);
		//
		//						String name = user.getName();
		//						long lastJoin = user.getLastLogin();
		//
		//						PlayerDataManager.updatePlayerData(uuid, name, lastJoin);
		//						i++;
		//
		//						if (player != null) {
		//							JSONMessage.create(ChatColor.GREEN
		//									+ String.format("%.2f", (((double) i / (double) size) * 100d)) + "% done.")
		//									.actionbar(player);
		//						}
		//					}
		//
		//					if (player != null) {
		//						JSONMessage.create("完了！").actionbar(player);
		//						player.sendMessage(ChatColor.GREEN + "完了！");
		//					}
		//				}
		//			}.start();
		//			return;
		//		}

		if (args[0].equalsIgnoreCase("user")) {

			if (args.length <= 2) {
				getUserHelpMSG(label).send(p);
				return;
			}

			String user = args[1];
			PlayerData data = PlayerDataManager.getPlayerData(user);
			if (data.getUuid() == null) {
				p.sendMessage(ChatColor.RED + "プレイヤーが見つかりませんでした。");
				return;
			}

			if (args[2].equalsIgnoreCase("info")) {
				StringBuilder builder = new StringBuilder(
						ChatColor.GREEN + data.getName() + ChatColor.LIGHT_PURPLE + "のユーザー情報 \n");
				builder.append(ChatColor.RED + "Name: " + ChatColor.GREEN + data.getName() + "\n");
				builder.append(ChatColor.YELLOW + "Tickets: " + ChatColor.GREEN + data.getTickets() + "\n");
				builder.append(ChatColor.GRAY + "LastJoin: " + data.getLastJoin());

				p.sendMessage(builder.toString());
				return;
			}

			if (args[2].equalsIgnoreCase("add")) {

				if (args.length <= 3) {
					p.sendMessage(ChatColor.RED + "/" + label + " user <user> add <amount>");
					return;
				}

				BigInteger num = BigInteger.ZERO;

				try {
					int intNum = Integer.parseInt(args[3]);
					num = BigInteger.valueOf(intNum);
				} catch (Exception e) {
					p.sendMessage(ChatColor.RED + "正しい数値を入力してください。");
					return;
				}

				if (num.compareTo(BigInteger.ZERO) < 0) {
					p.sendMessage(ChatColor.RED + "数値は0より大きい必要があります。");
					return;
				}

				TicketManager.addTicket(data.getUuid(), num);

				p.sendMessage(ChatColor.GREEN + "チケットを" + ChatColor.YELLOW + data.getTickets().add(num).toString()
						+ ChatColor.GREEN + "枚に変更しました。");
				return;
			}

			if (args[2].equalsIgnoreCase("remove")) {

				if (args.length <= 3) {
					p.sendMessage(ChatColor.RED + "/" + label + " user <user> remove <amount>");
					return;
				}

				BigInteger num = BigInteger.ZERO;

				try {
					int intNum = Integer.parseInt(args[3]);
					num = BigInteger.valueOf(intNum);
				} catch (Exception e) {
					p.sendMessage(ChatColor.RED + "正しい数値を入力してください。");
					return;
				}

				if (num.compareTo(BigInteger.ZERO) < 0) {
					p.sendMessage(ChatColor.RED + "数値は0より大きい必要があります。");
					return;
				}

				if (data.getTickets().compareTo(num) < 0) {
					p.sendMessage(ChatColor.RED + "数値はプレイヤーの現在所持しているチケット枚数より少ないか、同じである必要があります。");
					return;
				}

				TicketManager.removeTicket(data.getUuid(), num);

				p.sendMessage(ChatColor.GREEN + "チケットを" + ChatColor.YELLOW + data.getTickets().subtract(num).toString()
						+ ChatColor.GREEN
						+ "枚に変更しました。");
				return;
			}

			getUserHelpMSG(label).send(p);
			return;
		}
	}

	private void executeForConsole(CommandSender sender, Command cmd, String label, String[] args) {

	}

	private JSONMessage getMainHelpMSG(String label) {
		JSONMessage msg = JSONMessage
				.create(ChatColor.LIGHT_PURPLE + StringUtils.repeat("-", 15) + ChatColor.GREEN + "[HOMOs]"
						+ ChatColor.LIGHT_PURPLE
						+ StringUtils.repeat("-", 15));
		msg.newline();

		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " ticketvalue" + ChatColor.GRAY
				+ " - 中央値の設定、表示")
				.runCommand("/" + label + " ticketvalue").newline();
		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " user [user]" + ChatColor.GRAY
				+ " - ユーザーデータの表示")
				.suggestCommand("/" + label + " user ").newline();
		msg.then(ChatColor.LIGHT_PURPLE + StringUtils.repeat("-", 37));
		return msg;
	}

	private JSONMessage getTicketValueHelpMSG(String label) {
		JSONMessage msg = JSONMessage
				.create(ChatColor.LIGHT_PURPLE + StringUtils.repeat("-", 15) + ChatColor.GREEN + "[HOMOs]"
						+ ChatColor.LIGHT_PURPLE
						+ StringUtils.repeat("-", 15));
		msg.newline();

		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " ticketvalue view" + ChatColor.GRAY
				+ " - 現在の中央値の表示").runCommand("/" + label + " ticketvalue view").newline();
		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " ticketvalue lock [value]" + ChatColor.GRAY
				+ " - 指定した値で固定 " + ChatColor.GRAY + "(未指定で現在の値)").suggestCommand("/" + label + " ticketvalue lock ")
				.newline();
		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " ticketvalue unlock" + ChatColor.GRAY
				+ " - 値の固定を解除").runCommand("/" + label + " ticketvalue unlock").newline();
		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " ticketvalue update" + ChatColor.GRAY
				+ " - 価格を更新").runCommand("/" + label + " ticketvalue update").newline();
		msg.then(ChatColor.LIGHT_PURPLE + StringUtils.repeat("-", 37));
		return msg;
	}

	private JSONMessage getUserHelpMSG(String label) {
		JSONMessage msg = JSONMessage
				.create(ChatColor.LIGHT_PURPLE + StringUtils.repeat("-", 15) + ChatColor.GREEN + "[HOMOs]"
						+ ChatColor.LIGHT_PURPLE
						+ StringUtils.repeat("-", 15));
		msg.newline();

		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " user <user> info" + ChatColor.GRAY
				+ " - ユーザーの詳細の表示").suggestCommand("/" + label + " user <user> info").newline();
		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " user <user> add <amount>" + ChatColor.GRAY
				+ " - チケットを追加").suggestCommand("/" + label + " user <user> add <amount>").newline();
		msg.then(
				ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " user <user> remove <amount>" + ChatColor.GRAY
						+ " - チケットを剥奪")
				.suggestCommand("/" + label + " user <user> remove <amount>").newline();
		msg.then(ChatColor.LIGHT_PURPLE + StringUtils.repeat("-", 37));
		return msg;
	}
}
