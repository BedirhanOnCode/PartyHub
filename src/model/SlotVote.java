package model;

import com.google.gson.annotations.SerializedName;

public class SlotVote {
    @SerializedName("slot_id")
    public String slotId;
    @SerializedName("user_id")
    public String userId;
    public String choice; // yes|no
}
