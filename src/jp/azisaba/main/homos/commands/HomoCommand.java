package jp.azisaba.main.homos.commands;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import jp.azisaba.main.homos.Homos;
import jp.azisaba.main.homos.JSONMessage;
import jp.azisaba.main.homos.classes.PlayerData;
import jp.azisaba.main.homos.database.SQLDataManager;
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

		if (args[0].equalsIgnoreCase("ticketvalue")) {

			if (args.length <= 1) {
				getTicketValueHelpMSG(label).send(p);
				return;
			}

			if (args[1].equalsIgnoreCase("view")) {
				p.sendMessage(ChatColor.YELLOW + "現在のチケット1枚当たりの値段: " + ChatColor.GREEN + ""
						+ Homos.getMedianManager().getCurrentMedian());
				return;
			}

			if (args[1].equalsIgnoreCase("lock")) {

				int value = 0;

				if (args.length <= 2) {
					value = Homos.getMedianManager().getCurrentMedian();
				} else {
					try {
						value = Integer.parseInt(args[2]);
					} catch (Exception e) {
						p.sendMessage(ChatColor.RED + "整数を入力してください。");
						return;
					}
				}

				if (value <= 0) {
					p.sendMessage(ChatColor.RED + "値は0より大きい必要があります。");
					return;
				}

				Homos.getMedianManager().updateMedian(value, true);
				Homos.getMedianManager().lock(true);

				p.sendMessage(ChatColor.GREEN + "" + value + ChatColor.LIGHT_PURPLE + "に固定しました。");
				return;
			}

			if (args[1].equalsIgnoreCase("unlock")) {
				if (!Homos.getMedianManager().isLocked()) {
					p.sendMessage(ChatColor.RED + "中央値は現在固定されていません。");
					return;
				}

				Homos.getMedianManager().lock(false);
				Homos.getMedianManager().updateMedian();

				p.sendMessage(ChatColor.GREEN + "固定を解除しました。現在の値は" + ChatColor.LIGHT_PURPLE
						+ Homos.getMedianManager().getCurrentMedian() + ChatColor.GREEN + "です。");
				return;
			}

			getTicketValueHelpMSG(label).send(p);

			return;
		}

		if (args[0].equalsIgnoreCase("user")) {

			if (args.length <= 2) {
				getUserHelpMSG(label).send(p);
				return;
			}

			String user = args[1];
			PlayerData data = SQLDataManager.getPlayerData(user);
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

				int num = 0;

				try {
					num = Integer.parseInt(args[3]);
				} catch (Exception e) {
					p.sendMessage(ChatColor.RED + "正しい数値を入力してください。");
					return;
				}

				if (num < 0) {
					p.sendMessage(ChatColor.RED + "数値は0より大きい必要があります。");
					return;
				}

				SQLDataManager.addTicket(data.getUuid(), num);

				p.sendMessage(ChatColor.GREEN + "チケットを" + ChatColor.YELLOW + (data.getTickets() + num) + ChatColor.GREEN
						+ "枚に変更しました。");
				return;
			}

			if (args[2].equalsIgnoreCase("remove")) {

				if (args.length <= 3) {
					p.sendMessage(ChatColor.RED + "/" + label + " user <user> remove <amount>");
					return;
				}

				int num = 0;

				try {
					num = Integer.parseInt(args[3]);
				} catch (Exception e) {
					p.sendMessage(ChatColor.RED + "正しい数値を入力してください。");
					return;
				}

				if (num < 0) {
					p.sendMessage(ChatColor.RED + "数値は0より大きい必要があります。");
					return;
				}

				if (data.getTickets() < num) {
					p.sendMessage(ChatColor.RED + "数値はプレイヤーの現在所持しているチケット枚数より少ないか、同じである必要があります。");
					return;
				}

				SQLDataManager.removeTicket(data.getUuid(), num);

				p.sendMessage(ChatColor.GREEN + "チケットを" + ChatColor.YELLOW + (data.getTickets() - num) + ChatColor.GREEN
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

		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " median" + ChatColor.GRAY + " - 中央値の設定、表示")
				.runCommand("/" + label + " median").newline();
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
				+ " - 指定した値で固定 (未指定で現在の値)").suggestCommand("/" + label + " ticketvalue lock ").newline();
		msg.then(ChatColor.YELLOW + " » " + ChatColor.RED + "/" + label + " ticketvalue unlock" + ChatColor.GRAY
				+ " - 値の固定を解除").runCommand("/" + label + " ticketvalue unlock").newline();
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
