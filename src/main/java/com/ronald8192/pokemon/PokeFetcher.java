package com.ronald8192.pokemon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

public class PokeFetcher {

	Logger log = LoggerFactory.getLogger(this.getClass());

	private DecimalFormat df = new DecimalFormat("####.##");
	private DecimalFormat dfPercent = new DecimalFormat("#.####");

	private OkHttpClient httpClient = new OkHttpClient();
	private GoogleUserCredentialProvider provider = null;

	private boolean loggedIn = false;
	private PokemonGo pokemonGo;

	public String getLoginToken() {
		try {
			provider = new GoogleUserCredentialProvider(httpClient);
		} catch (LoginFailedException e) {
			e.printStackTrace();
		} catch (RemoteServerException e) {
			e.printStackTrace();
		}
		// in this url, you will get a code for the google account that is
		// logged
		log.info("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
		return GoogleUserCredentialProvider.LOGIN_URL;
	}

	public void login(String access) throws Exception {
		// we should be able to login with this token
		try {
			provider.login(access);
			pokemonGo = new PokemonGo(provider, httpClient);
		} catch (Exception e) {
			loggedIn = false;
			throw e;
		}
		
		loggedIn = true;
		
	}

	public EDownloadStatus download()  {
		// After this you can access the api from the PokemonGo instance :
		try {
			// PokeBank pokeBank = go.getInventories().getPokebank();

			// to get all his inventories (Pokemon, backpack, egg,
			// incubator)
			
			JSONObject pokeDetails = new JSONObject();
			
			PlayerProfile playerProfile= pokemonGo.getPlayerProfile();
			JSONObject player = new JSONObject();
			player.put("name", playerProfile.getPlayerData().getUsername());
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

			BufferedWriter writer = null;
			boolean hasError = false;
			try {
				
				writer = new BufferedWriter(new PrintWriter(getJarDir() + "/myPokemon.json"));
				writer.write(pokeDetails.toString());
			} catch (IOException e) {
				hasError = true;
			} finally {
				try {
					if (writer != null)
						writer.close();
				} catch (IOException e) {
				}
			}


			if (hasError) {
				log.error("Save file error.");
				log.info(myPokemons.toString());
				return EDownloadStatus.SAVE_TO_DISK_ERROR;
			} else {
				log.info("Your Pokemon data has saved to 'myPokemon.json', please copy the content to https://ronald8192.github.io/pokemon-go-detail-visualizer/");
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
			return EDownloadStatus.NETOWRK_ERROR;
		}catch (LoginFailedException e){
			log.error(e.getMessage());
			return EDownloadStatus.AUTH_ERROR;
		}
		return EDownloadStatus.SUCCESS;
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
