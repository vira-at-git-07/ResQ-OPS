import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

// ==========================================
// 4.7 CUSTOM EXCEPTION CLASSES
// ==========================================
class InvalidAuthenticationException extends Exception {
    public InvalidAuthenticationException(String errorDetail) { super(errorDetail); }
}

class EmptyFieldException extends Exception {
    public EmptyFieldException(String errorDetail) { super(errorDetail); }
}

class InvalidEmergencyTypeException extends Exception {
    public InvalidEmergencyTypeException(String errorDetail) { super(errorDetail); }
}

class ResponderUnavailableException extends Exception {
    public ResponderUnavailableException(String errorDetail) { super(errorDetail); }
}

// ==========================================
// 4.5 PRIORITY & STATUS ENUMERATIONS
// ==========================================
enum TicketStatus {
    PENDING, DISPATCHED, RESOLVED, FALSE_ALARM
}

enum SeverityLevel {
    LOW(10), MEDIUM(20), HIGH(30), CRITICAL_SOS(40);
    
    private final int weight;
    SeverityLevel(int w) { this.weight = w; }
    public int getRank() { return this.weight; }
}

// ==========================================
// 4.3 & 4.4 SYSTEM INTERFACES
// ==========================================
interface ExternalContactable {
    String fetchNearestFacility(String category);
    String notifyExternalService(String category, String campusZone);
}

interface Respondable {
    void dispatchResponder(String ticketRef) throws ResponderUnavailableException;
    String getTeamName();
}

// ==========================================
// 4.4 CAMPUS RESPONSE UNITS
// ==========================================
class MedicalTeam implements Respondable {
    private boolean unitReady = true;
    @Override
    public void dispatchResponder(String ticketRef) throws ResponderUnavailableException {
        if (!this.unitReady) {
            throw new ResponderUnavailableException("Medical & Trauma Unit is already attending another call!");
        }
    }
    @Override
    public String getTeamName() { return "Campus Paramedic & Ambulance Squad"; }
}

class FireSafetyTeam implements Respondable {
    private boolean unitReady = true;
    @Override
    public void dispatchResponder(String ticketRef) throws ResponderUnavailableException {
        if (!this.unitReady) {
            throw new ResponderUnavailableException("Fire & Hazard Suppression Crew is currently occupied!");
        }
    }
    @Override
    public String getTeamName() { return "Fire & Electrical Containment Unit"; }
}

class QuickResponseSecurity implements Respondable {
    private boolean unitReady = true;
    @Override
    public void dispatchResponder(String ticketRef) throws ResponderUnavailableException {
        if (!this.unitReady) {
            throw new ResponderUnavailableException("Quick Reaction Security Patrol is busy on lockdown duty!");
        }
    }
    @Override
    public String getTeamName() { return "Campus Tactical Security Force (QRSF)"; }
}

// ==========================================
// 4.1 USER HIERARCHY & ACCOUNTABILITY
// ==========================================
abstract class User implements Serializable {
    protected String accountId;
    protected String secretKey;
    protected String fullName;
    protected String accessRole;
    protected int penaltyCount = 0;

    protected User(String id, String pass, String userFullName, String roleTag) {
        this.accountId = id.trim();
        this.secretKey = pass;
        this.fullName = userFullName.trim();
        this.accessRole = roleTag.trim().toUpperCase();
    }

    public String getUserId() { return this.accountId; }
    public String getPasswordHash() { return this.secretKey; }
    public String getName() { return this.fullName; }
    public String getRole() { return this.accessRole; }
    public int getStrikes() { return this.penaltyCount; }
    public void incrementStrikes() { this.penaltyCount += 1; }
    
    public abstract void displayDashboardRole();
}

class Student extends User {
    public Student(String id, String pass, String name, String rollInfo) {
        super(id, pass, name, "STUDENT");
    }
    @Override
    public void displayDashboardRole() {
        String msg = "Student Session Active: " + getName() + "\nPermissions: Emergency Reporting & SOS Panic";
        JOptionPane.showMessageDialog(null, msg, "Student Access Verified", JOptionPane.INFORMATION_MESSAGE);
    }
}

class Faculty extends User {
    public Faculty(String id, String pass, String name, String deptInfo) {
        super(id, pass, name, "FACULTY");
    }
    @Override
    public void displayDashboardRole() {
        String msg = "Faculty/Admin Session Active: " + getName() + "\nPermissions: Full Command & Audit Control";
        JOptionPane.showMessageDialog(null, msg, "Administrator Access Verified", JOptionPane.INFORMATION_MESSAGE);
    }
}

class SecurityOfficer extends User {
    public SecurityOfficer(String id, String pass, String name, String badgeNo) {
        super(id, pass, name, "SECURITY_OFFICER");
    }
    @Override
    public void displayDashboardRole() {
        String msg = "Security Officer Active: " + getName() + "\nPermissions: Live Dispatch & False Alarm Checks";
        JOptionPane.showMessageDialog(null, msg, "Security Control Verified", JOptionPane.INFORMATION_MESSAGE);
    }
}

// ==========================================
// 4.2 EMERGENCY MODEL & SURVIVAL PROTOCOLS
// ==========================================
abstract class Emergency implements Serializable {
    protected String ticketCode;
    protected String filedBy;
    protected String zoneName;
    protected String details;
    protected String responderUnit;
    protected SeverityLevel urgency;
    protected TicketStatus currentState;

    protected Emergency(String user, String loc, String info, SeverityLevel level) {
        int randomNum = 10000 + new Random().nextInt(90000);
        this.ticketCode = "RESQ-" + randomNum;
        this.filedBy = user;
        this.zoneName = loc.trim();
        this.details = info.trim();
        this.urgency = level;
        this.currentState = TicketStatus.PENDING;
        this.responderUnit = "Unassigned";
    }

    public String getEmergencyId() { return this.ticketCode; }
    public String getReporterId() { return this.filedBy; }
    public String getLocation() { return this.zoneName; }
    public String getDescription() { return this.details; }
    public SeverityLevel getSeverity() { return this.urgency; }
    public TicketStatus getStatus() { return this.currentState; }
    public void setStatus(TicketStatus st) { this.currentState = st; }
    public String getAssignedResponder() { return this.responderUnit; }
    public void setAssignedResponder(String unit) { this.responderUnit = unit; }

    public abstract void displayImmediateSteps();
}

