package model;

import com.google.gson.annotations.SerializedName;

public class Match {
    @SerializedName("match_id")
    public String matchId;
    @SerializedName("party_id")
    public String partyId;
    @SerializedName("game_id")
    public String gameId;
    public String date; // ISO datetime
}