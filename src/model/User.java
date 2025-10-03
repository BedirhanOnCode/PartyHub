package model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("user_id")
    public String userId;
    public String name;
    @SerializedName("gamer_tag")
    public String gamerTag;
    public String avatar;
}