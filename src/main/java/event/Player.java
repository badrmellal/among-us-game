package event;

import java.awt.*;
import java.awt.geom.*;
import java.util.UUID;

public class Player {
    // Player properties
    private String id;
    private String name;
    private Color color;
    private boolean isImpostor;
    private boolean isDead;

    // Position and movement
    private double x;
    private double y;
    private double speed;
    private boolean movingUp;
    private boolean movingDown;
    private boolean movingLeft;
    private boolean movingRight;

    // Animation
    private int animationFrame;
    private int animationDelay;
    private long lastAnimationUpdate;
    private Direction facing;

    // Constants
    private static final double DEFAULT_SPEED = 3.0;
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 50;
    private static final Color[] PLAYER_COLORS = {
            new Color(197, 17, 17),    // Red
            new Color(19, 46, 209),    // Blue
            new Color(17, 127, 45),    // Green
            new Color(237, 84, 186),   // Pink
            new Color(239, 125, 13),   // Orange
            new Color(246, 246, 87),   // Yellow
            new Color(63, 71, 78),     // Black
            new Color(215, 225, 241),  // White
            new Color(107, 47, 187),   // Purple
            new Color(113, 73, 30)     // Brown
    };

    public enum Direction {
        LEFT, RIGHT
    }

    public Player(String name, boolean isImpostor) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.isImpostor = isImpostor;
        this.isDead = false;
        this.speed = DEFAULT_SPEED;
        this.color = PLAYER_COLORS[(int)(Math.random() * PLAYER_COLORS.length)];

        // Initialize position
        this.x = 100;
        this.y = 100;

        // Initialize animation
        this.animationFrame = 0;
        this.animationDelay = 100; // milliseconds
        this.lastAnimationUpdate = System.currentTimeMillis();
        this.facing = Direction.RIGHT;
    }

    public void update() {
        // Update position based on movement
        if (movingUp) y -= speed;
        if (movingDown) y += speed;
        if (movingLeft) {
            x -= speed;
            facing = Direction.LEFT;
        }
        if (movingRight) {
            x += speed;
            facing = Direction.RIGHT;
        }

        // Update animation
        if (isMoving() && System.currentTimeMillis() - lastAnimationUpdate > animationDelay) {
            animationFrame = (animationFrame + 1) % 4;
            lastAnimationUpdate = System.currentTimeMillis();
        }
    }

    public void draw(Graphics2D g2d) {
        // Save the original transform
        AffineTransform originalTransform = g2d.getTransform();

        // Apply facing direction
        if (facing == Direction.LEFT) {
            g2d.translate(x + PLAYER_WIDTH, y);
            g2d.scale(-1, 1);
        } else {
            g2d.translate(x, y);
        }

        // Draw the player body
        drawBody(g2d);

        if (!isDead) {
            // Draw the legs
            drawLegs(g2d);

            // Draw accessories (if any)
            if (isImpostor) {
                drawImpostorAccessories(g2d);
            }
        } else {
            // Draw dead body
            drawDeadBody(g2d);
        }

        // Draw name tag
        drawNameTag(g2d);

        // Restore the original transform
        g2d.setTransform(originalTransform);
    }

    private void drawBody(Graphics2D g2d) {
        // Main body
        g2d.setColor(color);
        g2d.fillRoundRect(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT - 10, 10, 10);

        // Backpack
        g2d.fillRoundRect(-5, 10, 10, 20, 5, 5);

        // Visor
        g2d.setColor(new Color(155, 188, 215));
        g2d.fillRoundRect(20, 8, 15, 10, 5, 5);
    }

    private void drawLegs(Graphics2D g2d) {
        g2d.setColor(color);
        if (isMoving()) {
            // Animated legs
            switch (animationFrame) {
                case 0:
                    g2d.fillRect(5, PLAYER_HEIGHT - 10, 10, 10);
                    g2d.fillRect(25, PLAYER_HEIGHT - 10, 10, 10);
                    break;
                case 1:
                    g2d.fillRect(10, PLAYER_HEIGHT - 10, 10, 10);
                    g2d.fillRect(20, PLAYER_HEIGHT - 10, 10, 10);
                    break;
                case 2:
                    g2d.fillRect(15, PLAYER_HEIGHT - 10, 10, 10);
                    g2d.fillRect(15, PLAYER_HEIGHT - 10, 10, 10);
                    break;
                case 3:
                    g2d.fillRect(20, PLAYER_HEIGHT - 10, 10, 10);
                    g2d.fillRect(10, PLAYER_HEIGHT - 10, 10, 10);
                    break;
            }
        } else {
            // Standing still legs
            g2d.fillRect(10, PLAYER_HEIGHT - 10, 8, 10);
            g2d.fillRect(22, PLAYER_HEIGHT - 10, 8, 10);
        }
    }

    private void drawImpostorAccessories(Graphics2D g2d) {
        // Draw knife or other impostor-specific accessories
        g2d.setColor(Color.GRAY);
        if (facing == Direction.RIGHT) {
            g2d.fillRect(PLAYER_WIDTH - 5, 20, 12, 3);
            g2d.fillRect(PLAYER_WIDTH + 2, 18, 3, 7);
        } else {
            g2d.fillRect(-7, 20, 12, 3);
            g2d.fillRect(-5, 18, 3, 7);
        }
    }

    private void drawDeadBody(Graphics2D g2d) {
        // Draw dead body (lying down)
        g2d.setColor(color);
        g2d.rotate(Math.PI / 2, PLAYER_WIDTH / 2, PLAYER_HEIGHT / 2);
        g2d.fillRoundRect(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT - 10, 10, 10);

        // Draw bone
        g2d.setColor(Color.WHITE);
        g2d.fillRect(15, 25, 10, 3);
        g2d.fillRect(18, 22, 4, 9);
    }

    private void drawNameTag(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int nameWidth = fm.stringWidth(name);

        // Draw name background
        g2d.fillRoundRect(
                (PLAYER_WIDTH - nameWidth) / 2 - 2,
                -20,
                nameWidth + 4,
                16,
                5,
                5
        );

        // Draw name text
        g2d.setColor(Color.BLACK);
        g2d.drawString(
                name,
                (PLAYER_WIDTH - nameWidth) / 2,
                -7
        );
    }

    // Movement setters
    public void setMovingUp(boolean movingUp) {
        this.movingUp = movingUp;
    }

    public void setMovingDown(boolean movingDown) {
        this.movingDown = movingDown;
    }

    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }

    // Utility methods
    public boolean isMoving() {
        return movingUp || movingDown || movingLeft || movingRight;
    }

    public void kill() {
        this.isDead = true;
    }

    public boolean isNear(Player other) {
        double distance = Point2D.distance(x, y, other.x, other.y);
        return distance < 50; // Interaction range
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isImpostor() { return isImpostor; }
    public boolean isDead() { return isDead; }
    public double getX() { return x; }
    public double getY() { return y; }
    public Color getColor() { return color; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point2D.Double getPosition() {
        return new Point2D.Double(x, y);
    }
}