class FireEmergency extends Emergency {
    public FireEmergency(String u, String l, String d, SeverityLevel s) { super(u, l, d, s); }
    @Override
    public void displayImmediateSteps() {
        String guide = "🔥 FIRE SURVIVAL STEPS:\n"
                     + "• Trigger the nearest manual fire alarm box.\n"
                     + "• Use staircases only; strictly avoid elevators.\n"
                     + "• Crawl low under smoke and head to the open assembly area.";
        JOptionPane.showMessageDialog(null, guide, "Fire Protocol Advisory", JOptionPane.WARNING_MESSAGE);
    }
}

class MedicalEmergency extends Emergency {
    public MedicalEmergency(String u, String l, String d, SeverityLevel s) { super(u, l, d, s); }
    @Override
    public void displayImmediateSteps() {
        String guide = "🏥 MEDICAL FIRST-AID STEPS:\n"
                     + "• Keep the patient still if spinal trauma is suspected.\n"
                     + "• Verify airway, pulse, and breathing status.\n"
                     + "• Apply clean, direct pressure on active bleeding until medics arrive.";
        JOptionPane.showMessageDialog(null, guide, "Medical Protocol Advisory", JOptionPane.INFORMATION_MESSAGE);
    }
}

class SecurityEmergency extends Emergency {
    public SecurityEmergency(String u, String l, String d, SeverityLevel s) { super(u, l, d, s); }
    @Override
    public void displayImmediateSteps() {
        String guide = "🛡️ SECURITY LOCKDOWN STEPS:\n"
                     + "• Enter the nearest room and bolt/barricade the entry.\n"
                     + "• Put phones on silent mode and stay clear of windows.\n"
                     + "• Do not open doors until official security forces clear the zone.";
        JOptionPane.showMessageDialog(null, guide, "Security Protocol Advisory", JOptionPane.WARNING_MESSAGE);
    }
}

class ElectricalEmergency extends Emergency {
    public ElectricalEmergency(String u, String l, String d, SeverityLevel s) { super(u, l, d, s); }
    @Override
    public void displayImmediateSteps() {
        String guide = "⚡ ELECTRICAL HAZARD STEPS:\n"
                     + "• Maintain a minimum 10-meter perimeter from live wires.\n"
                     + "• Never pour water on electrical sparks or equipment.\n"
                     + "• Isolate the main breaker panel only if it is dry and safe.";
        JOptionPane.showMessageDialog(null, guide, "Electrical Protocol Advisory", JOptionPane.WARNING_MESSAGE);
    }
}

// ==========================================
// 4.3 DISTANCE-SORTED EXTERNAL DIRECTORY
// ==========================================
class AgencyRecord implements Comparable<AgencyRecord> {
    final String agencyTitle;
    final double kmAway;
    final int travelMinutes;
    final String phoneLine;

    AgencyRecord(String title, double km, int mins, String phone) {
        this.agencyTitle = title;
        this.kmAway = km;
        this.travelMinutes = mins;
        this.phoneLine = phone;
    }

    @Override
    public int compareTo(AgencyRecord other) {
        return Double.compare(this.kmAway, other.kmAway);
    }
}

class ExternalEmergencyDirectory implements ExternalContactable {
    private final Map<String, List<AgencyRecord>> agencyLookup = new HashMap<>();

    public ExternalEmergencyDirectory() {
        List<AgencyRecord> medList = new ArrayList<>();
        medList.add(new AgencyRecord("Max Multi Speciality Centre, Sector-19, Noida", 11.5, 18, "0120-6629999"));
        medList.add(new AgencyRecord("Jaypee Hospital, Sector-128, Noida", 1.8, 4, "0120-4122222"));
        medList.add(new AgencyRecord("Felix Hospital, Sector-137, Noida (Trauma Center)", 4.5, 9, "0120-7184400"));
        Collections.sort(medList);
        agencyLookup.put("HOSPITAL", medList);

        List<AgencyRecord> copList = new ArrayList<>();
        copList.add(new AgencyRecord("Police Station Expressway, Sector-135, Noida", 2.9, 5, "112 / 0120-2400222"));
        copList.add(new AgencyRecord("Sector-128 Police Chowki, Noida", 1.2, 3, "9870395200"));
        Collections.sort(copList);
        agencyLookup.put("POLICE", copList);

        List<AgencyRecord> fireList = new ArrayList<>();
        fireList.add(new AgencyRecord("Fire Station Phase-2, Noida", 5.8, 11, "0120-2568101"));
        fireList.add(new AgencyRecord("UP Fire Service Station, Sector-135 Expressway", 3.2, 6, "101 / 0120-2400101"));
        Collections.sort(fireList);
        agencyLookup.put("FIRE", fireList);
    }

    @Override
    public String fetchNearestFacility(String category) {
        String key = category.trim().toUpperCase();
        List<AgencyRecord> records = agencyLookup.get(key);
        if (records == null || records.isEmpty()) {
            return "No external units available for category: " + key;
        }

        StringBuilder out = new StringBuilder();
        out.append("RANKED BY SHORTEST DISTANCE FROM CAMPUS:\n");
        out.append("==================================================================\n");
        for (int idx = 0; idx < records.size(); idx++) {
            AgencyRecord rec = records.get(idx);
            String badge = (idx == 0) ? " [NEAREST / AUTO-PRIORITY]" : "";
            out.append(String.format("%d) %s%s\n   Distance: %.1f km  |  ETA: ~%d mins  |  Helpline: %s\n\n",
                    (idx + 1), rec.agencyTitle, badge, rec.kmAway, rec.travelMinutes, rec.phoneLine));
        }
        return out.toString();
    }

    public String getShortestRouteTarget(String category) {
        List<AgencyRecord> records = agencyLookup.get(category.trim().toUpperCase());
        return (records != null && !records.isEmpty()) ? records.get(0).agencyTitle : "Jaypee Hospital Sector 128 Noida";
    }

    @Override
    public String notifyExternalService(String category, String campusZone) {
        String cleanCat = category.trim().toUpperCase();
        String closestUnit = getShortestRouteTarget(cleanCat);
        String clockTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        return "[EXTERNAL SOS DISPATCH - " + clockTime + "]\n"
             + "Emergency telemetry transmitted to closest " + cleanCat + " facility:\n"
             + ">> " + closestUnit + "\n"
             + "Reported Campus Location: " + campusZone.trim();
    }
}

