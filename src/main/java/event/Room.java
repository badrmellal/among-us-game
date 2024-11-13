package event;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Room {
    private String name;
    private Rectangle2D.Double bounds;
    private List<RoomFeature> features;
    private boolean lightsOn;
    private double temperature;

    // Visual properties
    private Color floorColor;
    private Color wallColor;
    private static final Color DARK_COLOR = new Color(30, 30, 40);
    private static final Color LIGHT_COLOR = new Color(200, 200, 220);

    public Room(String name, Rectangle2D.Double bounds) {
        this.name = name;
        this.bounds = bounds;
        this.features = new ArrayList<>();
        this.lightsOn = true;
        this.temperature = 20.0; // Default room temperature

        // Set default colors
        this.floorColor = new Color(80, 80, 100);
        this.wallColor = new Color(60, 60, 80);
    }

    public void addFeature(Rectangle2D.Double bounds, String type) {
        features.add(new RoomFeature(bounds, type));
    }

    public void drawFeatures(Graphics2D g2d) {
        // Draw floor pattern
        drawFloorPattern(g2d);

        // Draw features with proper lighting
        for (RoomFeature feature : features) {
            feature.draw(g2d, lightsOn);
        }

        // Draw room effects (steam, electrical sparks, etc.)
        drawRoomEffects(g2d);
    }

    private void drawFloorPattern(Graphics2D g2d) {
        // Create floor pattern
        double tileSize = 40;
        Color baseColor = lightsOn ? floorColor : DARK_COLOR;
        Color altColor = new Color(
                Math.max(0, baseColor.getRed() - 10),
                Math.max(0, baseColor.getGreen() - 10),
                Math.max(0, baseColor.getBlue() - 10)
        );

        for (double x = bounds.x; x < bounds.x + bounds.width; x += tileSize) {
            for (double y = bounds.y; y < bounds.y + bounds.height; y += tileSize) {
                g2d.setColor(((int)(x/tileSize) + (int)(y/tileSize)) % 2 == 0 ? baseColor : altColor);
                g2d.fill(new Rectangle2D.Double(x, y, tileSize, tileSize));
            }
        }
    }

    private void drawRoomEffects(Graphics2D g2d) {
        // Add visual effects based on room type and state
        if (name.equals("Electrical") && !lightsOn) {
            drawElectricalSparks(g2d);
        } else if (name.equals("MedBay")) {
            drawMedicalEffects(g2d);
        } else if (name.contains("Engine")) {
            drawEngineEffects(g2d);
        }
    }

    private void drawElectricalSparks(Graphics2D g2d) {
        // Draw random electrical sparks
        Random rand = new Random();
        if (rand.nextInt(100) < 20) { // 20% chance to show spark
            g2d.setColor(Color.YELLOW);
            double sparkX = bounds.x + rand.nextDouble() * bounds.width;
            double sparkY = bounds.y + rand.nextDouble() * bounds.height;

            for (int i = 0; i < 5; i++) {
                double endX = sparkX + rand.nextDouble() * 20 - 10;
                double endY = sparkY + rand.nextDouble() * 20 - 10;
                g2d.draw(new Line2D.Double(sparkX, sparkY, endX, endY));
            }
        }
    }

    private void drawMedicalEffects(Graphics2D g2d) {
        // Draw subtle healing particles
        Random rand = new Random();
        g2d.setColor(new Color(100, 255, 100, 50));

        for (int i = 0; i < 5; i++) {
            double x = bounds.x + rand.nextDouble() * bounds.width;
            double y = bounds.y + rand.nextDouble() * bounds.height;
            double size = 5 + rand.nextDouble() * 5;

            g2d.fill(new Ellipse2D.Double(x, y, size, size));
        }
    }

    private void drawEngineEffects(Graphics2D g2d) {
        // Draw engine heat waves
        Random rand = new Random();
        g2d.setColor(new Color(255, 100, 0, 30));

        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();

        for (int i = 0; i < 3; i++) {
            double radius = 20 + rand.nextDouble() * 30;
            double offset = Math.sin(System.currentTimeMillis() / 1000.0 + i) * 5;

            g2d.draw(new Ellipse2D.Double(
                    centerX - radius + offset,
                    centerY - radius,
                    radius * 2,
                    radius * 2
            ));
        }
    }

    public void toggleLights() {
        lightsOn = !lightsOn;
    }

    public void setTemperature(double temp) {
        this.temperature = temp;
    }

    public String getName() {
        return name;
    }

    public Rectangle2D.Double getBounds() {
        return bounds;
    }

    public boolean containsPoint(Point2D.Double point) {
        return bounds.contains(point);
    }

    // Inner class for room features
    private static class RoomFeature {
        private Rectangle2D.Double bounds;
        private String type;

        public RoomFeature(Rectangle2D.Double bounds, String type) {
            this.bounds = bounds;
            this.type = type;
        }

        public void draw(Graphics2D g2d, boolean lightsOn) {
            Color featureColor = getFeatureColor(lightsOn);
            g2d.setColor(featureColor);

            switch (type) {
                case "Table":
                    drawTable(g2d);
                    break;
                case "Scanner":
                    drawScanner(g2d);
                    break;
                case "Wire Panel":
                    drawWirePanel(g2d);
                    break;
                case "Box":
                    drawBox(g2d);
                    break;
                default:
                    g2d.fill(bounds);
            }
        }

        private Color getFeatureColor(boolean lightsOn) {
            if (!lightsOn) {
                return DARK_COLOR;
            }

            switch (type) {
                case "Table":
                    return new Color(120, 80, 40);
                case "Scanner":
                    return new Color(200, 200, 220);
                case "Wire Panel":
                    return new Color(100, 100, 120);
                case "Box":
                    return new Color(139, 69, 19);
                default:
                    return Color.GRAY;
            }
        }

        private void drawTable(Graphics2D g2d) {
            // Draw table top
            g2d.fill(bounds);

            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fill(new Rectangle2D.Double(
                    bounds.x + 5,
                    bounds.y + 5,
                    bounds.width,
                    bounds.height
            ));
        }

        private void drawScanner(Graphics2D g2d) {
            // Draw base
            g2d.fill(bounds);

            // Draw scanning effect
            long time = System.currentTimeMillis();
            double scanHeight = (time % 1000) / 1000.0 * bounds.height;

            g2d.setColor(new Color(0, 255, 0, 50));
            g2d.fill(new Rectangle2D.Double(
                    bounds.x,
                    bounds.y + scanHeight,
                    bounds.width,
                    3
            ));
        }

        private void drawWirePanel(Graphics2D g2d) {
            // Draw panel background
            g2d.fill(bounds);

            // Draw wire slots
            g2d.setColor(Color.BLACK);
            double slotHeight = bounds.height / 4;
            for (int i = 0; i < 4; i++) {
                g2d.draw(new Line2D.Double(
                        bounds.x + 5,
                        bounds.y + i * slotHeight + slotHeight/2,
                        bounds.x + bounds.width - 5,
                        bounds.y + i * slotHeight + slotHeight/2
                ));
            }
        }

        private void drawBox(Graphics2D g2d) {
            // Draw main box
            g2d.fill(bounds);

            // Draw highlight
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.draw(new Line2D.Double(
                    bounds.x,
                    bounds.y,
                    bounds.x + bounds.width,
                    bounds.y
            ));
        }
    }
}
