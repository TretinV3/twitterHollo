package fr.tretinv3.twitterHolo;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.plugin.java.JavaPlugin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Plugin extends JavaPlugin {
    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        Bukkit.getLogger().info(ChatColor.GREEN + "Enabled " + this.getName());

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

        this.getCommand("twitter").setExecutor(new Commands(this));
        this.getCommand("tcreate").setExecutor(new Commands(this));

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            //System.out.print(this.getConfig());
            ConfigurationSection dataSection = this.getConfig().getConfigurationSection("data");

            Set<String> keysSet = dataSection.getKeys(false);
            String[] keys = new String[keysSet.size()];

            int f = 0;
            for(String x : keysSet) keys[f++] = x;

            Collection<Hologram> holograms = HologramsAPI.getHolograms(this);



            for(int i = 0; i < keys.length; i++){
                ConfigurationSection data = dataSection.getConfigurationSection(keys[i]); //.getConfigurationSection((String)dataSection.get(i).getKeys(false).toArray()[0]);
                //System.out.println(data);
                String[] posS = data.getString("pos").split(" ");

                Location loc = new Location(Bukkit.getWorld(data.getString("world")), Double.parseDouble(posS[0]), Double.parseDouble(posS[1]), Double.parseDouble(posS[2]));

                Hologram holo = null;

                for (Hologram holoTest : holograms) {
                    double deltaX = holoTest.getX()-loc.getX();
                    double deltaY = holoTest.getY()-loc.getY();
                    double deltaZ = holoTest.getZ()-loc.getZ();

                    boolean isSamePos = (deltaX < 1 && deltaX > -1) && (deltaY < 1 && deltaY > -1) && (deltaZ < 1 && deltaZ > -1);
                    if(isSamePos) {
                        holo = holoTest;
                        getLogger().info("Find Holo !");
                    }
                }

                String content = null;
                try {
                    content = makeApiRequest("/tweets/search/recent?query=" + data.getString("query") + "&expansions=author_id", this.getConfig().getString("token"));

                    Object obj = new JSONParser().parse(content);
                    JSONObject jo = (JSONObject) obj;
                    JSONArray dataTweet = (JSONArray) jo.get("data");
                    JSONObject tweet = (JSONObject) dataTweet.get(0);

                    String contentUser = makeApiRequest("/users/" + (String)tweet.get("author_id"), this.getConfig().getString("token"));

                    Object objUser = new JSONParser().parse(contentUser);
                    JSONObject joUser = (JSONObject) objUser;
                    JSONObject joData = (JSONObject)joUser.get("data");

                    String firstLine = this.getConfig().getString("hologram.firstLine").replace("{name}", data.getString("name"));
                    String contentLine = this.getConfig().getString("hologram.content").replace("{centent}", (String)tweet.get("text"));
                    String lastLine = this.getConfig().getString("hologram.lastLine").replace("{author}", (String)joData.get("username"));

                    if(holo == null || holo.isDeleted()){
                        holo = HologramsAPI.createHologram(this, loc);
                    }else{
                        holo.clearLines();
                    }

                    holo.appendTextLine(firstLine);

                    ArrayList<String> lines = getLineWithReturn(contentLine, this.getConfig().getInt("charBetweenLineJump"));
                    for (String line : lines) {
                        holo.appendTextLine(line);
                    }
                    //holo.appendTextLine(contentLine);
                    holo.appendTextLine(lastLine);


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }


            }

        }, 0L, 20*60L);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info(ChatColor.RED + "Disabled " + this.getName());
    }

    public static String makeApiRequest(String request, String bearerToken) throws IOException, URISyntaxException {

        URL url = new URL("https://api.twitter.com/2" + request);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("GET");
        http.setDoOutput(true);
        http.setRequestProperty("Authorization", "Bearer " + bearerToken);
        http.setRequestProperty("Content-Type", "application/json");

        if(http.getResponseCode() != 200){
            Bukkit.getLogger().warning("Error code : " + http.getResponseCode() + " " + http.getResponseMessage());
            return("{}");
        }

        InputStream inputStream = http.getInputStream();

        String content = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        //System.out.println(http.getResponseCode() + " " + content);

        http.disconnect();


        return content;
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static String getAuthorWithId(String id, String bearerToken) throws IOException, URISyntaxException {

        URL url = new URL("https://api.twitter.com/2/tweets?ids=" + id);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("GET");
        http.setDoOutput(true);
        http.setRequestProperty("Authorization", "Bearer " + bearerToken);
        http.setRequestProperty("Content-Type", "application/json");

        InputStream inputStream = http.getInputStream();

        String content = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        System.out.println(http.getResponseCode() + " " + content);

        http.disconnect();


        return content;
    }

    public ArrayList<String> getLineWithReturn(String textS, int max){
        char[] text = textS.toCharArray();
        int j = 0;
        String currentLine = "";
        ArrayList<String> lines = new ArrayList<>();
        for(int i = 0; i<text.length; i++){
            if(j>max && text[i]==' '){
                lines.add(currentLine);
                currentLine = "";
                j = 0;
            }else{
                j++;
                currentLine+=text[i];
            }
        }
        if(currentLine != "") lines.add(currentLine);

        return lines;
    }

}
