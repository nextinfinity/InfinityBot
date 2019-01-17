package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Reddit implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		String[] data = msg.split(":");
		String sort = null;
		String time = null;
		Integer amount = null;
		for(String entry : data){
			if(entry.contains("=")){
				String[] subdata = entry.split("=");
				if(subdata[0].equalsIgnoreCase("sort")){
					sort = subdata[1].toUpperCase();
				}else if(subdata[0].equalsIgnoreCase("time")){
					time = subdata[1].toUpperCase();
				}else if(subdata[0].equalsIgnoreCase("amount")){
					amount = Integer.valueOf(subdata[1]);
				}
			}
		}
		if(args.length > 1){
			if(args.length == 2){
				final String[] search = {sort, time};
				final Integer count = amount;
				InfinityBot.getThreadPool().execute(new Runnable(){
					@Override
					public void run(){
						e.getChannel().sendMessage(reddit.subredditSearch(args[1].toLowerCase(), search[0], search[1], count)).queue();
					}
				});
			}else{
				int i = 3;
				String m = args[2];
				while(i < args.length){
					m = m + " " + args[i];
					i++;
				}
				final String[] search = {m, sort, time};
				final Integer count = amount;
				InfinityBot.getThreadPool().execute(new Runnable(){
					@Override
					public void run(){
						e.getChannel().sendMessage(reddit.subredditSearch(args[1].toLowerCase(), search[0], search[1], search[2], count)).queue();
					}
				});
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
