package service;

import model.*;
import store.DataStore;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PartyService {
    private final DataStore db;
    private final Random rnd = new Random();

    public PartyService(DataStore db) {
        this.db = db;
    }

    private String newId(String prefix) {
        return prefix + Math.abs(rnd.nextInt(1_000_000));
    }

    public Party createParty(String title, String dateIso, String hostId) throws Exception {
        Party p = new Party();
        p.partyId = newId("P");
        p.title = title;
        p.date = dateIso;
        p.hostId = hostId;
        p.status = "open";
        db.addParty(p);
        return p;
    }

    public void addGamesToParty(String partyId, List<String> gameIds) {
        // Basit MVP: aday listesi kalıcı tutulmuyor; oy ekranında tüm oyunlar gösterilir.
        // İstenirse ayrı bir "party_games" koleksiyonu eklenebilir.
    }

    public void voteGame(String partyId, String userId, String gameId) throws Exception {
        GameVote v = new GameVote();
        v.partyId = partyId;
        v.userId = userId;
        v.gameId = gameId;
        db.addGameVote(v);
    }

    public Slot addSlot(String partyId, String startIso, String endIso) throws Exception {
        Slot s = new Slot();
        s.slotId = newId("S");
        s.partyId = partyId;
        s.start = startIso;
        s.end = endIso;
        db.addSlot(s);
        return s;
    }

    public void voteSlot(String slotId, String userId, boolean yes) throws Exception {
        SlotVote v = new SlotVote();
        v.slotId = slotId;
        v.userId = userId;
        v.choice = yes ? "yes" : "no";
        db.addSlotVote(v);
    }

    public Match createMatch(String partyId, String gameId, String dateIso) throws Exception {
        Match m = new Match();
        m.matchId = newId("M");
        m.partyId = partyId;
        m.gameId = gameId;
        m.date = dateIso;
        db.addMatch(m);
        return m;
    }

    public void postScores(String matchId, List<Score> results) throws Exception {
        db.addScores(results);
    }

    public Map<String, Long> gameVoteCounts(String partyId) {
        return db.gameVotes.stream()
                .filter(v -> v.partyId.equals(partyId))
                .collect(Collectors.groupingBy(v -> v.gameId, Collectors.counting()));
    }

    public Optional<String> selectedGameByVotes(String partyId) {
        return gameVoteCounts(partyId).entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Map<String, Long> slotYesCounts(String partyId) {
        Set<String> partySlotIds = db.slots.stream().filter(s -> s.partyId.equals(partyId))
                .map(s -> s.slotId).collect(Collectors.toSet());
        return db.slotVotes.stream()
                .filter(v -> partySlotIds.contains(v.slotId) && "yes".equals(v.choice))
                .collect(Collectors.groupingBy(v -> v.slotId, Collectors.counting()));
    }

    public Optional<String> suggestedSlotId(String partyId) {
        return slotYesCounts(partyId).entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Map<String, Integer> leaderboard(String partyId) {
        // Parti altındaki tüm maçlardaki puanları topla
        Set<String> partyMatchIds = db.matches.stream()
                .filter(m -> m.partyId.equals(partyId))
                .map(m -> m.matchId).collect(Collectors.toSet());
        Map<String, Integer> lb = new HashMap<>();
        for (Score s : db.scores) {
            if (!partyMatchIds.contains(s.matchId)) continue;
            int pts = switch (s.result) {
                case "win" -> 3;
                case "draw" -> 1;
                default -> 0;
            };
            lb.put(s.userId, lb.getOrDefault(s.userId, 0) + pts);
        }
        return lb;
    }

    public Summary summary(String partyId) {
        Summary sum = new Summary();
        sum.selectedGameId = selectedGameByVotes(partyId).orElse(null);
        sum.finalSlotId = suggestedSlotId(partyId).orElse(null);
        sum.leaderboard = leaderboard(partyId).entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();
        sum.totalMatches = (int) db.matches.stream().filter(m -> m.partyId.equals(partyId)).count();
        return sum;
    }

    public String nowIso() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static class Summary {
        public String selectedGameId;
        public String finalSlotId;
        public List<Map.Entry<String,Integer>> leaderboard;
        public int totalMatches;
    }
}
