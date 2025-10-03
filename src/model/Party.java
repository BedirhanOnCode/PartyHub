package model;

import com.google.gson.annotations.SerializedName;

public class Party {
    @SerializedName("party_id")
    public String partyId;
    public String title;
    @SerializedName("host_id")
    public String hostId;
    public String date; // ISO yyyy-MM-dd veya ISO datetime
    public String status; // open|closed

    @Override
    public String toString() {
        return title + " (" + status + ")";
    }
}
