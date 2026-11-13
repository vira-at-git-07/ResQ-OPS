import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

// --- 4.5 ENUMS & 4.7 CUSTOM EXCEPTIONS ---
enum TicketStatus { PENDING, DISPATCHED, RESOLVED, FALSE_ALARM }
enum SeverityLevel {
    LOW(1), MEDIUM(2), HIGH(3), CRITICAL_SOS(4);
    final int r; SeverityLevel(int r) { this.r = r; } int getRank() { return r; }
}

class InvalidAuthenticationException extends Exception { public InvalidAuthenticationException(String m) { super(m); } }
class EmptyFieldException extends Exception { public EmptyFieldException(String m) { super(m); } }
class InvalidEmergencyTypeException extends Exception { public InvalidEmergencyTypeException(String m) { super(m); } }
class ResponderUnavailableException extends Exception { public ResponderUnavailableException(String m) { super(m); } }

// --- 4.3 & 4.4 INTERFACES & RESPONDER TEAMS ---
interface Respondable { void dispatchResponder(String id) throws ResponderUnavailableException; String getTeamName(); }
interface ExternalContactable { String fetchNearestFacility(String t); String notifyExternalService(String t, String loc); }

class MedicalTeam implements Respondable {
    public void dispatchResponder(String id) throws ResponderUnavailableException {}
    public String getTeamName() { return "Campus Paramedic & Ambulance Unit"; }
}
class FireSafetyTeam implements Respondable {
    public void dispatchResponder(String id) throws ResponderUnavailableException {}
    public String getTeamName() { return "Fire & Electrical Hazard Squad"; }
}
class QuickResponseSecurity implements Respondable {
    public void dispatchResponder(String id) throws ResponderUnavailableException {}
    public String getTeamName() { return "Quick Response Security Force (QRSF)"; }
}

// --- 4.1 USER HIERARCHY & 3-STRIKE BAN ---
abstract class User implements Serializable {
    protected String uid, pwd, nm, rl; protected int strikes = 0;
    protected User(String u, String p, String n, String r) { uid = u.trim(); pwd = p; nm = n.trim(); rl = r.trim().toUpperCase(); }
    public String getUserId() { return uid; } public String getPasswordHash() { return pwd; }
    public String getName() { return nm; } public String getRole() { return rl; }
    public int getStrikes() { return strikes; } public void addStrike() { strikes++; }
    public abstract void displayDashboardRole();
}
class Student extends User {
    public Student(String u, String p, String n, String x) { super(u, p, n, "STUDENT"); }
    public void displayDashboardRole() { JOptionPane.showMessageDialog(null, "Student Active: " + nm + "\nAccess: Incident Reporting & SOS", "Student Portal", 1); }
}
class Faculty extends User {
    public Faculty(String u, String p, String n, String x) { super(u, p, n, "FACULTY"); }
    public void displayDashboardRole() { JOptionPane.showMessageDialog(null, "Admin Active: " + nm + "\nAccess: Full Command Control", "Admin Portal", 1); }
}
class SecurityOfficer extends User {
    public SecurityOfficer(String u, String p, String n, String x) { super(u, p, n, "SECURITY_OFFICER"); }
    public void displayDashboardRole() { JOptionPane.showMessageDialog(null, "Officer Active: " + nm + "\nAccess: Dispatch & Verification", "Security Control", 1); }
}

// --- 4.2 EMERGENCY HIERARCHY & IMMEDIATE PROTOCOLS ---
abstract class Emergency implements Serializable {
    protected String eid = "RESQ-" + (10000 + new Random().nextInt(90000)), rep, loc, desc, unit = "Unassigned";
    protected SeverityLevel sev; protected TicketStatus st = TicketStatus.PENDING;
    protected Emergency(String r, String l, String d, SeverityLevel s) { rep = r; loc = l.trim(); desc = d.trim(); sev = s; }
    public String getId() { return eid; } public String getRep() { return rep; } public String getLoc() { return loc; }
    public SeverityLevel getSev() { return sev; } public TicketStatus getSt() { return st; } public void setSt(TicketStatus s) { st = s; }
    public String getUnit() { return unit; } public void setUnit(String u) { unit = u; }
    public abstract void displayImmediateSteps();
}
class FireEmergency extends Emergency {
    public FireEmergency(String r, String l, String d, SeverityLevel s) { super(r, l, d, s); }
    public void displayImmediateSteps() { JOptionPane.showMessageDialog(null, "🔥 FIRE PROTOCOL:\n1. Pull nearest fire alarm.\n2. Use stairs only (NO elevators).\n3. Crawl low under smoke to Assembly Point.", "Fire Alert", 2); }
}
class MedicalEmergency extends Emergency {
    public MedicalEmergency(String r, String l, String d, SeverityLevel s) { super(r, l, d, s); }
    public void displayImmediateSteps() { JOptionPane.showMessageDialog(null, "🏥 MEDICAL PROTOCOL:\n1. Keep patient still.\n2. Check breathing & pulse.\n3. Apply firm pressure on bleeding wounds.", "Medical Alert", 1); }
}
class SecurityEmergency extends Emergency {
    public SecurityEmergency(String r, String l, String d, SeverityLevel s) { super(r, l, d, s); }
    public void displayImmediateSteps() { JOptionPane.showMessageDialog(null, "🛡️ SECURITY PROTOCOL:\n1. Lock & barricade doors.\n2. Silence phones, stay away from windows.\n3. Await official security clearance.", "Lockdown Alert", 2); }
}
class ElectricalEmergency extends Emergency {
    public ElectricalEmergency(String r, String l, String d, SeverityLevel s) { super(r, l, d, s); }
    public void displayImmediateSteps() { JOptionPane.showMessageDialog(null, "⚡ ELECTRICAL PROTOCOL:\n1. Stay 10m away from live wires.\n2. Do NOT use water.\n3. Cut main breaker if safe.", "Hazard Alert", 2); }
}

