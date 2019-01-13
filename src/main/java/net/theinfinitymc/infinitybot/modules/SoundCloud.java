package net.theinfinitymc.infinitybot.modules;

import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONArray;

public class SoundCloud {

    public String search(String query){
        try{
            query = query.replaceAll(" ", "%20");
            JSONArray results = JsonHandler.readJsonArrayFromUrl("https://api.soundcloud.com/track?client_id=35e00568851d1294e9816df13a80b987&q=" + query);
            return results.getJSONObject(0).getString("permalink_url");
        }catch(Exception e){
            e.printStackTrace();
            return "Error retrieving search results!";
        }
    }

}
