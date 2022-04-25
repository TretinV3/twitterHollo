package fr.tretinv3.twitterHolo;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

public class Commands implements CommandExecutor {

    Plugin parent;

    public Commands(Plugin parent){
        this.parent = parent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName() == "twitter") {
            try {
                String content = parent.makeApiRequest("/tweets/search/recent?query=" + args[0] + "&expansions=author_id", parent.getConfig().getString("token"));

                Object obj = new JSONParser().parse(content);
                JSONObject jo = (JSONObject) obj;
                JSONArray data = (JSONArray) jo.get("data");
                JSONObject tweet = (JSONObject) data.get(0);

                sender.sendMessage(tweet.toJSONString());
                //sender.sendMessage(content);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return false;
        }else if(command.getName() == "tcreate"){
            if(sender instanceof Player){
                Player player = (Player)sender;

                Location pos = player.getLocation();
                Hologram hologram = HologramsAPI.createHologram(parent, pos);
                TextLine textLine = hologram.appendTextLine("A hologram line");

            }else{
                sender.sendMessage("merci d'etre un joueur pour faire cela");
            }
        }else if(command.getName() == "tclear"){
            Collection<Hologram> holograms = HologramsAPI.getHolograms(parent);
            for(Hologram holo: holograms){
                holo.delete();
            }
            sender.sendMessage("done !");
        }
        return false;
    }



}