// --- 4.3 SHORTEST-DISTANCE SORTED DIRECTORY ---
class HubNode implements Comparable<HubNode> {
    String title, ph; double km; int eta;
    HubNode(String t, double k, int e, String p) { title = t; km = k; eta = e; ph = p; }
    public int compareTo(HubNode o) { return Double.compare(this.km, o.km); }
}

class ExternalEmergencyDirectory implements ExternalContactable {
    private final Map<String, List<HubNode>> map = new HashMap<>();
    public ExternalEmergencyDirectory() {
        put("HOSPITAL", new HubNode("Max Multi Speciality, Sec-19, Noida", 11.5, 18, "0120-6629999"),
                        new HubNode("Jaypee Hospital, Sector-128, Noida", 1.8, 4, "0120-4122222"),
                        new HubNode("Felix Hospital, Sector-137, Noida", 4.5, 9, "0120-7184400"));
        put("POLICE",   new HubNode("Police Station Expressway, Sec-135, Noida", 2.9, 5, "112 / 0120-2400222"),
                        new HubNode("Sector-128 Police Chowki, Noida", 1.2, 3, "9870395200"));
        put("FIRE",     new HubNode("Fire Station Phase-2, Noida", 5.8, 11, "0120-2568101"),
                        new HubNode("UP Fire Service Station, Sec-135, Noida", 3.2, 6, "101 / 0120-2400101"));
    }
    private void put(String k, HubNode... nodes) { List<HubNode> l = Arrays.asList(nodes); Collections.sort(l); map.put(k, l); }

    public String fetchNearestFacility(String t) {
        List<HubNode> l = map.get(t.trim().toUpperCase());
        if (l == null) return "No agency mapped.";
        StringBuilder sb = new StringBuilder("PRIORITIZED BY SHORTEST DISTANCE FROM CAMPUS:\n==================================================================\n");
        for (int i = 0; i < l.size(); i++) {
            HubNode n = l.get(i);
            sb.append(String.format("%d) %s%s\n   Distance: %.1f km | ETA: ~%d mins | Contact: %s\n\n", i + 1, n.title, i == 0 ? " [NEAREST / PRIMARY]" : "", n.km, n.eta, n.ph));
        }
        return sb.toString();
    }
    public String closestTarget(String t) { List<HubNode> l = map.get(t.trim().toUpperCase()); return (l != null && !l.isEmpty()) ? l.get(0).title : "Jaypee Hospital Sector 128 Noida"; }
    public String notifyExternalService(String t, String loc) {
        String c = t.trim().toUpperCase();
        return "[EXTERNAL SOS - " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]\nAlert sent to NEAREST " + c + ":\n>> " + closestTarget(c) + "\nCampus Location: " + loc.trim();
    }
}

// --- 4.8 & 4.9 BACKEND ENGINE, FILE I/O & GARBAGE COLLECTION ---
class EmergencyControlCenter {
    final List<Emergency> q = new ArrayList<>();
    final Map<String, User> reg = new HashMap<>();
    final ExternalEmergencyDirectory dir = new ExternalEmergencyDirectory();
    final Respondable med = new MedicalTeam(), fire = new FireSafetyTeam(), sec = new QuickResponseSecurity();
    final String U_FILE = "resqops_user_database.txt", A_FILE = "resqops_incident_archive.txt";
    int okCnt = 0, spamCnt = 0;

