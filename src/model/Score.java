package model;

import com.google.gson.annotations.SerializedName;

public class Score {
    @SerializedName("match_id")
    public String matchId;
    @SerializedName("user_id")
    public String userId;
    public int score;
    public String result; // win|lose|draw
}
