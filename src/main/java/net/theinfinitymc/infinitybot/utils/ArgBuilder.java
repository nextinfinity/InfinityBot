package net.theinfinitymc.infinitybot.utils;

public class ArgBuilder {

	public static String buildString(String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append(args[0]);
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				sb.append(" ");
				sb.append(args[i]);
			}
		}
		return sb.toString();
	}

}