    public EmergencyControlCenter() {
        loadU();
        if (!reg.containsKey("pankaj")) { reg.put("pankaj", new Faculty("pankaj", "pankaj123", "Pankaj Sir", "CSE")); saveU(); }
        countLogs();
    }
    public void register(User u) throws Exception {
        if (u.getUserId().isEmpty() || u.getPasswordHash().isEmpty() || u.getName().isEmpty()) throw new EmptyFieldException("All fields are mandatory!");
        if (reg.containsKey(u.getUserId())) throw new InvalidAuthenticationException("User ID already exists!");
        reg.put(u.getUserId(), u); saveU();
    }
    public User login(String id, String pw) throws Exception {
        if (id.trim().isEmpty() || pw.trim().isEmpty()) throw new EmptyFieldException("Enter ID and Password!");
        User u = reg.get(id.trim());
        if (u == null || !u.getPasswordHash().equals(pw)) throw new InvalidAuthenticationException("Invalid User ID or Password!");
        if (u.getStrikes() >= 3 && "STUDENT".equalsIgnoreCase(u.getRole())) throw new InvalidAuthenticationException("BANNED_ACCOUNT");
        return u;
    }
    private void saveU() {
        try (PrintWriter w = new PrintWriter(new FileWriter(U_FILE))) {
            for (User u : reg.values()) w.println(String.join(",", u.getUserId(), u.getPasswordHash(), u.getName(), u.getRole(), String.valueOf(u.getStrikes())));
        } catch (Exception ignored) {}
    }
    private void loadU() {
        try (BufferedReader r = new BufferedReader(new FileReader(U_FILE))) {
            for (String s; (s = r.readLine()) != null; ) {
                String[] p = s.split(",");
                if (p.length >= 4) {
                    User u = "FACULTY".equalsIgnoreCase(p[3]) ? new Faculty(p[0], p[1], p[2], "") : "SECURITY_OFFICER".equalsIgnoreCase(p[3]) ? new SecurityOfficer(p[0], p[1], p[2], "") : new Student(p[0], p[1], p[2], "");
                    if (p.length >= 5) u.strikes = Integer.parseInt(p[4].trim());
                    reg.put(u.getUserId(), u);
                }
            }
        } catch (Exception ignored) {}
    }
    public Emergency make(String t, String r, String l, String d, SeverityLevel s) throws Exception {
        if (l == null || l.trim().isEmpty()) throw new EmptyFieldException("Location cannot be blank!");
        String n = (d == null || d.trim().isEmpty()) ? "Standard alert." : d.trim(), k = t.trim().toUpperCase();
        if ("FIRE".equals(k)) return new FireEmergency(r, l, n, s);
        if ("MEDICAL".equals(k)) return new MedicalEmergency(r, l, n, s);
        if ("SECURITY".equals(k)) return new SecurityEmergency(r, l, n, s);
        if ("ELECTRICAL".equals(k)) return new ElectricalEmergency(r, l, n, s);
        throw new InvalidEmergencyTypeException("Invalid Type: " + t);
    }
    public void push(Emergency e) { q.add(e); q.sort((a, b) -> Integer.compare(b.getSev().getRank(), a.getSev().getRank())); }
    public void dispatch(String id) throws ResponderUnavailableException {
        for (Emergency e : q) if (e.getId().equals(id)) {
            Respondable tm = (e instanceof MedicalEmergency) ? med : (e instanceof FireEmergency || e instanceof ElectricalEmergency) ? fire : sec;
            tm.dispatchResponder(id); e.setSt(TicketStatus.DISPATCHED); e.setUnit(tm.getTeamName()); break;
        }
    }
    public String archive(String id, boolean spam, String by) {
        Iterator<Emergency> it = q.iterator();
        while (it.hasNext()) {
            Emergency e = it.next();
            if (e.getId().equals(id)) {
                e.setSt(spam ? TicketStatus.FALSE_ALARM : TicketStatus.RESOLVED);
                if (spam) { User u = reg.get(e.getRep()); if (u != null) { u.addStrike(); saveU(); } spamCnt++; } else okCnt++;
                try (PrintWriter w = new PrintWriter(new FileWriter(A_FILE, true))) {
                    w.println(String.join(",", e.getId(), e.getClass().getSimpleName(), e.getLoc(), e.getSt().toString(), by, new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())));
                } catch (Exception ignored) {}
                it.remove();
                e = null; System.gc(); // 4.9 Explicit Nullification & GC
                return "Incident " + id + " archived & cleared via System.gc().";
            }
        }
        return "Not found.";
    }
    private void countLogs() {
        okCnt = spamCnt = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(A_FILE))) {
            for (String s; (s = r.readLine()) != null; ) if (s.contains("RESOLVED")) okCnt++; else if (s.contains("FALSE_ALARM")) spamCnt++;
        } catch (Exception ignored) {}
    }
}

