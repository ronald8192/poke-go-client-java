package com.ronald8192.pokemon;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.NoSuchItemException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;

import static com.ronald8192.pokemon.EDownloadStatus.*;

public class PokeFetcher {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private DecimalFormat df = new DecimalFormat("####.##");
	private DecimalFormat dfPercent = new DecimalFormat("#.####");

	private OkHttpClient httpClient = new OkHttpClient();
	private GoogleUserCredentialProvider provider = null;

	private boolean loggedIn = false;
	private PokemonGo pokemonGo = null;
	private String playerName = null;

	private String refreshToken = null;
	private String configDir = System.getProperty("user.home") + File.separator + ".poke-go-client" + File.separator;
	private String diskRefreshTokenName =  "config.json";

	public PokeFetcher() throws LoginFailedException, RemoteServerException {
		refreshToken = getRefreshTokenFromDisk();
		if(refreshToken != null){
			loggedIn = true;
			provider = new GoogleUserCredentialProvider(httpClient, refreshToken);
		}
		log.trace("logged in:" + isLoggedIn());
	}

	/**
	 * Get google oauth login url
	 * @return Google oauth login url
	 * @throws LoginFailedException
	 * @throws RemoteServerException
     */
	public String getLoginToken() throws LoginFailedException, RemoteServerException {
		provider = new GoogleUserCredentialProvider(httpClient);
		log.info("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
		return GoogleUserCredentialProvider.LOGIN_URL;
	}

	/**
	 * oauth login, save refresh token
	 * @param access
	 * @throws Exception
     */
	public void login(String access) throws LoginFailedException, RemoteServerException {
		try {
			provider.login(access);
			refreshToken = provider.getRefreshToken();  //refresh token for next auth
			try {
				getPlayerName();
			} catch (RemoteServerException | LoginFailedException e) {
				log.error("Error getting player name.");
				log.error(e.getMessage());
			}
			saveRefreshTokenToDisk();
		} catch (LoginFailedException | RemoteServerException e) {
			loggedIn = false;
			throw e;
		}
		loggedIn = true;
	}

	public void logout(){
		provider = null;
		playerName = null;
		refreshToken = null;
		loggedIn = false;
		File configFile = new File(configDir + diskRefreshTokenName);
		configFile.delete();
	}

	public String getRefreshTokenFromDisk(){
		return getRefreshTokenFromDisk(configDir + diskRefreshTokenName);
	}

	public String getRefreshTokenFromDisk(String filePath){
		String refreshToken = null;
		File config = new File(filePath);
		File configDir = new File(config.getParent());

		if(configDir.exists()){
			try (BufferedReader br = new BufferedReader(new FileReader(filePath));){
				String json = br.readLine();
				if(json != null){
					JSONObject o = new JSONObject(json);
					refreshToken = o.getString("rt");
				}
			} catch (IOException e) {
				if (config.exists()){
					log.error("Error reading config file.");
					log.error(e.getMessage());
				}else{
					log.trace("config file not exsit.");
				}
			}
		}else{
			//first time
			configDir.mkdir();
		}

		return refreshToken;
	}

	public void saveRefreshTokenToDisk(){
		try (BufferedWriter br = new BufferedWriter(new FileWriter(configDir + diskRefreshTokenName));){
			JSONObject o = new JSONObject();
			o.put("comment", "please keep this file safe.");
			if(playerName != null){
				o.put("player", playerName);
			}
			if(refreshToken != null){
				o.put("rt", refreshToken);
			}
			br.write(o.toString());
		} catch (IOException e) {
			log.error("Write refresh token to disk error.");
			log.error(e.getMessage());
		}
	}

	private void apiInit() throws LoginFailedException, RemoteServerException {
		log.trace("provider.isTokenIdExpired(): " + provider.isTokenIdExpired());
//		if(provider.isTokenIdExpired() || pokemonGo == null){
//			pokemonGo = new PokemonGo(new GoogleUserCredentialProvider(httpClient, refreshToken), httpClient);
//		}else{
			pokemonGo = new PokemonGo(provider, httpClient);
//		}
	}

	public EDownloadStatus download()  {
		try{
			apiInit();
		}catch(LoginFailedException | RemoteServerException e){
			return AUTH_ERROR;
		}
		// After this you can access the api from the PokemonGo instance :
		try {

			// PokeBank pokeBank = go.getInventories().getPokebank();
			// to get all his inventories (Pokemon, backpack, egg, incubator)
			
			JSONObject pokeDetails = new JSONObject();

			JSONObject player = new JSONObject();
			player.put("name", playerName);
			pokeDetails.put("player", player);
			
			List<Pokemon> pokemons = pokemonGo.getInventories().getPokebank().getPokemons();
			JSONArray myPokemons = new JSONArray();
			for (Pokemon pokemon : pokemons) {
				JSONObject p = new JSONObject();
				p.put("poke_id", pokemon.getMeta().getNumber());
				p.put("nickname", (pokemon.getNickname()));
				p.put("lv", pokemon.getLevel());
				p.put("cp", pokemon.getCp());
				p.put("move_1", humanize(pokemon.getMove1().toString()));
				p.put("move_2", humanize(pokemon.getMove2().toString()));
				p.put("iv", Double.parseDouble(dfPercent.format(pokemon.getIvRatio())));
				p.put("iv_a", pokemon.getIndividualAttack());
				p.put("iv_d", pokemon.getIndividualDefense());
				p.put("iv_s", pokemon.getIndividualStamina());
				p.put("height", Double.parseDouble(df.format(pokemon.getHeightM())));
				p.put("weight", Double.parseDouble(df.format(pokemon.getWeightKg())));
				p.put("fav", pokemon.isFavorite());
				p.put("created_at", pokemon.getCreationTimeMs());
				try {
					p.put("max_cp",pokemon.getMaxCp());
				} catch (JSONException | NoSuchItemException e1) {
					p.put("max_cp","");
					e1.printStackTrace();
				}
				try {
					p.put("max_cp_fp", pokemon.getMaxCpForPlayer());
				} catch (JSONException | NoSuchItemException e) {
					p.put("max_cp_fp", "");
					e.printStackTrace();
				}
				p.put("max_cp_fepufp",pokemon.getMaxCpFullEvolveAndPowerupForPlayer());
				myPokemons.put(p);
			}
			pokeDetails.put("pokemons", myPokemons);

			try (BufferedWriter writer = new BufferedWriter(new PrintWriter(getJarDir() + "/myPokemon.json"));) {
				writer.write(pokeDetails.toString());
				log.info("Your Pokemon data has saved to 'myPokemon.json', please copy the content to https://ronald8192.github.io/pokemon-go-detail-visualizer/");
			} catch (IOException e) {
				log.error("Save file error.");
				log.info(myPokemons.toString());
				return SAVE_TO_DISK_ERROR;
			}

			// System.out.println();
			// System.out.println("Currencies:");
			// Map currencies = go.getPlayerProfile().getCurrencies();
			// for (Object o:currencies.keySet()) {
			// PlayerProfile.Currency c = (PlayerProfile.Currency)o;
			// System.out.println(c.name() + " : " + currencies.get(o));
			// }

		} catch (RemoteServerException | NullPointerException e) {
			log.error(e.getMessage());
			return NETOWRK_ERROR;
		}catch (LoginFailedException e){
			log.error(e.getMessage());
			return AUTH_ERROR;
		}
		return SUCCESS;
	}

	public String getPlayerName() throws LoginFailedException, RemoteServerException {
		apiInit();
		if(playerName == null){
			PlayerProfile playerProfile= pokemonGo.getPlayerProfile();
			playerName = playerProfile.getPlayerData().getUsername();
		}
		return playerName;
	}

	public boolean isLoggedIn(){
		return this.loggedIn;
	}
	
	private static String humanize(String s) {
		if (s.length() == 0)
			return s;

		String[] words = s.split("_");
		s = "";
		for (int i = 0; i < words.length; i++) {
			if (words[i].equals("FAST")) {
				s += "";
			} else {
				s += (words[i].charAt(0) + "").toUpperCase() + words[i].substring(1).toLowerCase();
			}
			if (i + 1 != words.length)
				s += " ";
		}
		return s.trim();
	}
	
	/**
	 * Get the jar file(program itself) directory
	 * @return jar directory
	 */
	private static String getJarDir() {
		URL url = PokeFetcher.class.getProtectionDomain().getCodeSource().getLocation(); //Gets the path
	  	String jarPath = null;
		try {
			jarPath = URLDecoder.decode(url.getFile(), "UTF-8"); //Should fix it to be read correctly by the system
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
			
		//Path of the jar	
	    return new File(jarPath).getParentFile().getPath();
	}

}
