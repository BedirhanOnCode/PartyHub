package model;

import com.google.gson.annotations.SerializedName;

public class Slot {
    @SerializedName("slot_id")
    public String slotId;
    @SerializedName("party_id")
    public String partyId;
    public String start; // ISO datetime
    public String end;   // ISO datetime

    @Override
    public String toString() {
        return start + " → " + end;
    }
}