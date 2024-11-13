package event;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class WiringTaskPanel extends JPanel {
    private TaskManager taskManager;
    private List<Wire> leftWires;
    private List<Wire> rightWires;
    private Wire selectedWire;
    private Point dragPoint;
    private List<WireConnection> connections;
    private boolean isCompleted;

    // Colors for wires
    private static final Color[] WIRE_COLORS = {
            new Color(255, 0, 0),    // Red
            new Color(0, 255, 0),    // Green
            new Color(0, 0, 255),    // Blue
            new Color(255, 255, 0),  // Yellow
    };

    public WiringTaskPanel(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.connections = new ArrayList<>();
        this.isCompleted = false;

        setPreferredSize(new Dimension(400, 300));
        initializeWires();
        setupMouseListeners();
    }

    private void initializeWires() {
        leftWires = new ArrayList<>();
        rightWires = new ArrayList<>();

        // Create wires with random colors
        List<Color> colors = new ArrayList<>(Arrays.asList(WIRE_COLORS));
        Collections.shuffle(colors);

        int wireSpacing = 50;
        int startY = 50;

        // Create left wires
        for (int i = 0; i < 4; i++) {
            leftWires.add(new Wire(
                    50,
                    startY + i * wireSpacing,
                    colors.get(i),
                    true
            ));
        }

        // Shuffle colors again for right wires
        Collections.shuffle(colors);

        // Create right wires
        for (int i = 0; i < 4; i++) {
            rightWires.add(new Wire(
                    350,
                    startY + i * wireSpacing,
                    colors.get(i),
                    false
            ));
        }
    }

    private void setupMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e.getPoint());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable antialiasing
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // Draw background
        g2d.setColor(new Color(40, 40, 40));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw panel details
        drawPanelDetails(g2d);

        // Draw completed connections
        for (WireConnection connection : connections) {
            drawConnection(g2d, connection);
        }

        // Draw active drag connection
        if (selectedWire != null && dragPoint != null) {
            g2d.setColor(selectedWire.color);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new Line2D.Double(
                    selectedWire.x,
                    selectedWire.y,
                    dragPoint.x,
                    dragPoint.y
            ));
        }

        // Draw all wires
        for (Wire wire : leftWires) {
            wire.draw(g2d);
        }
        for (Wire wire : rightWires) {
            wire.draw(g2d);
        }
    }

    private void drawPanelDetails(Graphics2D g2d) {
        // Draw panel border
        g2d.setColor(new Color(70, 70, 70));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(10, 10, getWidth() - 20, getHeight() - 20);

        // Draw screws in corners
        int screwSize = 8;
        int margin = 15;
        g2d.setColor(new Color(100, 100, 100));
        drawScrew(g2d, margin, margin, screwSize);
        drawScrew(g2d, getWidth() - margin, margin, screwSize);
        drawScrew(g2d, margin, getHeight() - margin, screwSize);
        drawScrew(g2d, getWidth() - margin, getHeight() - margin, screwSize);

        // Draw task title
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String title = "Fix Wiring";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title,
                (getWidth() - fm.stringWidth(title)) / 2,
                30
        );
    }

    private void drawScrew(Graphics2D g2d, int x, int y, int size) {
        g2d.fillOval(x - size/2, y - size/2, size, size);
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawLine(
                x - size/4, y - size/4,
                x + size/4, y + size/4
        );
        g2d.drawLine(
                x - size/4, y + size/4,
                x + size/4, y - size/4
        );
    }

    private void drawConnection(Graphics2D g2d, WireConnection connection) {
        g2d.setColor(connection.wire1.color);
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Draw wire with a slight curve
        Path2D.Double path = new Path2D.Double();
        path.moveTo(connection.wire1.x, connection.wire1.y);

        double controlX = (connection.wire1.x + connection.wire2.x) / 2;
        path.curveTo(
                controlX, connection.wire1.y,
                controlX, connection.wire2.y,
                connection.wire2.x, connection.wire2.y
        );

        g2d.draw(path);

        // Draw spark effect when connection is made
        if (connection.sparkTime > 0) {
            drawSparkEffect(g2d, connection);
            connection.sparkTime--;
        }
    }

    private void drawSparkEffect(Graphics2D g2d, WireConnection connection) {
        Random rand = new Random();
        int sparkCount = 5;
        double midX = (connection.wire1.x + connection.wire2.x) / 2;
        double midY = (connection.wire1.y + connection.wire2.y) / 2;

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));

        for (int i = 0; i < sparkCount; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double length = rand.nextDouble() * 10 + 5;
            g2d.draw(new Line2D.Double(
                    midX,
                    midY,
                    midX + Math.cos(angle) * length,
                    midY + Math.sin(angle) * length
            ));
        }
    }

    private void handleMousePressed(Point point) {
        // Check left wires
        selectedWire = findWire(leftWires, point);
        if (selectedWire == null) {
            // Check right wires
            selectedWire = findWire(rightWires, point);
        }

        if (selectedWire != null) {
            dragPoint = point;
            // Remove any existing connection for this wire
            connections.removeIf(c ->
                    c.wire1 == selectedWire || c.wire2 == selectedWire
            );
            repaint();
        }
    }

    private void handleMouseDragged(Point point) {
        if (selectedWire != null) {
            dragPoint = point;
            repaint();
        }
    }

    private void handleMouseReleased(Point point) {
        if (selectedWire != null) {
            Wire targetWire = null;

            // Find target wire
            if (selectedWire.isLeft) {
                targetWire = findWire(rightWires, point);
            } else {
                targetWire = findWire(leftWires, point);
            }

            // Create connection if valid
            if (targetWire != null && selectedWire.color.equals(targetWire.color)) {
                WireConnection connection = new WireConnection(
                        selectedWire.isLeft ? selectedWire : targetWire,
                        selectedWire.isLeft ? targetWire : selectedWire
                );
                connections.add(connection);

                // Check if all wires are connected
                checkCompletion();
            }

            selectedWire = null;
            dragPoint = null;
            repaint();
        }
    }

    private Wire findWire(List<Wire> wires, Point point) {
        for (Wire wire : wires) {
            if (wire.contains(point)) {
                return wire;
            }
        }
        return null;
    }

    private void checkCompletion() {
        if (connections.size() == 4) {
            boolean allCorrect = connections.stream()
                    .allMatch(c -> c.wire1.color.equals(c.wire2.color));

            if (allCorrect && !isCompleted) {
                isCompleted = true;
                Timer timer = new Timer(1000, e -> {
                    taskManager.completeTask(taskManager.getCurrentTask());
                });
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    // Inner classes
    private static class Wire {
        int x, y;
        Color color;
        boolean isLeft;
        static final int WIRE_RADIUS = 15;

        Wire(int x, int y, Color color, boolean isLeft) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isLeft = isLeft;
        }

        void draw(Graphics2D g2d) {
            // Draw wire end
            g2d.setColor(color);
            g2d.fillOval(
                    x - WIRE_RADIUS,
                    y - WIRE_RADIUS,
                    WIRE_RADIUS * 2,
                    WIRE_RADIUS * 2
            );

            // Draw highlight
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.fillOval(
                    x - WIRE_RADIUS/2,
                    y - WIRE_RADIUS/2,
                    WIRE_RADIUS,
                    WIRE_RADIUS
            );
        }

        boolean contains(Point point) {
            return point.distance(x, y) <= WIRE_RADIUS;
        }
    }

    private static class WireConnection {
        Wire wire1, wire2;
        int sparkTime;

        WireConnection(Wire wire1, Wire wire2) {
            this.wire1 = wire1;
            this.wire2 = wire2;
            this.sparkTime = 10; // Number of frames to show spark effect
        }
    }
}
