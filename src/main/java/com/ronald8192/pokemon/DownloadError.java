package com.ronald8192.pokemon;

public enum DownloadError {
	NO_ERROR("Download complete. Your Pokemon data has saved to 'myPokemon.json'."),
	SAVE_TO_DISK_ERROR("Save Pokemon data to disk failed, make sure you have permission."),
	NETOWRK_ERROR("Cannot download from remote server, please try again later."),
	AUTH_ERROR("Authentication error.")
	;

	String msg;
	DownloadError(String msg){
		this.msg = msg;
	}
	String say(){
		return this.msg;
	}
}
