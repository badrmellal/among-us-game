package event;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Vent {
    private Point2D.Double position;
    private List<Vent> connections;
    private boolean isOpen;
    private double animationState;
    private long lastAnimationUpdate;

    // Visual properties
    private static final int VENT_SIZE = 40;
    private static final Color VENT_COLOR = new Color(80, 80, 80);
    private static final Color VENT_SHADOW = new Color(40, 40, 40);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 50);

    // Animation properties
    private static final double ANIMATION_SPEED = 0.1;
    private static final long ANIMATION_INTERVAL = 50; // milliseconds

    public Vent(double x, double y) {
        this.position = new Point2D.Double(x, y);
        this.connections = new ArrayList<>();
        this.isOpen = false;
        this.animationState = 0.0;
        this.lastAnimationUpdate = System.currentTimeMillis();
    }

    public void addConnection(Vent otherVent) {
        if (!connections.contains(otherVent)) {
            connections.add(otherVent);
            // Add reciprocal connection
            if (!otherVent.getConnections().contains(this)) {
                otherVent.addConnection(this);
            }
        }
    }

    public void draw(Graphics2D g2d) {
        // Update animation
        updateAnimation();

        // Create vent shape
        Shape ventShape = createVentShape();

        // Draw shadow
        AffineTransform shadowTransform = AffineTransform.getTranslateInstance(3, 3);
        Shape shadowShape = shadowTransform.createTransformedShape(ventShape);
        g2d.setColor(VENT_SHADOW);
        g2d.fill(shadowShape);

        // Draw main vent
        g2d.setColor(VENT_COLOR);
        g2d.fill(ventShape);

        // Draw vent grates
        drawVentGrates(g2d);

        // Draw highlight
        if (isOpen) {
            drawOpenEffect(g2d);
        }

        // Draw connection indicators
        drawConnectionIndicators(g2d);
    }

    private Shape createVentShape() {
        double x = position.x - VENT_SIZE / 2.0;
        double y = position.y - VENT_SIZE / 2.0;

        if (isOpen) {
            // Create open vent shape with animation
            double openAmount = Math.sin(animationState) * 5;
            Path2D.Double path = new Path2D.Double();
            path.moveTo(x, y);
            path.lineTo(x + VENT_SIZE, y - openAmount);
            path.lineTo(x + VENT_SIZE, y + VENT_SIZE - openAmount);
            path.lineTo(x, y + VENT_SIZE);
            path.closePath();
            return path;
        } else {
            // Create regular vent shape
            return new Rectangle2D.Double(x, y, VENT_SIZE, VENT_SIZE);
        }
    }

    private void drawVentGrates(Graphics2D g2d) {
        double x = position.x - VENT_SIZE / 2.0;
        double y = position.y - VENT_SIZE / 2.0;

        // Draw vent grates
        g2d.setColor(VENT_SHADOW);
        for (int i = 1; i < 4; i++) {
            double grateY = y + (VENT_SIZE * i / 4.0);
            if (isOpen) {
                // Animate grates when vent is open
                double offset = Math.sin(animationState + i) * 3;
                g2d.draw(new Line2D.Double(
                        x + offset, grateY,
                        x + VENT_SIZE + offset, grateY
                ));
            } else {
                g2d.draw(new Line2D.Double(
                        x, grateY,
                        x + VENT_SIZE, grateY
                ));
            }
        }
    }

    private void drawOpenEffect(Graphics2D g2d) {
        double x = position.x - VENT_SIZE / 2.0;
        double y = position.y - VENT_SIZE / 2.0;

        // Create gradient for open effect
        GradientPaint gradient = new GradientPaint(
                (float)x, (float)y,
                new Color(0, 0, 0, 150),
                (float)(x + VENT_SIZE), (float)(y + VENT_SIZE),
                new Color(0, 0, 0, 0)
        );

        g2d.setPaint(gradient);
        g2d.fill(new Rectangle2D.Double(
                x - 5, y - 5,
                VENT_SIZE + 10, VENT_SIZE + 10
        ));

        // Add highlight effect
        double highlightIntensity = (Math.sin(animationState * 2) + 1) / 2;
        g2d.setColor(new Color(255, 255, 255, (int)(highlightIntensity * 50)));
        g2d.draw(new Rectangle2D.Double(
                x, y, VENT_SIZE, VENT_SIZE
        ));
    }

    private void drawConnectionIndicators(Graphics2D g2d) {
        // Draw arrows pointing to connected vents
        if (isOpen) {
            g2d.setColor(HIGHLIGHT_COLOR);
            for (Vent connectedVent : connections) {
                double angle = Math.atan2(
                        connectedVent.getPosition().y - position.y,
                        connectedVent.getPosition().x - position.x
                );

                double arrowX = position.x + Math.cos(angle) * (VENT_SIZE / 2 + 10);
                double arrowY = position.y + Math.sin(angle) * (VENT_SIZE / 2 + 10);

                drawArrow(g2d, position.x, position.y, arrowX, arrowY);
            }
        }
    }

    private void drawArrow(Graphics2D g2d, double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);

        // Draw arrow line
        g2d.draw(new Line2D.Double(x1, y1, x2, y2));

        // Draw arrow head
        double arrowSize = 8;
        double arrowAngle = Math.PI / 6; // 30 degrees

        Path2D.Double arrowHead = new Path2D.Double();
        arrowHead.moveTo(x2, y2);
        arrowHead.lineTo(
                x2 - arrowSize * Math.cos(angle - arrowAngle),
                y2 - arrowSize * Math.sin(angle - arrowAngle)
        );
        arrowHead.lineTo(
                x2 - arrowSize * Math.cos(angle + arrowAngle),
                y2 - arrowSize * Math.sin(angle + arrowAngle)
        );
        arrowHead.closePath();

        g2d.fill(arrowHead);
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimationUpdate > ANIMATION_INTERVAL) {
            animationState += ANIMATION_SPEED;
            lastAnimationUpdate = currentTime;
        }
    }

    public void toggle() {
        isOpen = !isOpen;
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(
                position.x - VENT_SIZE / 2.0,
                position.y - VENT_SIZE / 2.0,
                VENT_SIZE,
                VENT_SIZE
        );
    }

    public Point2D.Double getPosition() {
        return position;
    }

    public List<Vent> getConnections() {
        return connections;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean canConnectTo(Vent otherVent) {
        // Check if vent is within maximum connection distance
        double maxDistance = 500; // Maximum distance for vent connections
        return position.distance(otherVent.getPosition()) <= maxDistance;
    }
}
