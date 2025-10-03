package model;

import com.google.gson.annotations.SerializedName;

public class GameVote {
    @SerializedName("party_id")
    public String partyId;
    @SerializedName("game_id")
    public String gameId;
    @SerializedName("user_id")
    public String userId;
}