// ==========================================
// 4.8 & 4.9 CORE CONTROLLER & FILE STORAGE
// ==========================================
class EmergencyControlCenter {
    private final List<Emergency> liveQueue = new ArrayList<>();
    private final Map<String, User> registeredAccounts = new HashMap<>();
    private final ExternalEmergencyDirectory extDirectory = new ExternalEmergencyDirectory();

    private final Respondable medicSquad = new MedicalTeam();
    private final Respondable fireSquad = new FireSafetyTeam();
    private final Respondable secSquad = new QuickResponseSecurity();

    private static final String ACCOUNTS_DB = "resqops_user_database.txt";
    private static final String ARCHIVE_DB = "resqops_incident_archive.txt";

    public int resolvedCounter = 0;
    public int falseAlarmCounter = 0;

    public EmergencyControlCenter() {
        readAccountsFromDisk();
        if (!registeredAccounts.containsKey("pankaj")) {
            registeredAccounts.put("pankaj", new Faculty("pankaj", "pankaj123", "Pankaj Sir", "CSE"));
            writeAccountsToDisk();
        }
        syncHistoricalCounters();
    }

    public ExternalEmergencyDirectory getDirectory() { return this.extDirectory; }
    public List<Emergency> getActiveIncidents() { return this.liveQueue; }

    public void registerUser(User candidate) throws Exception {
        if (candidate.getUserId().isEmpty() || candidate.getPasswordHash().isEmpty() || candidate.getName().isEmpty()) {
            throw new EmptyFieldException("User ID, Password, and Name cannot be left blank!");
        }
        if (registeredAccounts.containsKey(candidate.getUserId())) {
            throw new InvalidAuthenticationException("User ID '" + candidate.getUserId() + "' is already taken!");
        }
        registeredAccounts.put(candidate.getUserId(), candidate);
        writeAccountsToDisk();
    }

    public User authenticateUser(String inputId, String inputPass) throws InvalidAuthenticationException, EmptyFieldException {
        String cleanId = inputId.trim();
        if (cleanId.isEmpty() || inputPass.trim().isEmpty()) {
            throw new EmptyFieldException("Please enter both User ID and Password!");
        }
        User found = registeredAccounts.get(cleanId);
        if (found == null || !found.getPasswordHash().equals(inputPass)) {
            throw new InvalidAuthenticationException("Incorrect User ID or Password!");
        }
        if (found.getStrikes() >= 3 && "STUDENT".equalsIgnoreCase(found.getRole())) {
            throw new InvalidAuthenticationException("BANNED_ACCOUNT");
        }
        return found;
    }

    private void writeAccountsToDisk() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ACCOUNTS_DB))) {
            for (User acc : registeredAccounts.values()) {
                writer.println(String.join(",", acc.getUserId(), acc.getPasswordHash(), acc.getName(), acc.getRole(), String.valueOf(acc.getStrikes())));
            }
        } catch (IOException ignored) { }
    }

    private void readAccountsFromDisk() {
        File dbFile = new File(ACCOUNTS_DB);
        if (!dbFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String row;
            while ((row = reader.readLine()) != null) {
                String[] tokens = row.split(",");
                if (tokens.length >= 4) {
                    User loaded;
                    String roleType = tokens[3].trim();
                    if ("FACULTY".equalsIgnoreCase(roleType)) loaded = new Faculty(tokens[0], tokens[1], tokens[2], "Dept");
                    else if ("SECURITY_OFFICER".equalsIgnoreCase(roleType)) loaded = new SecurityOfficer(tokens[0], tokens[1], tokens[2], "Badge");
                    else loaded = new Student(tokens[0], tokens[1], tokens[2], "Roll");
                    
                    if (tokens.length >= 5) {
                        loaded.penaltyCount = Integer.parseInt(tokens[4].trim());
                    }
                    registeredAccounts.put(loaded.getUserId(), loaded);
                }
            }
        } catch (Exception ignored) { }
    }

    public Emergency buildIncident(String category, String reporter, String spot, String notes, SeverityLevel sev)
            throws EmptyFieldException, InvalidEmergencyTypeException {
        if (spot == null || spot.trim().isEmpty()) {
            throw new EmptyFieldException("Incident location is required!");
        }
        String safeNotes = (notes == null || notes.trim().isEmpty()) ? "Standard alert filed." : notes.trim();
        String normCat = category.trim().toUpperCase();

        if ("FIRE".equals(normCat)) return new FireEmergency(reporter, spot, safeNotes, sev);
        if ("MEDICAL".equals(normCat)) return new MedicalEmergency(reporter, spot, safeNotes, sev);
        if ("SECURITY".equals(normCat)) return new SecurityEmergency(reporter, spot, safeNotes, sev);
        if ("ELECTRICAL".equals(normCat)) return new ElectricalEmergency(reporter, spot, safeNotes, sev);
        
        throw new InvalidEmergencyTypeException("Unrecognized Emergency Category: " + category);
    }

    public void enqueueIncident(Emergency item) {
        liveQueue.add(item);
        liveQueue.sort((a, b) -> Integer.compare(b.getSeverity().getRank(), a.getSeverity().getRank()));
    }

    public void assignResponderUnit(String ticketId) throws ResponderUnavailableException {
        for (Emergency item : liveQueue) {
            if (item.getEmergencyId().equals(ticketId)) {
                Respondable chosenSquad = secSquad;
                if (item instanceof MedicalEmergency) chosenSquad = medicSquad;
                else if (item instanceof FireEmergency || item instanceof ElectricalEmergency) chosenSquad = fireSquad;

                chosenSquad.dispatchResponder(ticketId);
                item.setStatus(TicketStatus.DISPATCHED);
                item.setAssignedResponder(chosenSquad.getTeamName());
                return;
            }
        }
    }

    public String closeAndArchiveTicket(String ticketId, boolean markAsSpam, String supervisorName) {
        for (int i = 0; i < liveQueue.size(); i++) {
            Emergency target = liveQueue.get(i);
            if (target.getEmergencyId().equals(ticketId)) {
                target.setStatus(markAsSpam ? TicketStatus.FALSE_ALARM : TicketStatus.RESOLVED);

                if (markAsSpam) {
                    User offender = registeredAccounts.get(target.getReporterId());
                    if (offender != null) {
                        offender.incrementStrikes();
                        writeAccountsToDisk();
                    }
                    falseAlarmCounter++;
                } else {
                    resolvedCounter++;
                }

                try (PrintWriter logOut = new PrintWriter(new FileWriter(ARCHIVE_DB, true))) {
                    String dateTag = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                    logOut.println(target.getEmergencyId() + "," + target.getClass().getSimpleName() + ","
                            + target.getLocation() + "," + target.getStatus() + "," + supervisorName + "," + dateTag);
                } catch (IOException ignored) { }

                liveQueue.remove(i);

                // 4.9 Memory Management & Explicit Garbage Collection Trigger
                target = null;
                System.gc();
                return "Ticket " + ticketId + " archived. Memory reference cleared via System.gc().";
            }
        }
        return "Ticket ID not found.";
    }

    private void syncHistoricalCounters() {
        resolvedCounter = 0;
        falseAlarmCounter = 0;
        File logFile = new File(ARCHIVE_DB);
        if (!logFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.contains("RESOLVED")) resolvedCounter++;
                else if (entry.contains("FALSE_ALARM")) falseAlarmCounter++;
            }
        } catch (Exception ignored) { }
    }
}

