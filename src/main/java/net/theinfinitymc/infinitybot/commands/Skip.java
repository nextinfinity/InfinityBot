package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Skip implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(audio.isPlaying(e.getGuild())){
			audio.skip(e.getGuild());
		}else{
			e.getChannel().sendMessage("Music is not playing!").queue();
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
