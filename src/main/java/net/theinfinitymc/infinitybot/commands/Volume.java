package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Volume implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length == 2){
			Integer vol = Integer.parseInt(args[1]);
			if(vol > 1000){
				vol = 1000;
			}
			if(vol < 0){
				vol = 0;
			}
			InfinityBot.getAudio().setVolume(vol, event.getGuild());
			event.getChannel().sendMessage("Volume set to " + vol + "!").queue();
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