// ==========================================
// CUSTOM LIQUID GLASS SWING RENDERERS
// ==========================================
class AmbientLiquidBackdrop extends JPanel {
    private float animAngle = 0f;

    public AmbientLiquidBackdrop(LayoutManager mgr) {
        super(mgr);
        setOpaque(true);
        new javax.swing.Timer(50, evt -> {
            animAngle += 0.03f;
            repaint();
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2d.setPaint(new GradientPaint(0, 0, new Color(10, 15, 30), width, height, new Color(24, 24, 58)));
        g2d.fillRect(0, 0, width, height);

        int cx1 = (int) (width * 0.22 + Math.sin(animAngle) * 40);
        int cy1 = (int) (height * 0.28 + Math.cos(animAngle * 0.85) * 30);
        g2d.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx1, cy1), 310f,
                new float[]{0f, 1f},
                new Color[]{new Color(56, 189, 248, 52), new Color(56, 189, 248, 0)}
        ));
        g2d.fillOval(cx1 - 310, cy1 - 310, 620, 620);

        int cx2 = (int) (width * 0.78 + Math.cos(animAngle * 0.9) * 48);
        int cy2 = (int) (height * 0.72 + Math.sin(animAngle * 0.75) * 38);
        g2d.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx2, cy2), 350f,
                new float[]{0f, 1f},
                new Color[]{new Color(139, 92, 246, 52), new Color(139, 92, 246, 0)}
        ));
        g2d.fillOval(cx2 - 350, cy2 - 350, 700, 700);

        g2d.dispose();
    }
}

class FrostedGlassCard extends JPanel {
    private final int arcSize;
    private final Color surfaceTint;
    private final Color edgeStroke;

    public FrostedGlassCard(LayoutManager mgr, int radius, Color fill, Color border) {
        super(mgr);
        this.arcSize = radius;
        this.surfaceTint = fill;
        this.edgeStroke = border;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, arcSize, arcSize);

        g2d.setColor(surfaceTint);
        g2d.fill(shape);

        g2d.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 36), 0, h * 0.5f, new Color(255, 255, 255, 0)));
        g2d.fill(shape);

        g2d.setColor(edgeStroke);
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.draw(shape);

        g2d.dispose();
        super.paintComponent(g);
    }
}

class PillLiquidButton extends JButton {
    private final Color normalBg;
    private final Color activeBg;
    private boolean mouseOver = false;

    public PillLiquidButton(String caption, Color primary, Color highlight) {
        super(caption);
        this.normalBg = primary;
        this.activeBg = highlight;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 20, 10, 20));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { mouseOver = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { mouseOver = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        Color shade = mouseOver ? activeBg : normalBg;

        g2d.setPaint(new GradientPaint(0, 0, shade.brighter(), 0, h, shade));
        g2d.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

        g2d.setColor(new Color(255, 255, 255, mouseOver ? 135 : 75));
        g2d.setStroke(new BasicStroke(1.1f));
        g2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h, h));

        g2d.dispose();
        super.paintComponent(g);
    }
}

// ==========================================
// 4.10 MAIN APPLICATION FRAME
// ==========================================
public class SmartCampusEmergencySystem extends JFrame {
    private final EmergencyControlCenter backend = new EmergencyControlCenter();
    private User activeSessionUser = null;
    private final CardLayout screenSwitcher = new CardLayout();
    private final JPanel rootContainer = new JPanel(screenSwitcher);