// --- LIQUID GLASS CUSTOM UI COMPONENTS ---
class LiquidBg extends JPanel {
    private float a = 0f;
    LiquidBg(LayoutManager l) { super(l); new javax.swing.Timer(50, e -> { a += 0.03f; repaint(); }).start(); }
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, new Color(10, 15, 30), w, h, new Color(24, 24, 58))); g2.fillRect(0, 0, w, h);
        orb(g2, (int)(w * 0.22 + Math.sin(a) * 40), (int)(h * 0.28 + Math.cos(a * 0.8) * 30), 310, new Color(56, 189, 248, 52));
        orb(g2, (int)(w * 0.78 + Math.cos(a * 0.9) * 45), (int)(h * 0.72 + Math.sin(a * 0.7) * 35), 350, new Color(139, 92, 246, 52));
        g2.dispose();
    }
    private void orb(Graphics2D g2, int x, int y, int r, Color c) {
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), r, new float[]{0f, 1f}, new Color[]{c, new Color(0, 0, 0, 0)}));
        g2.fillOval(x - r, y - r, r * 2, r * 2);
    }
}

class GlassBox extends JPanel {
    int rad; Color bg, rim;
    GlassBox(LayoutManager l, int r, Color b, Color m) { super(l); rad = r; bg = b; rim = m; setOpaque(false); }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D rect = new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, rad, rad);
        g2.setColor(bg); g2.fill(rect);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 36), 0, getHeight() * 0.5f, new Color(255, 255, 255, 0))); g2.fill(rect);
        g2.setColor(rim); g2.setStroke(new BasicStroke(1.2f)); g2.draw(rect); g2.dispose(); super.paintComponent(g);
    }
}

class GlowBtn extends JButton {
    Color c1, c2; boolean hov = false;
    GlowBtn(String t, Color a, Color b) {
        super(t); c1 = a; c2 = b; setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
        setForeground(Color.WHITE); setFont(new Font("SansSerif", 1, 13)); setCursor(new Cursor(12)); setBorder(new EmptyBorder(10, 20, 10, 20));
        addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { hov = true; repaint(); } public void mouseExited(MouseEvent e) { hov = false; repaint(); } });
    }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(); Color c = hov ? c2 : c1;
        g2.setPaint(new GradientPaint(0, 0, c.brighter(), 0, h, c)); g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
        g2.setColor(new Color(255, 255, 255, hov ? 135 : 75)); g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h, h));
        g2.dispose(); super.paintComponent(g);
    }
}

// --- 4.10 MAIN LIQUID GUI FRAME ---
public class SmartCampusEmergencySystem extends JFrame {
    private final EmergencyControlCenter core = new EmergencyControlCenter();
    private User usr = null;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final DefaultTableModel tmAct = model("Incident ID", "Type", "Reporter", "Location", "Severity", "Status", "Assigned Unit");
    private final DefaultTableModel tmArc = model("Incident ID", "Type", "Location", "Final Status", "Verified By", "Timestamp");
    private final JLabel kAct = new JLabel("0", 0), kOk = new JLabel("0", 0), kSpam = new JLabel("0", 0), badge = new JLabel("Not Logged In");

