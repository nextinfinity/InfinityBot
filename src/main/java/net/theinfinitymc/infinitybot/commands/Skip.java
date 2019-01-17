package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.Audio;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Skip implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		Audio audio = InfinityBot.getAudio();
		if(audio.isPlaying(event.getGuild())){
			audio.skip(event.getGuild());
		}else{
			event.getChannel().sendMessage("Music is not playing!").queue();
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
