package model;

import com.google.gson.annotations.SerializedName;

public class Game {
    @SerializedName("game_id")
    public String gameId;
    public String title;
    public String genre;
    @SerializedName("max_players")
    public int maxPlayers;

    @Override
    public String toString() {
        return title;
    }
}
