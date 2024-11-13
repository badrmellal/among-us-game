package event;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

public class Door {
    // Door properties
    private Point2D.Double position;
    private double width;
    private double height;
    private boolean isVertical;
    private boolean isOpen;
    private boolean isSabotaged;

    // Animation properties
    private double openProgress;
    private double targetProgress;
    private static final double ANIMATION_SPEED = 0.1;

    // Visual properties
    private Color doorColor;
    private static final Color NORMAL_COLOR = new Color(100, 100, 120);
    private static final Color SABOTAGED_COLOR = new Color(170, 50, 50);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 30);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 50);

    // Malfunction effect
    private Random random;
    private double malfunctionOffset;

    public Door(double x, double y, boolean isVertical) {
        this.position = new Point2D.Double(x, y);
        this.isVertical = isVertical;
        this.width = isVertical ? 10 : 80;
        this.height = isVertical ? 80 : 10;
        this.isOpen = true;
        this.isSabotaged = false;
        this.openProgress = 1.0;
        this.targetProgress = 1.0;
        this.doorColor = NORMAL_COLOR;
        this.random = new Random();
        this.malfunctionOffset = 0;
    }

    public void update() {
        // Update door animation
        if (openProgress != targetProgress) {
            double diff = targetProgress - openProgress;
            openProgress += Math.signum(diff) * ANIMATION_SPEED;

            // Clamp progress
            if (Math.abs(diff) < ANIMATION_SPEED) {
                openProgress = targetProgress;
            }
        }

        // Update malfunction effect when sabotaged
        if (isSabotaged) {
            malfunctionOffset = (random.nextDouble() - 0.5) * 2;
        } else {
            malfunctionOffset = 0;
        }

        // Update door color
        doorColor = isSabotaged ? SABOTAGED_COLOR : NORMAL_COLOR;
    }

    public void draw(Graphics2D g2d) {
        // Save the original transform
        AffineTransform originalTransform = g2d.getTransform();

        // Calculate door position with malfunction effect
        double drawX = position.x + (isVertical ? malfunctionOffset : 0);
        double drawY = position.y + (isVertical ? 0 : malfunctionOffset);

        // Draw door shadow
        g2d.setColor(SHADOW_COLOR);
        drawDoorShape(g2d, drawX + 2, drawY + 2);

        // Draw main door
        g2d.setColor(doorColor);
        drawDoorShape(g2d, drawX, drawY);

        // Draw door details
        drawDoorDetails(g2d, drawX, drawY);

        // Draw status effects
        if (isSabotaged) {
            drawSabotageEffect(g2d, drawX, drawY);
        }

        // Draw interaction highlight if door is interactive
        if (isInteractive()) {
            drawInteractionHighlight(g2d, drawX, drawY);
        }

        // Restore the original transform
        g2d.setTransform(originalTransform);
    }

    private void drawDoorShape(Graphics2D g2d, double x, double y) {
        if (isVertical) {
            // Vertical door slides left/right
            double slideOffset = (1 - openProgress) * width;
            g2d.fill(new Rectangle2D.Double(
                    x - slideOffset,
                    y,
                    width,
                    height
            ));
        } else {
            // Horizontal door slides up/down
            double slideOffset = (1 - openProgress) * height;
            g2d.fill(new Rectangle2D.Double(
                    x,
                    y - slideOffset,
                    width,
                    height
            ));
        }
    }

    private void drawDoorDetails(Graphics2D g2d, double x, double y) {
        // Draw door panels
        g2d.setColor(HIGHLIGHT_COLOR);
        if (isVertical) {
            double panelHeight = height / 4;
            for (int i = 0; i < 4; i++) {
                g2d.draw(new Rectangle2D.Double(
                        x + 2,
                        y + i * panelHeight + 2,
                        width - 4,
                        panelHeight - 4
                ));
            }
        } else {
            double panelWidth = width / 4;
            for (int i = 0; i < 4; i++) {
                g2d.draw(new Rectangle2D.Double(
                        x + i * panelWidth + 2,
                        y + 2,
                        panelWidth - 4,
                        height - 4
                ));
            }
        }
    }

    private void drawSabotageEffect(Graphics2D g2d, double x, double y) {
        // Draw warning stripes
        g2d.setColor(new Color(255, 0, 0, 50));
        double stripeSize = 10;
        int numStripes = isVertical ?
                (int)(height / stripeSize) :
                (int)(width / stripeSize);

        for (int i = 0; i < numStripes; i++) {
            if (i % 2 == 0) {
                if (isVertical) {
                    g2d.fill(new Rectangle2D.Double(
                            x,
                            y + i * stripeSize,
                            width,
                            stripeSize
                    ));
                } else {
                    g2d.fill(new Rectangle2D.Double(
                            x + i * stripeSize,
                            y,
                            stripeSize,
                            height
                    ));
                }
            }
        }

        // Draw electrical effect
        if (random.nextInt(100) < 20) { // 20% chance per frame
            g2d.setColor(Color.YELLOW);
            for (int i = 0; i < 3; i++) {
                double startX = x + random.nextDouble() * width;
                double startY = y + random.nextDouble() * height;
                double endX = startX + random.nextDouble() * 10 - 5;
                double endY = startY + random.nextDouble() * 10 - 5;
                g2d.draw(new Line2D.Double(startX, startY, endX, endY));
            }
        }
    }

    private void drawInteractionHighlight(Graphics2D g2d, double x, double y) {
        // Draw subtle highlight around the door
        g2d.setColor(new Color(255, 255, 255, 20));
        double padding = 2;
        g2d.draw(new Rectangle2D.Double(
                x - padding,
                y - padding,
                width + padding * 2,
                height + padding * 2
        ));
    }

    public void toggle() {
        if (!isSabotaged) {
            targetProgress = isOpen ? 0.0 : 1.0;
            isOpen = !isOpen;
        }
    }

    public void sabotage() {
        isSabotaged = true;
        targetProgress = 0.0;
        isOpen = false;
    }

    public void repair() {
        if (isSabotaged) {
            isSabotaged = false;
            targetProgress = 1.0;
            isOpen = true;
        }
    }

    public boolean isInteractive() {
        return !isSabotaged || isNearby(); // Can only interact if not sabotaged or if nearby for repair
    }

    public boolean isNearby() {
        // This should be implemented to check if a player is near the door
        // You would typically pass in the player's position and check the distance
        return true; // Placeholder implementation
    }

    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(
                position.x,
                position.y,
                width,
                height
        );
    }

    public boolean containsPoint(Point2D.Double point) {
        return getBounds().contains(point);
    }

    // Getters
    public boolean isOpen() { return isOpen; }
    public boolean isSabotaged() { return isSabotaged; }
    public Point2D.Double getPosition() { return position; }
}