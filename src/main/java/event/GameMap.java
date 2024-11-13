package event;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GameMap {
    private List<Room> rooms;
    private List<Vent> vents;
    private List<Door> doors;
    private Map<String, Point2D.Double> spawnPoints;
    private final int mapWidth = 2000;
    private final int mapHeight = 1500;

    // Map theme colors
    private final Color WALL_COLOR = new Color(50, 50, 70);
    private final Color FLOOR_COLOR = new Color(80, 80, 100);
    private final Color SHADOW_COLOR = new Color(0, 0, 0, 50);
    private final Color VENT_COLOR = new Color(100, 100, 100);

    public GameMap() {
        rooms = new ArrayList<>();
        vents = new ArrayList<>();
        doors = new ArrayList<>();
        spawnPoints = new HashMap<>();

        initializeMap();
    }

    private void initializeMap() {
        // Create rooms
        createCafeteria();
        createMedBay();
        createElectrical();
        createStorage();
        createEngineRooms();
        createSecurity();
        createWeapons();
        createShields();
        createAdmin();

        // Add connecting hallways
        createHallways();

        // Add vents
        createVentSystem();

        // Set up spawn points
        setupSpawnPoints();
    }

    private void createCafeteria() {
        Room cafeteria = new Room("Cafeteria", new Rectangle2D.Double(800, 100, 400, 300));
        cafeteria.addFeature(new Rectangle2D.Double(950, 200, 100, 100), "Emergency Button");

        // Add tables
        double tableSize = 60;
        Point2D.Double[] tablePositions = {
                new Point2D.Double(850, 150),
                new Point2D.Double(1050, 150),
                new Point2D.Double(850, 300),
                new Point2D.Double(1050, 300)
        };

        for (Point2D.Double pos : tablePositions) {
            cafeteria.addFeature(
                    new Rectangle2D.Double(
                            pos.x, pos.y, tableSize, tableSize
                    ),
                    "Table"
            );
        }

        rooms.add(cafeteria);
    }

    private void createMedBay() {
        Room medBay = new Room("MedBay", new Rectangle2D.Double(1300, 200, 250, 200));

        // Add scanner
        medBay.addFeature(
                new Rectangle2D.Double(1400, 250, 50, 50),
                "Scanner"
        );

        // Add beds
        double bedWidth = 40;
        double bedHeight = 80;
        medBay.addFeature(
                new Rectangle2D.Double(1330, 220, bedWidth, bedHeight),
                "Bed"
        );
        medBay.addFeature(
                new Rectangle2D.Double(1480, 220, bedWidth, bedHeight),
                "Bed"
        );

        rooms.add(medBay);
    }

    private void createElectrical() {
        Room electrical = new Room("Electrical", new Rectangle2D.Double(400, 600, 300, 250));

        // Add electrical panels
        electrical.addFeature(
                new Rectangle2D.Double(450, 620, 200, 30),
                "Electrical Panel"
        );

        // Add wire task locations
        Point2D.Double[] wirePanels = {
                new Point2D.Double(420, 700),
                new Point2D.Double(520, 700),
                new Point2D.Double(620, 700)
        };

        for (Point2D.Double pos : wirePanels) {
            electrical.addFeature(
                    new Rectangle2D.Double(pos.x, pos.y, 30, 40),
                    "Wire Panel"
            );
        }

        rooms.add(electrical);
    }

    private void createStorage() {
        Room storage = new Room("Storage", new Rectangle2D.Double(800, 800, 400, 300));

        // Add storage boxes
        Random rand = new Random(123); // Fixed seed for consistent layout
        for (int i = 0; i < 8; i++) {
            double x = 820 + rand.nextDouble() * 360;
            double y = 820 + rand.nextDouble() * 260;
            storage.addFeature(
                    new Rectangle2D.Double(x, y, 30, 30),
                    "Box"
            );
        }

        rooms.add(storage);
    }

    private void createEngineRooms() {
        // Upper Engine
        Room upperEngine = new Room("Upper Engine", new Rectangle2D.Double(200, 200, 250, 200));
        upperEngine.addFeature(
                new Rectangle2D.Double(250, 250, 150, 100),
                "Engine"
        );
        rooms.add(upperEngine);

        // Lower Engine
        Room lowerEngine = new Room("Lower Engine", new Rectangle2D.Double(200, 900, 250, 200));
        lowerEngine.addFeature(
                new Rectangle2D.Double(250, 950, 150, 100),
                "Engine"
        );
        rooms.add(lowerEngine);
    }

    private void createSecurity() {
        Room security = new Room("Security", new Rectangle2D.Double(500, 400, 200, 150));

        // Add cameras
        security.addFeature(
                new Rectangle2D.Double(550, 420, 100, 30),
                "Cameras"
        );

        // Add chair
        security.addFeature(
                new Rectangle2D.Double(580, 470, 40, 40),
                "Chair"
        );

        rooms.add(security);
    }

    private void createWeapons() {
        Room weapons = new Room("Weapons", new Rectangle2D.Double(1300, 500, 250, 200));

        // Add weapon console
        weapons.addFeature(
                new Rectangle2D.Double(1350, 550, 150, 50),
                "Weapons Console"
        );

        rooms.add(weapons);
    }

    private void createShields() {
        Room shields = new Room("Shields", new Rectangle2D.Double(1300, 800, 200, 200));

        // Add shield controls
        shields.addFeature(
                new Rectangle2D.Double(1350, 850, 100, 100),
                "Shield Controls"
        );

        rooms.add(shields);
    }

    private void createAdmin() {
        Room admin = new Room("Admin", new Rectangle2D.Double(900, 500, 200, 150));

        // Add admin table
        admin.addFeature(
                new Rectangle2D.Double(950, 550, 100, 50),
                "Admin Table"
        );

        rooms.add(admin);
    }

    private void createHallways() {
        // Vertical hallways
        createHallway(300, 400, 50, 500); // Left vertical
        createHallway(1000, 400, 50, 400); // Center vertical
        createHallway(1400, 400, 50, 400); // Right vertical

        // Horizontal hallways
        createHallway(300, 300, 500, 50); // Upper horizontal
        createHallway(300, 800, 500, 50); // Lower horizontal
        createHallway(1000, 600, 400, 50); // Right horizontal
    }

    private void createHallway(double x, double y, double width, double height) {
        Room hallway = new Room("Hallway", new Rectangle2D.Double(x, y, width, height));
        rooms.add(hallway);
    }

    private void createVentSystem() {
        // Create vents and their connections
        Vent medBayVent = new Vent(1450, 300);
        Vent electricalVent = new Vent(450, 750);
        Vent securityVent = new Vent(550, 450);
        Vent cafeteriaVent = new Vent(850, 200);

        // Connect vents
        medBayVent.addConnection(cafeteriaVent);
        electricalVent.addConnection(securityVent);
        securityVent.addConnection(medBayVent);

        vents.addAll(Arrays.asList(medBayVent, electricalVent, securityVent, cafeteriaVent));
    }

    private void setupSpawnPoints() {
        // Add spawn points for different scenarios
        spawnPoints.put("game_start", new Point2D.Double(1000, 250)); // Cafeteria
        spawnPoints.put("emergency", new Point2D.Double(1000, 250)); // Cafeteria
        spawnPoints.put("ghost", new Point2D.Double(1000, 400)); // Center of map
    }

    public void draw(Graphics2D g2d) {
        // Draw background
        g2d.setColor(FLOOR_COLOR);
        g2d.fillRect(0, 0, mapWidth, mapHeight);

        // Draw rooms
        for (Room room : rooms) {
            // Draw room shadow
            g2d.setColor(SHADOW_COLOR);
            Rectangle2D bounds = room.getBounds();
            g2d.fill(new Rectangle2D.Double(
                    bounds.getX() + 5,
                    bounds.getY() + 5,
                    bounds.getWidth(),
                    bounds.getHeight()
            ));

            // Draw room
            g2d.setColor(WALL_COLOR);
            g2d.fill(bounds);

            // Draw room features
            room.drawFeatures(g2d);
        }

        // Draw vents
        g2d.setColor(VENT_COLOR);
        for (Vent vent : vents) {
            vent.draw(g2d);
        }

        // Draw doors
        for (Door door : doors) {
            door.draw(g2d);
        }
    }

    public Room getRoomAt(Point2D.Double position) {
        for (Room room : rooms) {
            if (room.getBounds().contains(position)) {
                return room;
            }
        }
        return null;
    }

    public Vent getVentAt(Point2D.Double position) {
        for (Vent vent : vents) {
            if (vent.getBounds().contains(position)) {
                return vent;
            }
        }
        return null;
    }

    public Point2D.Double getSpawnPoint(String type) {
        return spawnPoints.getOrDefault(type, spawnPoints.get("game_start"));
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public int getWidth() {
        return mapWidth;
    }

    public int getHeight() {
        return mapHeight;
    }
}
