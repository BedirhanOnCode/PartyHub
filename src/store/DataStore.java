package store;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class DataStore {
    private static final String BASE_URL = "http://localhost:3000";
    private final HttpClient httpClient;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<User> users = new ArrayList<>();
    public List<Game> games = new ArrayList<>();
    public List<Party> parties = new ArrayList<>();
    public List<GameVote> gameVotes = new ArrayList<>();
    public List<Slot> slots = new ArrayList<>();
    public List<SlotVote> slotVotes = new ArrayList<>();
    public List<Match> matches = new ArrayList<>();
    public List<Score> scores = new ArrayList<>();

    public DataStore() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void load() throws Exception {
        users = get("/users", new TypeToken<List<User>>(){}.getType());
        games = get("/games", new TypeToken<List<Game>>(){}.getType());
        parties = get("/parties", new TypeToken<List<Party>>(){}.getType());
        gameVotes = get("/game_votes", new TypeToken<List<GameVote>>(){}.getType());
        slots = get("/slots", new TypeToken<List<Slot>>(){}.getType());
        slotVotes = get("/slot_votes", new TypeToken<List<SlotVote>>(){}.getType());
        matches = get("/matches", new TypeToken<List<Match>>(){}.getType());
        scores = get("/scores", new TypeToken<List<Score>>(){}.getType());
    }

    private <T> List<T> get(String endpoint, Type type) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return gson.fromJson(response.body(), type);
    }

    public synchronized void save() throws Exception {
        // JSON Server otomatik olarak kaydeder, bu metod artık kullanılmıyor
        // Uyumluluk için boş bırakıyoruz
    }

    // Yeni CRUD metodları
    public <T> T post(String endpoint, T item) throws Exception {
        String json = gson.toJson(item);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return gson.fromJson(response.body(), (Class<T>) item.getClass());
    }

    public <T> T put(String endpoint, T item) throws Exception {
        String json = gson.toJson(item);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return gson.fromJson(response.body(), (Class<T>) item.getClass());
    }

    public void delete(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    // Koleksiyon işlemleri için yardımcı metodlar
    public void addParty(Party party) throws Exception {
        Party created = post("/parties", party);
        parties.removeIf(p -> p.partyId.equals(created.partyId));
        parties.add(created);
    }

    public void addGameVote(GameVote vote) throws Exception {
        // JSON Server'da ID otomatik oluşturulduğu için sadece lokal listeden temizle
        gameVotes.removeIf(gv -> gv.partyId.equals(vote.partyId) && gv.userId.equals(vote.userId));

        GameVote created = post("/game_votes", vote);
        gameVotes.add(created);
    }

    public void addSlot(Slot slot) throws Exception {
        Slot created = post("/slots", slot);
        slots.removeIf(s -> s.slotId.equals(created.slotId));
        slots.add(created);
    }

    public void addSlotVote(SlotVote vote) throws Exception {
        // JSON Server'da ID otomatik oluşturulduğu için sadece lokal listeden temizle
        slotVotes.removeIf(sv -> sv.slotId.equals(vote.slotId) && sv.userId.equals(vote.userId));

        SlotVote created = post("/slot_votes", vote);
        slotVotes.add(created);
    }

    public void addMatch(Match match) throws Exception {
        Match created = post("/matches", match);
        matches.removeIf(m -> m.matchId.equals(created.matchId));
        matches.add(created);
    }

    public void addScores(List<Score> scoreList) throws Exception {
        for (Score score : scoreList) {
            // JSON Server'da ID otomatik oluşturulduğu için sadece lokal listeden temizle
            scores.removeIf(s -> s.matchId.equals(score.matchId) && s.userId.equals(score.userId));

            Score created = post("/scores", score);
            scores.add(created);
        }
    }
}