    public SmartCampusEmergencySystem() {
        setTitle("ResQOPS - Liquid Glass Command Edition"); setSize(1160, 780);
        setDefaultCloseOperation(3); setLocationRelativeTo(null);
        root.add(loginView(), "L"); root.add(dashView(), "D"); setContentPane(root);
    }
    private DefaultTableModel model(String... cols) { return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }
    private JLabel lbl(String t, int s, int st, Color c) { JLabel l = new JLabel(t, SwingConstants.LEFT); l.setFont(new Font("SansSerif", st, s)); l.setForeground(c); return l; }
    private GridBagConstraints gb(int x, int y, int w) { GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10, 10, 10, 10); g.fill = 2; g.gridx = x; g.gridy = y; g.gridwidth = w; return g; }
    private <T extends JTextField> T fld(T f) { f.setBackground(new Color(15, 23, 42)); f.setForeground(Color.WHITE); f.setCaretColor(new Color(56, 189, 248)); f.setFont(new Font("SansSerif", 0, 14)); f.setBorder(new CompoundBorder(new LineBorder(new Color(255, 255, 255, 70), 1, true), new EmptyBorder(7, 12, 7, 12))); return f; }
    private JScrollPane tblPane(JTable t) {
        t.setRowHeight(34); t.setFillsViewportHeight(true); t.setBackground(new Color(15, 23, 42)); t.setForeground(new Color(241, 245, 249));
        t.setGridColor(new Color(255, 255, 255, 25)); t.setFont(new Font("SansSerif", 0, 13)); t.setSelectionBackground(new Color(56, 189, 248, 90)); t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(new Color(30, 41, 59)); t.getTableHeader().setForeground(new Color(125, 211, 252)); t.getTableHeader().setFont(new Font("SansSerif", 1, 13));
        JScrollPane sp = new JScrollPane(t); sp.getViewport().setBackground(new Color(15, 23, 42)); sp.setBorder(new LineBorder(new Color(255, 255, 255, 45), 1, true)); return sp;
    }

    private JPanel loginView() {
        LiquidBg bg = new LiquidBg(new GridBagLayout());
        GlassBox box = new GlassBox(new GridBagLayout(), 28, new Color(255, 255, 255, 20), new Color(255, 255, 255, 65));
        box.setBorder(new EmptyBorder(40, 50, 40, 50));

        JLabel h1 = lbl("ResQOPS LIQUID SHIELD", 24, 1, new Color(125, 211, 252)); h1.setHorizontalAlignment(0);
        JLabel h2 = lbl("Next-Gen Campus Emergency Dispatch & Authentication", 13, 0, new Color(203, 213, 225)); h2.setHorizontalAlignment(0);
        JTextField uIn = fld(new JTextField(18)); JPasswordField pIn = fld(new JPasswordField(18));

        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0)); row.setOpaque(false);
        GlowBtn bIn = new GlowBtn("Secure Login", new Color(2, 132, 199), new Color(14, 165, 233));
        GlowBtn bUp = new GlowBtn("Register User", new Color(5, 150, 105), new Color(16, 185, 129));
        GlowBtn bDemo = new GlowBtn("Quick Demo Login (Pankaj Sir - Admin)", new Color(217, 119, 6), new Color(245, 158, 11));
        row.add(bIn); row.add(bUp);

        box.add(h1, gb(0, 0, 2)); box.add(h2, gb(0, 1, 2));
        box.add(lbl("User ID / Enrollment:", 13, 1, Color.WHITE), gb(0, 2, 1)); box.add(uIn, gb(1, 2, 1));
        box.add(lbl("Password:", 13, 1, Color.WHITE), gb(0, 3, 1)); box.add(pIn, gb(1, 3, 1));
        box.add(row, gb(0, 4, 2)); box.add(bDemo, gb(0, 5, 2));

        bIn.addActionListener(e -> {
            try {
                usr = core.login(uIn.getText(), new String(pIn.getPassword())); usr.displayDashboardRole();
                badge.setText("Active Operator: " + usr.getName() + " [" + usr.getRole() + "]");
                uIn.setText(""); pIn.setText(""); cards.show(root, "D"); sync();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "BANNED_ACCOUNT".equals(ex.getMessage()) ? "🚨 ACCOUNT SUSPENDED 🚨\n\n3+ False Alarm strikes accumulated.\nAccess permanently revoked. Contact Security." : ex.getMessage(), "Auth Error", 0);
            }
        });
        bUp.addActionListener(e -> {
            JTextField u = new JTextField(), n = new JTextField(); JPasswordField p = new JPasswordField();
            JComboBox<String> r = new JComboBox<>(new String[]{"STUDENT", "FACULTY", "SECURITY_OFFICER"});
            JPanel f = new JPanel(new GridLayout(4, 2, 8, 8));
            f.add(new JLabel("User ID:")); f.add(u); f.add(new JLabel("Password:")); f.add(p); f.add(new JLabel("Full Name:")); f.add(n); f.add(new JLabel("Role:")); f.add(r);
            if (JOptionPane.showConfirmDialog(this, f, "Create Account", 2, -1) == 0) {
                try {
                    String rl = (String) r.getSelectedItem(), pw = new String(p.getPassword());
                    core.register("FACULTY".equals(rl) ? new Faculty(u.getText(), pw, n.getText(), "") : "SECURITY_OFFICER".equals(rl) ? new SecurityOfficer(u.getText(), pw, n.getText(), "") : new Student(u.getText(), pw, n.getText(), ""));
                    JOptionPane.showMessageDialog(this, "Registered successfully!");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", 0); }
            }
        });
        bDemo.addActionListener(e -> { try { usr = core.login("pankaj", "pankaj123"); badge.setText("Active Operator: " + usr.getName() + " [" + usr.getRole() + "]"); cards.show(root, "D"); sync(); } catch (Exception ignored) {} });
        bg.add(box); return bg;
    }

    private JPanel kpi(String title, JLabel val, Color c) {
        GlassBox p = new GlassBox(new BorderLayout(5, 5), 22, new Color(c.getRed(), c.getGreen(), c.getBlue(), 30), new Color(c.getRed(), c.getGreen(), c.getBlue(), 140));
        p.setBorder(new EmptyBorder(14, 22, 14, 22));
        JLabel t = lbl(title, 12, 1, new Color(226, 232, 240)); t.setHorizontalAlignment(0);
        val.setFont(new Font("SansSerif", 1, 30)); val.setForeground(c.brighter());
        p.add(t, "North"); p.add(val, "Center"); return p;
    }

    private JPanel dashView() {
        LiquidBg ws = new LiquidBg(new BorderLayout(12, 12)); ws.setBorder(new EmptyBorder(15, 18, 15, 18));
        GlassBox nav = new GlassBox(new BorderLayout(), 20, new Color(255, 255, 255, 18), new Color(255, 255, 255, 50));
        nav.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel lt = new JPanel(new GridLayout(2, 1)); lt.setOpaque(false);
        badge.setFont(new Font("SansSerif", 0, 13)); badge.setForeground(new Color(148, 163, 184));
        lt.add(lbl("ResQOPS Liquid Command Center", 22, 1, Color.WHITE)); lt.add(badge);

        JLabel clk = lbl("🕒 Syncing...", 15, 1, new Color(125, 211, 252));
        GlowBtn out = new GlowBtn("Sign Out", new Color(71, 85, 105), new Color(100, 116, 139));
        out.addActionListener(e -> { usr = null; cards.show(root, "L"); });
        JPanel rt = new JPanel(new FlowLayout(2, 15, 2)); rt.setOpaque(false); rt.add(clk); rt.add(out);
        nav.add(lt, "West"); nav.add(rt, "East"); ws.add(nav, "North");

        Thread t = new Thread(() -> {
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a"); int s = 0;
            while (true) try { Thread.sleep(1000); final boolean r = (++s % 5 == 0); SwingUtilities.invokeLater(() -> { clk.setText("🕒 " + df.format(new Date())); if (r && usr != null) sync(); }); } catch (Exception ignored) {}
        });
        t.setDaemon(true); t.start();

        JTabbedPane tabs = new JTabbedPane(); tabs.setFont(new Font("SansSerif", 1, 14));

        // TAB 1: REPORT
        JPanel t1 = new JPanel(new GridBagLayout()); t1.setOpaque(false);
        GlassBox form = new GlassBox(new GridBagLayout(), 26, new Color(255, 255, 255, 22), new Color(255, 255, 255, 65));
        form.setBorder(new EmptyBorder(30, 40, 30, 40));
        GlowBtn sos = new GlowBtn("🚨 ONE-CLICK CRITICAL SOS PANIC 🚨", new Color(220, 38, 38), new Color(239, 68, 68));
        sos.setFont(new Font("SansSerif", 1, 16)); sos.setPreferredSize(new Dimension(430, 50));
        JComboBox<String> cbCat = new JComboBox<>(new String[]{"Fire", "Medical", "Security", "Electrical"});
        JTextField fLoc = fld(new JTextField("Academic Block 2", 22)), fDesc = fld(new JTextField(22));
        JComboBox<SeverityLevel> cbSev = new JComboBox<>(SeverityLevel.values()); cbSev.setSelectedItem(SeverityLevel.HIGH);
        GlowBtn sub = new GlowBtn("Submit Incident Report", new Color(2, 132, 199), new Color(14, 165, 233));

        form.add(sos, gb(0, 0, 2)); form.add(new JSeparator(), gb(0, 1, 2));
        form.add(lbl("Emergency Category:", 13, 1, Color.WHITE), gb(0, 2, 1)); form.add(cbCat, gb(1, 2, 1));
        form.add(lbl("Exact Campus Location:", 13, 1, Color.WHITE), gb(0, 3, 1)); form.add(fLoc, gb(1, 3, 1));
        form.add(lbl("Severity Assessment:", 13, 1, Color.WHITE), gb(0, 4, 1)); form.add(cbSev, gb(1, 4, 1));
        form.add(lbl("Situation Description:", 13, 1, Color.WHITE), gb(0, 5, 1)); form.add(fDesc, gb(1, 5, 1));
        form.add(sub, gb(0, 6, 2)); t1.add(form);

        sos.addActionListener(e -> {
            try {
                String l = fLoc.getText().trim().isEmpty() ? "Campus Central Zone" : fLoc.getText().trim();
                Emergency em = core.make("Security", usr.getUserId(), l, "CRITICAL SOS PANIC ACTIVATED", SeverityLevel.CRITICAL_SOS);
                core.push(em); em.displayImmediateSteps(); sync();
                if (!"STUDENT".equals(usr.getRole())) tabs.setSelectedIndex(1); else JOptionPane.showMessageDialog(this, "SOS Broadcasted! ID: " + em.getId());
            } catch (Exception ignored) {}
        });
        sub.addActionListener(e -> {
            try {
                Emergency em = core.make((String) cbCat.getSelectedItem(), usr.getUserId(), fLoc.getText(), fDesc.getText(), (SeverityLevel) cbSev.getSelectedItem());
                core.push(em); em.displayImmediateSteps(); fDesc.setText(""); sync();
                if (!"STUDENT".equals(usr.getRole())) tabs.setSelectedIndex(1); else JOptionPane.showMessageDialog(this, "Logged! ID: " + em.getId());
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", 0); }
        });

        // TAB 2: CONTROL ROOM
        GlassBox t2 = new GlassBox(new BorderLayout(12, 12), 24, new Color(255, 255, 255, 16), new Color(255, 255, 255, 45));
        t2.setBorder(new EmptyBorder(15, 15, 15, 15));
        JPanel kpis = new JPanel(new GridLayout(1, 3, 20, 0)); kpis.setOpaque(false);
        kpis.add(kpi("ACTIVE EMERGENCIES", kAct, new Color(248, 113, 113)));
        kpis.add(kpi("RESOLVED INCIDENTS", kOk, new Color(52, 211, 153)));
        kpis.add(kpi("FALSE ALARMS LOGGED", kSpam, new Color(251, 191, 36)));

        JPanel sBar = new JPanel(new FlowLayout(0)); sBar.setOpaque(false);
        JTextField fSrch = fld(new JTextField(25));
        sBar.add(lbl("🔍 Live Filter (Location / Type / ID): ", 13, 1, Color.WHITE)); sBar.add(fSrch);
        JPanel topA = new JPanel(new BorderLayout(0, 12)); topA.setOpaque(false); topA.add(kpis, "North"); topA.add(sBar, "South");

        JTable tblA = new JTable(tmAct); TableRowSorter<DefaultTableModel> srt = new TableRowSorter<>(tmAct); tblA.setRowSorter(srt);
        tblA.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean isS, boolean hasF, int r, int c) {
                Component cmp = super.getTableCellRendererComponent(tb, v, isS, hasF, r, c);
                if (!isS) {
                    String sv = String.valueOf(tb.getModel().getValueAt(tb.convertRowIndexToModel(r), 4));
                    cmp.setBackground("CRITICAL_SOS".equals(sv) ? new Color(153, 27, 27) : "HIGH".equals(sv) ? new Color(146, 64, 14) : new Color(15, 23, 42));
                    cmp.setForeground(Color.WHITE);
                }
                return cmp;
            }
        });
        fSrch.getDocument().addDocumentListener(new DocumentListener() {
            void f() { String q = fSrch.getText().trim(); srt.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + q)); }
            public void insertUpdate(DocumentEvent e) { f(); } public void removeUpdate(DocumentEvent e) { f(); } public void changedUpdate(DocumentEvent e) { f(); }
        });

        JPanel aBtns = new JPanel(new FlowLayout(2, 12, 5)); aBtns.setOpaque(false);
        GlowBtn bDisp = new GlowBtn("Dispatch Response Team", new Color(2, 132, 199), new Color(14, 165, 233));
        GlowBtn bRes = new GlowBtn("Mark Resolved", new Color(5, 150, 105), new Color(16, 185, 129));
        GlowBtn bFls = new GlowBtn("Flag False Alarm (+1 Strike)", new Color(220, 38, 38), new Color(239, 68, 68));
        aBtns.add(bDisp); aBtns.add(bRes); aBtns.add(bFls);
        t2.add(topA, "North"); t2.add(tblPane(tblA), "Center"); t2.add(aBtns, "South");

        ActionListener actL = e -> {
            int r = tblA.getSelectedRow(); if (r < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
            String id = (String) tblA.getValueAt(r, 0);
            try {
                if (e.getSource() == bDisp) { core.dispatch(id); JOptionPane.showMessageDialog(this, "Unit Dispatched to " + id); }
                else JOptionPane.showMessageDialog(this, core.archive(id, e.getSource() == bFls, usr.getName()));
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", 0); }
            sync();
        };
        bDisp.addActionListener(actL); bRes.addActionListener(actL); bFls.addActionListener(actL);

        // TAB 3: EXTERNAL DIRECTORY
        GlassBox t3 = new GlassBox(new BorderLayout(15, 15), 24, new Color(255, 255, 255, 16), new Color(255, 255, 255, 45));
        t3.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel dTop = new JPanel(new FlowLayout(0, 14, 5)); dTop.setOpaque(false);
        JComboBox<String> cbAg = new JComboBox<>(new String[]{"HOSPITAL", "POLICE", "FIRE"});
        GlowBtn bEsc = new GlowBtn("📡 Broadcast SOS to Nearest Unit", new Color(220, 38, 38), new Color(239, 68, 68));
        GlowBtn bMap = new GlowBtn("🗺️ Open Shortest Route in Google Maps", new Color(5, 150, 105), new Color(16, 185, 129));
        dTop.add(lbl("Target External Agency:", 14, 1, Color.WHITE)); dTop.add(cbAg); dTop.add(bEsc); dTop.add(bMap);

        JTextArea dOut = new JTextArea(core.dir.fetchNearestFacility("HOSPITAL"));
        dOut.setFont(new Font("Monospaced", 0, 15)); dOut.setBackground(new Color(15, 23, 42)); dOut.setForeground(new Color(125, 211, 252)); dOut.setEditable(false); dOut.setBorder(new EmptyBorder(18, 18, 18, 18));
        cbAg.addActionListener(e -> dOut.setText(core.dir.fetchNearestFacility((String) cbAg.getSelectedItem())));
        bEsc.addActionListener(e -> JOptionPane.showMessageDialog(this, core.dir.notifyExternalService((String) cbAg.getSelectedItem(), fLoc.getText()), "Dispatched", 2));
        bMap.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI("https://www.google.com/maps/dir/?api=1&origin=" + URLEncoder.encode("JIIT Sector 128 Noida", "UTF-8") + "&destination=" + URLEncoder.encode(core.dir.closestTarget((String) cbAg.getSelectedItem()), "UTF-8") + "&travelmode=driving")); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Browser error."); }
        });
        t3.add(dTop, "North"); t3.add(new JScrollPane(dOut), "Center");

        // TAB 4: ARCHIVES
        GlassBox t4 = new GlassBox(new BorderLayout(12, 12), 24, new Color(255, 255, 255, 16), new Color(255, 255, 255, 45));
        t4.setBorder(new EmptyBorder(15, 15, 15, 15));
        JPanel cBtns = new JPanel(new FlowLayout(0, 12, 5)); cBtns.setOpaque(false);
        GlowBtn bRef = new GlowBtn("Refresh Logs", new Color(71, 85, 105), new Color(100, 116, 139));
        GlowBtn bCsv = new GlowBtn("Export Audit Log to CSV", new Color(5, 150, 105), new Color(16, 185, 129));
        bRef.addActionListener(e -> sync());
        bCsv.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == 0) {
                File f = fc.getSelectedFile(); if (!f.getName().endsWith(".csv")) f = new File(f.getAbsolutePath() + ".csv");
                try (PrintWriter w = new PrintWriter(new FileWriter(f))) {
                    w.println("Incident ID,Type,Location,Final Status,Verified By,Timestamp");
                    for (int i = 0; i < tmArc.getRowCount(); i++) w.println(tmArc.getValueAt(i, 0) + "," + tmArc.getValueAt(i, 1) + "," + tmArc.getValueAt(i, 2) + "," + tmArc.getValueAt(i, 3) + "," + tmArc.getValueAt(i, 4) + "," + tmArc.getValueAt(i, 5));
                    JOptionPane.showMessageDialog(this, "Saved: " + f.getAbsolutePath());
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Export Failed."); }
            }
        });
        cBtns.add(bRef); cBtns.add(bCsv); t4.add(tblPane(new JTable(tmArc)), "Center"); t4.add(cBtns, "South");

        tabs.addTab("  Report Incident  ", t1); tabs.addTab("  Live Control Room  ", t2);
        tabs.addTab("  External Directory  ", t3); tabs.addTab("  Audit Archives  ", t4);
        tabs.addChangeListener(e -> { if (usr != null && "STUDENT".equals(usr.getRole()) && (tabs.getSelectedIndex() == 1 || tabs.getSelectedIndex() == 3)) { JOptionPane.showMessageDialog(this, "Restricted Area!"); tabs.setSelectedIndex(0); } });
        ws.add(tabs, "Center"); return ws;
    }

    private void sync() {
        tmAct.setRowCount(0);
        for (Emergency e : core.q) tmAct.addRow(new Object[]{e.getId(), e.getClass().getSimpleName(), e.getRep(), e.getLoc(), e.getSev(), e.getSt(), e.getUnit()});
        kAct.setText(String.valueOf(core.q.size()));
        if (usr != null && !"STUDENT".equals(usr.getRole())) {
            tmArc.setRowCount(0);
            try (BufferedReader r = new BufferedReader(new FileReader(core.A_FILE))) {
                for (String s; (s = r.readLine()) != null; ) { String[] p = s.split(","); if (p.length >= 6) tmArc.addRow(new Object[]{p[0], p[1], p[2], p[3], p[4], p[5]}); }
            } catch (Exception ignored) {}
            kOk.setText(String.valueOf(core.okCnt)); kSpam.setText(String.valueOf(core.spamCnt));
        }
    }

    public static void main(String[] args) {
        try { for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels()) if ("Nimbus".equals(i.getName())) UIManager.setLookAndFeel(i.getClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SmartCampusEmergencySystem().setVisible(true));
    }
}