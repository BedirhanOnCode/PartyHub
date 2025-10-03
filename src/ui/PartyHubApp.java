package ui;

import model.*;
import service.PartyService;
import store.DataStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class PartyHubApp extends JFrame {
    private final DataStore db = new DataStore();
    private PartyService svc;

    private JComboBox<User> cboUser;
    private JList<Party> lstParties;
    private DefaultListModel<Party> partyListModel;

    // Oyun oylama
    private JComboBox<Game> cboGames;
    private JTextArea txtGameVotes;

    // Slot
    private DefaultListModel<Slot> slotListModel;
    private JList<Slot> lstSlots;
    private JLabel lblSlotSuggest;

    // Skor
    private DefaultTableModel scoreModel;
    private JTable tblScores;
    private JButton btnCreateMatchAndSave;

    // Özet
    private JTextArea txtSummary;

    public PartyHubApp() {
        setTitle("Game+ Party Hub (MVP)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        try {
            db.load();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Veri yüklenemedi: " + e.getMessage());
        }
        svc = new PartyService(db);

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);
        refreshParties();
    }

    private JPanel buildTopBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cboUser = new JComboBox<>(db.users.toArray(new User[0]));
        cboUser.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value == null ? "" : value.name));
        JButton btnNewParty = new JButton("Yeni Parti");

        btnNewParty.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(this, "Parti başlığı:");
            if (title == null || title.isBlank()) return;
            String date = JOptionPane.showInputDialog(this, "Tarih (YYYY-MM-DD):", "2025-10-16");
            if (date == null || date.isBlank()) return;
            User host = (User) cboUser.getSelectedItem();
            try {
                svc.createParty(title, date, host.userId);
                db.load(); // Verileri yeniden yükle
                refreshParties();
            } catch (Exception ex) {
                showErr(ex);
            }
        });

        p.add(new JLabel("Aktif Kullanıcı:"));
        p.add(cboUser);
        p.add(Box.createHorizontalStrut(12));
        p.add(btnNewParty);
        return p;
    }

    private JSplitPane buildMain() {
        // Sol: Parti listesi
        partyListModel = new DefaultListModel<>();
        lstParties = new JList<>(partyListModel);
        lstParties.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshGameVotes();
                refreshSlots();
                refreshScoresTable();
                refreshSummary();
            }
        });
        JScrollPane left = new JScrollPane(lstParties);

        // Sağ: Sekmeler
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Oyun Oylaması", buildGameTab());
        tabs.addTab("Slot Planlama", buildSlotTab());
        tabs.addTab("Skor", buildScoreTab());
        tabs.addTab("Özet", buildSummaryTab());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);
        split.setDividerLocation(260);
        return split;
    }

    private JPanel buildGameTab() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cboGames = new JComboBox<>(db.games.toArray(new Game[0]));
        JButton btnVote = new JButton("Oy Ver");

        btnVote.addActionListener(e -> {
            Party party = lstParties.getSelectedValue();
            if (party == null) return;
            User user = (User) cboUser.getSelectedItem();
            Game game = (Game) cboGames.getSelectedItem();
            try {
                svc.voteGame(party.partyId, user.userId, game.gameId);
                db.load(); // Verileri yeniden yükle
                refreshGameVotes();
            } catch (Exception ex) {
                showErr(ex);
            }
        });

        top.add(new JLabel("Oyun:"));
        top.add(cboGames);
        top.add(btnVote);

        txtGameVotes = new JTextArea();
        txtGameVotes.setEditable(false);

        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(txtGameVotes), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSlotTab() {
        JPanel p = new JPanel(new BorderLayout());

        // Üst: slot ekle
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtStart = new JTextField("2025-10-16T19:00:00Z", 20);
        JTextField txtEnd = new JTextField("2025-10-16T20:00:00Z", 20);
        JButton btnAddSlot = new JButton("Slot Ekle");
        btnAddSlot.addActionListener(e -> {
            Party party = lstParties.getSelectedValue();
            if (party == null) return;
            try {
                svc.addSlot(party.partyId, txtStart.getText().trim(), txtEnd.getText().trim());
                refreshSlots();
            } catch (Exception ex) {
                showErr(ex);
            }
        });
        top.add(new JLabel("Başlangıç:"));
        top.add(txtStart);
        top.add(new JLabel("Bitiş:"));
        top.add(txtEnd);
        top.add(btnAddSlot);

        // Orta: slot listesi + oy
        slotListModel = new DefaultListModel<>();
        lstSlots = new JList<>(slotListModel);

        JPanel votePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnYes = new JButton("✓ Evet");
        JButton btnNo = new JButton("× Hayır");
        btnYes.addActionListener(e -> voteCurrentSlot(true));
        btnNo.addActionListener(e -> voteCurrentSlot(false));
        votePanel.add(btnYes);
        votePanel.add(btnNo);

        // Alt: önerilen slot
        lblSlotSuggest = new JLabel("Önerilen slot: -");

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(lstSlots), BorderLayout.CENTER);
        center.add(votePanel, BorderLayout.SOUTH);

        p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        p.add(lblSlotSuggest, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildScoreTab() {
        JPanel p = new JPanel(new BorderLayout());
        scoreModel = new DefaultTableModel(new Object[]{"Kullanıcı", "Skor", "Sonuç (win/draw/lose)"}, 0);
        tblScores = new JTable(scoreModel);
        btnCreateMatchAndSave = new JButton("Maç Oluştur + Skorları Kaydet");
        btnCreateMatchAndSave.addActionListener(e -> saveScores());
        p.add(new JScrollPane(tblScores), BorderLayout.CENTER);
        p.add(btnCreateMatchAndSave, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildSummaryTab() {
        JPanel p = new JPanel(new BorderLayout());
        txtSummary = new JTextArea();
        txtSummary.setEditable(false);
        p.add(new JScrollPane(txtSummary), BorderLayout.CENTER);
        return p;
    }

    private void refreshParties() {
        partyListModel.clear();
        db.parties.forEach(partyListModel::addElement);
        if (!partyListModel.isEmpty()) lstParties.setSelectedIndex(0);
    }

    private void refreshGameVotes() {
        Party party = lstParties.getSelectedValue();
        if (party == null) {
            txtGameVotes.setText("");
            return;
        }
        Map<String, Long> counts = svc.gameVoteCounts(party.partyId);
        long total = counts.values().stream().mapToLong(l -> l).sum();
        String selectedGame = svc.selectedGameByVotes(party.partyId).orElse("-");
        String selectedTitle = db.games.stream().filter(g -> g.gameId.equals(selectedGame)).map(g -> g.title).findFirst().orElse("-");
        StringBuilder sb = new StringBuilder();
        sb.append("Oy Durumu (Toplam: ").append(total).append(")\n\n");
        // En çok oydan aza
        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted((a,b)-> Long.compare(b.getValue(), a.getValue())).toList();
        for (Map.Entry<String, Long> e : sorted) {
            String title = db.games.stream().filter(g -> g.gameId.equals(e.getKey())).map(g -> g.title).findFirst().orElse(e.getKey());
            int pct = (total == 0) ? 0 : (int) Math.round(100.0 * e.getValue() / total);
            sb.append(String.format("- %s: %d oy (%d%%)\n", title, e.getValue(), pct));
        }
        sb.append("\nSeçilen Oyun (öneri): ").append(selectedTitle);
        txtGameVotes.setText(sb.toString());
    }

    private void refreshSlots() {
        slotListModel.clear();
        Party party = lstParties.getSelectedValue();
        if (party == null) return;
        db.slots.stream().filter(s -> s.partyId.equals(party.partyId)).forEach(slotListModel::addElement);

        Map<String, Long> yesCounts = svc.slotYesCounts(party.partyId);
        Optional<String> suggested = svc.suggestedSlotId(party.partyId);

        String label = "Önerilen slot: -";
        if (suggested.isPresent()) {
            String sid = suggested.get();
            Slot s = db.slots.stream().filter(x -> x.slotId.equals(sid)).findFirst().orElse(null);
            long yes = yesCounts.getOrDefault(sid, 0L);
            label = "Önerilen slot: " + (s == null ? sid : s.toString()) + " (✓ " + yes + ")";
        }
        lblSlotSuggest.setText(label);
    }

    private void refreshScoresTable() {
        scoreModel.setRowCount(0);
        // Basitçe tüm kullanıcılar için satır açalım
        for (User u : db.users) {
            scoreModel.addRow(new Object[]{u.name, 0, "lose"});
        }
    }

    private void refreshSummary() {
        Party party = lstParties.getSelectedValue();
        if (party == null) {
            txtSummary.setText("");
            return;
        }
        PartyService.Summary s = svc.summary(party.partyId);
        String gameTitle = s.selectedGameId == null ? "-" :
                db.games.stream().filter(g -> g.gameId.equals(s.selectedGameId)).map(g -> g.title).findFirst().orElse(s.selectedGameId);
        String slotText = "-";
        if (s.finalSlotId != null) {
            Slot sl = db.slots.stream().filter(x -> x.slotId.equals(s.finalSlotId)).findFirst().orElse(null);
            slotText = sl == null ? s.finalSlotId : sl.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Seçilen Oyun: ").append(gameTitle).append("\n");
        sb.append("Önerilen Slot: ").append(slotText).append("\n\n");
        sb.append("Liderlik Tablosu:\n");
        int rank = 1;
        for (Map.Entry<String,Integer> e : s.leaderboard) {
            String uname = db.users.stream().filter(u -> u.userId.equals(e.getKey())).map(u -> u.name).findFirst().orElse(e.getKey());
            sb.append(rank++).append(". ").append(uname).append(" - ").append(e.getValue()).append(" puan\n");
        }
        sb.append("\nToplam maç: ").append(s.totalMatches);
        txtSummary.setText(sb.toString());
    }

    private void voteCurrentSlot(boolean yes) {
        Party party = lstParties.getSelectedValue();
        if (party == null) return;
        Slot slot = lstSlots.getSelectedValue();
        if (slot == null) return;
        User user = (User) cboUser.getSelectedItem();
        try {
            svc.voteSlot(slot.slotId, user.userId, yes);
            refreshSlots();
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void saveScores() {
        Party party = lstParties.getSelectedValue();
        if (party == null) return;
        String gameId = svc.selectedGameByVotes(party.partyId).orElse(null);
        if (gameId == null) {
            JOptionPane.showMessageDialog(this, "Önce oyun oylayın.");
            return;
        }
        try {
            Match m = svc.createMatch(party.partyId, gameId, svc.nowIso());
            List<Score> res = new ArrayList<>();
            for (int i = 0; i < scoreModel.getRowCount(); i++) {
                String userName = (String) scoreModel.getValueAt(i, 0);
                String userId = db.users.stream().filter(u -> u.name.equals(userName)).map(u -> u.userId).findFirst().orElse(null);
                if (userId == null) continue;
                int score = 0;
                try { score = Integer.parseInt(String.valueOf(scoreModel.getValueAt(i, 1))); } catch (Exception ignore) {}
                String result = String.valueOf(scoreModel.getValueAt(i, 2)).toLowerCase(Locale.ROOT);
                if (!result.equals("win") && !result.equals("draw") && !result.equals("lose")) result = "lose";
                Score s = new Score();
                s.matchId = m.matchId;
                s.userId = userId;
                s.score = score;
                s.result = result;
                res.add(s);
            }
            svc.postScores(m.matchId, res);
            refreshSummary();
            JOptionPane.showMessageDialog(this, "Skorlar kaydedildi. Match: " + m.matchId);
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void showErr(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PartyHubApp().setVisible(true));
    }
}