    private final DefaultTableModel liveTableModel = new DefaultTableModel(
            new String[]{"Incident ID", "Type", "Reporter", "Location", "Severity", "Status", "Assigned Unit"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultTableModel historyTableModel = new DefaultTableModel(
            new String[]{"Incident ID", "Type", "Location", "Final Status", "Verified By", "Timestamp"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JLabel metricActive = new JLabel("0", SwingConstants.CENTER);
    private final JLabel metricResolved = new JLabel("0", SwingConstants.CENTER);
    private final JLabel metricFalse = new JLabel("0", SwingConstants.CENTER);
    private final JLabel sessionHeaderTag = new JLabel("Not Logged In");

    public SmartCampusEmergencySystem() {
        setTitle("ResQOPS - Liquid Glass Command Edition");
        setSize(1160, 780);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        rootContainer.add(constructAuthScreen(), "AUTH_VIEW");
        rootContainer.add(constructMainWorkspace(), "MAIN_VIEW");
        setContentPane(rootContainer);
    }

    private JLabel makeStyledLabel(String text, int pt, boolean isBold, Color fg) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", isBold ? Font.BOLD : Font.PLAIN, pt));
        label.setForeground(fg);
        return label;
    }

    private void applyDarkFieldTheme(JTextField input) {
        input.setBackground(new Color(15, 23, 42));
        input.setForeground(Color.WHITE);
        input.setCaretColor(new Color(56, 189, 248));
        input.setFont(new Font("SansSerif", Font.PLAIN, 14));
        input.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 70), 1, true),
                new EmptyBorder(7, 12, 7, 12)
        ));
    }

    private void configureGlassTable(JTable grid) {
        grid.setRowHeight(34);
        grid.setFillsViewportHeight(true);
        grid.setBackground(new Color(15, 23, 42));
        grid.setForeground(new Color(241, 245, 249));
        grid.setGridColor(new Color(255, 255, 255, 25));
        grid.setFont(new Font("SansSerif", Font.PLAIN, 13));
        grid.setSelectionBackground(new Color(56, 189, 248, 90));
        grid.setSelectionForeground(Color.WHITE);

        JTableHeader th = grid.getTableHeader();
        th.setBackground(new Color(30, 41, 59));
        th.setForeground(new Color(125, 211, 252));
        th.setFont(new Font("SansSerif", Font.BOLD, 13));
        th.setBorder(new LineBorder(new Color(255, 255, 255, 40)));
    }

    private JPanel constructAuthScreen() {
        AmbientLiquidBackdrop bgCanvas = new AmbientLiquidBackdrop(new GridBagLayout());

        FrostedGlassCard authBox = new FrostedGlassCard(
                new GridBagLayout(), 28, new Color(255, 255, 255, 20), new Color(255, 255, 255, 65));
        authBox.setBorder(new EmptyBorder(40, 50, 40, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel headerTitle = new JLabel("ResQOPS LIQUID SHIELD", SwingConstants.CENTER);
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerTitle.setForeground(new Color(125, 211, 252));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        authBox.add(headerTitle, gbc);

        JLabel subCaption = new JLabel("Next-Gen Campus Emergency Dispatch & Authentication", SwingConstants.CENTER);
        subCaption.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subCaption.setForeground(new Color(203, 213, 225));
        gbc.gridy = 1;
        authBox.add(subCaption, gbc);

        gbc.gridwidth = 1; gbc.gridy = 2; gbc.gridx = 0;
        authBox.add(makeStyledLabel("User ID / Enrollment:", 13, true, new Color(226, 232, 240)), gbc);
        JTextField idField = new JTextField(18);
        applyDarkFieldTheme(idField);
        gbc.gridx = 1;
        authBox.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        authBox.add(makeStyledLabel("Password:", 13, true, new Color(226, 232, 240)), gbc);
        JPasswordField passField = new JPasswordField(18);
        applyDarkFieldTheme(passField);
        gbc.gridx = 1;
        authBox.add(passField, gbc);

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 12, 0));
        actionRow.setOpaque(false);
        PillLiquidButton loginBtn = new PillLiquidButton("Secure Login", new Color(2, 132, 199), new Color(14, 165, 233));
        PillLiquidButton signupBtn = new PillLiquidButton("Register User", new Color(5, 150, 105), new Color(16, 185, 129));
        actionRow.add(loginBtn);
        actionRow.add(signupBtn);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        authBox.add(actionRow, gbc);

        PillLiquidButton demoAdminBtn = new PillLiquidButton("Quick Demo Login (Pankaj Sir - Admin)", new Color(217, 119, 6), new Color(245, 158, 11));
        gbc.gridy = 5;
        authBox.add(demoAdminBtn, gbc);

        loginBtn.addActionListener(evt -> {
            try {
                activeSessionUser = backend.authenticateUser(idField.getText(), new String(passField.getPassword()));
                activeSessionUser.displayDashboardRole();
                sessionHeaderTag.setText("Active Operator: " + activeSessionUser.getName() + " [" + activeSessionUser.getRole() + "]");
                idField.setText("");
                passField.setText("");
                screenSwitcher.show(rootContainer, "MAIN_VIEW");
                syncTablesAndMetrics();
            } catch (Exception ex) {
                if ("BANNED_ACCOUNT".equals(ex.getMessage())) {
                    String banNotice = "🚨 ACCOUNT SUSPENDED 🚨\n\n"
                                     + "Your ID has accumulated 3 or more False Alarm strikes.\n"
                                     + "Access to ResQOPS has been permanently revoked.\n"
                                     + "Please report to Campus Security Control.";
                    JOptionPane.showMessageDialog(this, banNotice, "Security Ban Enforced", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Authentication Failed", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        signupBtn.addActionListener(evt -> {
            JPanel regForm = new JPanel(new GridLayout(4, 2, 10, 10));
            JTextField fId = new JTextField();
            JPasswordField fPass = new JPasswordField();
            JTextField fName = new JTextField();
            JComboBox<String> fRole = new JComboBox<>(new String[]{"STUDENT", "FACULTY", "SECURITY_OFFICER"});
            regForm.add(new JLabel("User ID:")); regForm.add(fId);
            regForm.add(new JLabel("Password:")); regForm.add(fPass);
            regForm.add(new JLabel("Full Name:")); regForm.add(fName);
            regForm.add(new JLabel("Role:")); regForm.add(fRole);

            int choice = JOptionPane.showConfirmDialog(this, regForm, "Create New Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice == JOptionPane.OK_OPTION) {
                try {
                    String pickedRole = String.valueOf(fRole.getSelectedItem());
                    String rawPass = new String(fPass.getPassword());
                    User created;
                    if ("FACULTY".equals(pickedRole)) created = new Faculty(fId.getText(), rawPass, fName.getText(), "Dept");
                    else if ("SECURITY_OFFICER".equals(pickedRole)) created = new SecurityOfficer(fId.getText(), rawPass, fName.getText(), "Badge");
                    else created = new Student(fId.getText(), rawPass, fName.getText(), "Enrollment");

                    backend.registerUser(created);
                    JOptionPane.showMessageDialog(this, "Account registered successfully! You can now sign in.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        demoAdminBtn.addActionListener(evt -> {
            try {
                activeSessionUser = backend.authenticateUser("pankaj", "pankaj123");
                sessionHeaderTag.setText("Active Operator: " + activeSessionUser.getName() + " [" + activeSessionUser.getRole() + "]");
                screenSwitcher.show(rootContainer, "MAIN_VIEW");
                syncTablesAndMetrics();
            } catch (Exception ignored) { }
        });

        bgCanvas.add(authBox);
        return bgCanvas;
    }

    private JPanel buildMetricTile(String heading, JLabel counterView, Color accent) {
        Color fillAlpha = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30);
        Color borderAlpha = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140);
        FrostedGlassCard tile = new FrostedGlassCard(new BorderLayout(5, 5), 22, fillAlpha, borderAlpha);
        tile.setBorder(new EmptyBorder(14, 22, 14, 22));

        JLabel titleLbl = new JLabel(heading, SwingConstants.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLbl.setForeground(new Color(226, 232, 240));

        counterView.setFont(new Font("SansSerif", Font.BOLD, 30));
        counterView.setForeground(accent.brighter());

        tile.add(titleLbl, BorderLayout.NORTH);
        tile.add(counterView, BorderLayout.CENTER);
        return tile;
    }

    private JPanel constructMainWorkspace() {
        AmbientLiquidBackdrop workspace = new AmbientLiquidBackdrop(new BorderLayout(12, 12));
        workspace.setBorder(new EmptyBorder(15, 18, 15, 18));

        // Top Navigation Bar
        FrostedGlassCard navBar = new FrostedGlassCard(
                new BorderLayout(), 20, new Color(255, 255, 255, 18), new Color(255, 255, 255, 50));
        navBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel leftBrandStack = new JPanel(new GridLayout(2, 1));
        leftBrandStack.setOpaque(false);
        JLabel mainHeading = makeStyledLabel("ResQOPS Liquid Command Center", 22, true, Color.WHITE);
        sessionHeaderTag.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sessionHeaderTag.setForeground(new Color(148, 163, 184));
        leftBrandStack.add(mainHeading);
        leftBrandStack.add(sessionHeaderTag);

        JLabel liveClockLbl = makeStyledLabel("🕒 Syncing...", 15, true, new Color(125, 211, 252));
        PillLiquidButton logoutBtn = new PillLiquidButton("Sign Out", new Color(71, 85, 105), new Color(100, 116, 139));
        logoutBtn.addActionListener(e -> {
            activeSessionUser = null;
            screenSwitcher.show(rootContainer, "AUTH_VIEW");
        });

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 2));
        rightControls.setOpaque(false);
        rightControls.add(liveClockLbl);
        rightControls.add(logoutBtn);

        navBar.add(leftBrandStack, BorderLayout.WEST);
        navBar.add(rightControls, BorderLayout.EAST);
        workspace.add(navBar, BorderLayout.NORTH);

        // Daemon Thread for Clock & Periodic Refresh
        Thread clockWorker = new Thread(() -> {
            SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
            int secCount = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    secCount++;
                    final boolean doSync = (secCount % 5 == 0);
                    SwingUtilities.invokeLater(() -> {
                        liveClockLbl.setText("🕒 " + fmt.format(new Date()));
                        if (doSync && activeSessionUser != null) syncTablesAndMetrics();
                    });
                } catch (Exception ignored) { }
            }
        });
        clockWorker.setDaemon(true);
        clockWorker.start();

        JTabbedPane navTabs = new JTabbedPane();
        navTabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        navTabs.setOpaque(false);

        // --- TAB 1: INCIDENT REPORTING & SOS ---
        JPanel tab1Holder = new JPanel(new GridBagLayout());
        tab1Holder.setOpaque(false);

        FrostedGlassCard formCard = new FrostedGlassCard(
                new GridBagLayout(), 26, new Color(255, 255, 255, 22), new Color(255, 255, 255, 65));
        formCard.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints fc = new GridBagConstraints();
        fc.insets = new Insets(10, 10, 10, 10);
        fc.fill = GridBagConstraints.HORIZONTAL;

        PillLiquidButton panicBtn = new PillLiquidButton("🚨 ONE-CLICK CRITICAL SOS PANIC 🚨", new Color(220, 38, 38), new Color(239, 68, 68));
        panicBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        panicBtn.setPreferredSize(new Dimension(430, 50));
        fc.gridx = 0; fc.gridy = 0; fc.gridwidth = 2;
        formCard.add(panicBtn, fc);

        JSeparator divider = new JSeparator();
        divider.setForeground(new Color(255, 255, 255, 50));
        fc.gridy = 1;
        formCard.add(divider, fc);

        fc.gridwidth = 1; fc.gridy = 2; fc.gridx = 0;
        formCard.add(makeStyledLabel("Emergency Category:", 13, true, Color.WHITE), fc);
        JComboBox<String> categorySelect = new JComboBox<>(new String[]{"Fire", "Medical", "Security", "Electrical"});
        fc.gridx = 1;
        formCard.add(categorySelect, fc);

        fc.gridy = 3; fc.gridx = 0;
        formCard.add(makeStyledLabel("Exact Campus Location:", 13, true, Color.WHITE), fc);
        JTextField locInput = new JTextField("Academic Block 2", 22);
        applyDarkFieldTheme(locInput);
        fc.gridx = 1;
        formCard.add(locInput, fc);

        fc.gridy = 4; fc.gridx = 0;
        formCard.add(makeStyledLabel("Severity Assessment:", 13, true, Color.WHITE), fc);
        JComboBox<SeverityLevel> sevSelect = new JComboBox<>(SeverityLevel.values());
        sevSelect.setSelectedItem(SeverityLevel.HIGH);
        fc.gridx = 1;
        formCard.add(sevSelect, fc);

        fc.gridy = 5; fc.gridx = 0;
        formCard.add(makeStyledLabel("Situation Description:", 13, true, Color.WHITE), fc);
        JTextField descInput = new JTextField(22);
        applyDarkFieldTheme(descInput);
        fc.gridx = 1;
        formCard.add(descInput, fc);

        PillLiquidButton fileReportBtn = new PillLiquidButton("Submit Incident Report", new Color(2, 132, 199), new Color(14, 165, 233));
        fileReportBtn.setPreferredSize(new Dimension(430, 42));
        fc.gridy = 6; fc.gridx = 0; fc.gridwidth = 2;
        formCard.add(fileReportBtn, fc);

        tab1Holder.add(formCard);

        panicBtn.addActionListener(e -> {
            try {
                String spot = locInput.getText().trim().isEmpty() ? "Campus Central Zone" : locInput.getText().trim();
                Emergency sosTicket = backend.buildIncident("Security", activeSessionUser.getUserId(), spot, "CRITICAL SOS PANIC BUTTON ACTIVATED", SeverityLevel.CRITICAL_SOS);
                backend.enqueueIncident(sosTicket);
                sosTicket.displayImmediateSteps();
                syncTablesAndMetrics();
                if (!"STUDENT".equalsIgnoreCase(activeSessionUser.getRole())) navTabs.setSelectedIndex(1);
                else JOptionPane.showMessageDialog(this, "SOS Alert Broadcasted! Ticket ID: " + sosTicket.getEmergencyId());
            } catch (Exception ignored) { }
        });

        fileReportBtn.addActionListener(e -> {
            try {
                String cat = String.valueOf(categorySelect.getSelectedItem());
                SeverityLevel lvl = (SeverityLevel) sevSelect.getSelectedItem();
                Emergency newTicket = backend.buildIncident(cat, activeSessionUser.getUserId(), locInput.getText(), descInput.getText(), lvl);
                backend.enqueueIncident(newTicket);
                newTicket.displayImmediateSteps();
                descInput.setText("");
                syncTablesAndMetrics();
                if (!"STUDENT".equalsIgnoreCase(activeSessionUser.getRole())) navTabs.setSelectedIndex(1);
                else JOptionPane.showMessageDialog(this, "Report Logged! Ticket ID: " + newTicket.getEmergencyId());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- TAB 2: LIVE CONTROL ROOM ---
        FrostedGlassCard adminView = new FrostedGlassCard(
                new BorderLayout(12, 12), 24, new Color(255, 255, 255, 16), new Color(255, 255, 255, 45));
        adminView.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel metricsStrip = new JPanel(new GridLayout(1, 3, 20, 0));
        metricsStrip.setOpaque(false);
        metricsStrip.add(buildMetricTile("ACTIVE EMERGENCIES", metricActive, new Color(248, 113, 113)));
        metricsStrip.add(buildMetricTile("RESOLVED INCIDENTS", metricResolved, new Color(52, 211, 153)));
        metricsStrip.add(buildMetricTile("FALSE ALARMS LOGGED", metricFalse, new Color(251, 191, 36)));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterBar.setOpaque(false);
        filterBar.add(makeStyledLabel("🔍 Live Filter (Location / Type / ID): ", 13, true, new Color(226, 232, 240)));
        JTextField filterInput = new JTextField(25);
        applyDarkFieldTheme(filterInput);
        filterBar.add(filterInput);

        JPanel adminNorthBox = new JPanel(new BorderLayout(0, 12));
        adminNorthBox.setOpaque(false);
        adminNorthBox.add(metricsStrip, BorderLayout.NORTH);
        adminNorthBox.add(filterBar, BorderLayout.SOUTH);
        adminView.add(adminNorthBox, BorderLayout.NORTH);

        JTable liveGrid = new JTable(liveTableModel);
        configureGlassTable(liveGrid);
        TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(liveTableModel);
        liveGrid.setRowSorter(rowSorter);

        liveGrid.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                Component cell = super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                if (!sel) {
                    int modelRow = tbl.convertRowIndexToModel(r);
                    String sevText = String.valueOf(tbl.getModel().getValueAt(modelRow, 4));
                    if ("CRITICAL_SOS".equals(sevText)) {
                        cell.setBackground(new Color(153, 27, 27));
                        cell.setForeground(Color.WHITE);
                    } else if ("HIGH".equals(sevText)) {
                        cell.setBackground(new Color(146, 64, 14));
                        cell.setForeground(Color.WHITE);
                    } else {
                        cell.setBackground(new Color(15, 23, 42));
                        cell.setForeground(new Color(226, 232, 240));
                    }
                }
                return cell;
            }
        });

        JScrollPane liveScroll = new JScrollPane(liveGrid);
        liveScroll.getViewport().setBackground(new Color(15, 23, 42));
        liveScroll.setBorder(new LineBorder(new Color(255, 255, 255, 45), 1, true));
        adminView.add(liveScroll, BorderLayout.CENTER);

        filterInput.getDocument().addDocumentListener(new DocumentListener() {
            private void applySearch() {
                String q = filterInput.getText().trim();
                rowSorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + q));
            }
            @Override public void insertUpdate(DocumentEvent e) { applySearch(); }
            @Override public void removeUpdate(DocumentEvent e) { applySearch(); }
            @Override public void changedUpdate(DocumentEvent e) { applySearch(); }
        });

        JPanel controlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        controlBtns.setOpaque(false);
        PillLiquidButton dispatchBtn = new PillLiquidButton("Dispatch Response Team", new Color(2, 132, 199), new Color(14, 165, 233));
        PillLiquidButton resolveBtn = new PillLiquidButton("Mark Resolved", new Color(5, 150, 105), new Color(16, 185, 129));
        PillLiquidButton strikeBtn = new PillLiquidButton("Flag False Alarm (+1 Strike)", new Color(220, 38, 38), new Color(239, 68, 68));
        controlBtns.add(dispatchBtn);
        controlBtns.add(resolveBtn);
        controlBtns.add(strikeBtn);
        adminView.add(controlBtns, BorderLayout.SOUTH);

        ActionListener commandHandler = evt -> {
            int selectedRow = liveGrid.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select an active incident row first.");
                return;
            }
            String ticketId = String.valueOf(liveGrid.getValueAt(selectedRow, 0));

            if (evt.getSource() == dispatchBtn) {
                try {
                    backend.assignResponderUnit(ticketId);
                    JOptionPane.showMessageDialog(this, "Specialized Campus Response Unit dispatched to " + ticketId);
                } catch (ResponderUnavailableException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Dispatch Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (evt.getSource() == resolveBtn) {
                String resMsg = backend.closeAndArchiveTicket(ticketId, false, activeSessionUser.getName());
                JOptionPane.showMessageDialog(this, resMsg, "Ticket Closed", JOptionPane.INFORMATION_MESSAGE);
            } else if (evt.getSource() == strikeBtn) {
                String spamMsg = backend.closeAndArchiveTicket(ticketId, true, activeSessionUser.getName());
                JOptionPane.showMessageDialog(this, spamMsg + "\nPenalty Strike recorded against reporter.", "False Alarm Logged", JOptionPane.WARNING_MESSAGE);
            }
            syncTablesAndMetrics();
        };
        dispatchBtn.addActionListener(commandHandler);
        resolveBtn.addActionListener(commandHandler);
        strikeBtn.addActionListener(commandHandler);

        // --- TAB 3: EXTERNAL DIRECTORY & SHORTEST GOOGLE MAPS ROUTE ---
        FrostedGlassCard dirView = new FrostedGlassCard(
                new BorderLayout(15, 15), 24, new Color(255, 255, 255, 16), new Color(255, 255, 255, 45));
        dirView.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel dirHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
        dirHeader.setOpaque(false);
        dirHeader.add(makeStyledLabel("Target External Agency:", 14, true, Color.WHITE));
        JComboBox<String> agencyDropdown = new JComboBox<>(new String[]{"HOSPITAL", "POLICE", "FIRE"});
        PillLiquidButton notifyExtBtn = new PillLiquidButton("📡 Broadcast SOS to Nearest Unit", new Color(220, 38, 38), new Color(239, 68, 68));
        PillLiquidButton gmapsBtn = new PillLiquidButton("🗺️ Open Shortest Route in Google Maps", new Color(5, 150, 105), new Color(16, 185, 129));
        dirHeader.add(agencyDropdown);
        dirHeader.add(notifyExtBtn);
        dirHeader.add(gmapsBtn);

        JTextArea dirDisplay = new JTextArea();
        dirDisplay.setFont(new Font("Monospaced", Font.PLAIN, 15));
        dirDisplay.setBackground(new Color(15, 23, 42));
        dirDisplay.setForeground(new Color(125, 211, 252));
        dirDisplay.setEditable(false);
        dirDisplay.setBorder(new EmptyBorder(18, 18, 18, 18));
        dirDisplay.setText(backend.getDirectory().fetchNearestFacility("HOSPITAL"));

        agencyDropdown.addActionListener(e -> {
            String chosen = String.valueOf(agencyDropdown.getSelectedItem());
            dirDisplay.setText(backend.getDirectory().fetchNearestFacility(chosen));
        });

        notifyExtBtn.addActionListener(e -> {
            String chosen = String.valueOf(agencyDropdown.getSelectedItem());
            String confirmation = backend.getDirectory().notifyExternalService(chosen, locInput.getText());
            JOptionPane.showMessageDialog(this, confirmation, "Nearest External Agency Notified", JOptionPane.WARNING_MESSAGE);
        });

        gmapsBtn.addActionListener(e -> {
            try {
                String chosen = String.valueOf(agencyDropdown.getSelectedItem());
                String targetPlace = backend.getDirectory().getShortestRouteTarget(chosen);
                String mapsUrl = "https://www.google.com/maps/dir/?api=1&origin="
                        + URLEncoder.encode("JIIT Sector 128 Noida", "UTF-8")
                        + "&destination=" + URLEncoder.encode(targetPlace, "UTF-8")
                        + "&travelmode=driving";
                Desktop.getDesktop().browse(new URI(mapsUrl));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unable to launch external browser.", "Map Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JScrollPane dirScroll = new JScrollPane(dirDisplay);
        dirScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(255, 255, 255, 60), 1, true),
                "Emergency Agencies Ranked by Shortest Distance",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13), new Color(226, 232, 240)
        ));
        dirScroll.setOpaque(false);
        dirView.add(dirHeader, BorderLayout.NORTH);
        dirView.add(dirScroll, BorderLayout.CENTER);

        // --- TAB 4: AUDIT ARCHIVES & CSV EXPORT ---
        FrostedGlassCard archiveView = new FrostedGlassCard(
                new BorderLayout(12, 12), 24, new Color(255, 255, 255, 16), new Color(255, 255, 255, 45));
        archiveView.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTable historyGrid = new JTable(historyTableModel);
        configureGlassTable(historyGrid);
        JScrollPane historyScroll = new JScrollPane(historyGrid);
        historyScroll.getViewport().setBackground(new Color(15, 23, 42));
        historyScroll.setBorder(new LineBorder(new Color(255, 255, 255, 45), 1, true));
        archiveView.add(historyScroll, BorderLayout.CENTER);

        JPanel archiveFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        archiveFooter.setOpaque(false);
        PillLiquidButton reloadArchBtn = new PillLiquidButton("Refresh Logs", new Color(71, 85, 105), new Color(100, 116, 139));
        PillLiquidButton csvExportBtn = new PillLiquidButton("Export Audit Log to CSV", new Color(5, 150, 105), new Color(16, 185, 129));
        archiveFooter.add(reloadArchBtn);
        archiveFooter.add(csvExportBtn);
        archiveView.add(archiveFooter, BorderLayout.SOUTH);

        reloadArchBtn.addActionListener(e -> syncTablesAndMetrics());
        csvExportBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Audit Log as CSV");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outFile = chooser.getSelectedFile();
                if (!outFile.getName().toLowerCase().endsWith(".csv")) {
                    outFile = new File(outFile.getAbsolutePath() + ".csv");
                }
                try (PrintWriter csvOut = new PrintWriter(new FileWriter(outFile))) {
                    csvOut.println("Incident ID,Type,Location,Final Status,Verified By,Timestamp");
                    for (int r = 0; r < historyTableModel.getRowCount(); r++) {
                        List<String> cells = new ArrayList<>();
                        for (int c = 0; c < historyTableModel.getColumnCount(); c++) {
                            cells.add(String.valueOf(historyTableModel.getValueAt(r, c)));
                        }
                        csvOut.println(String.join(",", cells));
                    }
                    JOptionPane.showMessageDialog(this, "Audit Log exported successfully to:\n" + outFile.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to export CSV file.", "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        navTabs.addTab("  Report Incident  ", tab1Holder);
        navTabs.addTab("  Live Control Room  ", adminView);
        navTabs.addTab("  External Directory  ", dirView);
        navTabs.addTab("  Audit Archives  ", archiveView);
        workspace.add(navTabs, BorderLayout.CENTER);

        navTabs.addChangeListener(e -> {
            if (activeSessionUser != null && "STUDENT".equalsIgnoreCase(activeSessionUser.getRole())) {
                int tabIdx = navTabs.getSelectedIndex();
                if (tabIdx == 1 || tabIdx == 3) {
                    JOptionPane.showMessageDialog(this, "Access Denied: Administrative & Security clearance required.", "Restricted Area", JOptionPane.WARNING_MESSAGE);
                    navTabs.setSelectedIndex(0);
                }
            }
        });

        return workspace;
    }

    private void syncTablesAndMetrics() {
        liveTableModel.setRowCount(0);
        for (Emergency item : backend.getActiveIncidents()) {
            liveTableModel.addRow(new Object[]{
                    item.getEmergencyId(),
                    item.getClass().getSimpleName(),
                    item.getReporterId(),
                    item.getLocation(),
                    item.getSeverity(),
                    item.getStatus(),
                    item.getAssignedResponder()
            });
        }
        metricActive.setText(String.valueOf(backend.getActiveIncidents().size()));

        if (activeSessionUser != null && !"STUDENT".equalsIgnoreCase(activeSessionUser.getRole())) {
            historyTableModel.setRowCount(0);
            File logFile = new File("resqops_incident_archive.txt");
            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] cols = line.split(",");
                        if (cols.length >= 6) {
                            historyTableModel.addRow(new Object[]{cols[0], cols[1], cols[2], cols[3], cols[4], cols[5]});
                        }
                    }
                } catch (Exception ignored) { }
            }
            metricResolved.setText(String.valueOf(backend.resolvedCounter));
            metricFalse.setText(String.valueOf(backend.falseAlarmCounter));
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo lnf : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(lnf.getName())) {
                    UIManager.setLookAndFeel(lnf.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new SmartCampusEmergencySystem().setVisible(true));
    }
}
