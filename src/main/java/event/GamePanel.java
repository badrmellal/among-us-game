package event;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel {
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;

    private Player localPlayer;
    private List<Player> players;
    private GameMap gameMap;
    private Camera camera;
    private List<Task> tasks;

    // Lighting and FOV
    private boolean isEmergencyLighting;
    private double visionRadius;
    private float lightingAlpha;

    // Visual effects
    private List<ParticleEffect> particles;
    private List<Animation> animations;
    private ImageCache imageCache;

    // Double buffering
    private BufferedImage backBuffer;
    private Graphics2D backBufferGraphics;

    public GamePanel(Player localPlayer, List<Player> players) {
        this.localPlayer = localPlayer;
        // Use CopyOnWriteArrayList for thread safety
        this.players = new CopyOnWriteArrayList<>(players);
        this.gameMap = new GameMap();
        this.camera = new Camera(localPlayer);
        this.tasks = new ArrayList<>();
        this.particles = new CopyOnWriteArrayList<>();
        this.animations = new CopyOnWriteArrayList<>();
        this.imageCache = new ImageCache();

        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        // Initialize vision settings
        visionRadius = 200.0;
        lightingAlpha = 0.7f;
        isEmergencyLighting = false;

        setupTasks();
    }




    private void setupTasks() {
        // Add tasks at specific locations
        tasks.add(new Task("Wires", new Point2D.Double(200, 150), TaskType.WIRES));
        tasks.add(new Task("Upload Data", new Point2D.Double(400, 300), TaskType.UPLOAD));
        tasks.add(new Task("Scan", new Point2D.Double(600, 200), TaskType.SCAN));
        tasks.add(new Task("Fuel Engine", new Point2D.Double(300, 450), TaskType.FUEL_ENGINE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Create back buffer if needed
        if (backBuffer == null || backBuffer.getWidth() != getWidth()
                || backBuffer.getHeight() != getHeight()) {
            backBuffer = new BufferedImage(getWidth(), getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            backBufferGraphics = backBuffer.createGraphics();
            backBufferGraphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
        }

        // Clear back buffer
        backBufferGraphics.setColor(getBackground());
        backBufferGraphics.fillRect(0, 0, getWidth(), getHeight());

        // Apply camera transform
        AffineTransform oldTransform = backBufferGraphics.getTransform();
        camera.apply(backBufferGraphics);

        // Draw game elements
        drawMap(backBufferGraphics);
        drawTasks(backBufferGraphics);
        drawPlayers(backBufferGraphics);
        drawParticles(backBufferGraphics);
        drawAnimations(backBufferGraphics);

        // Reset transform for lighting overlay
        backBufferGraphics.setTransform(oldTransform);

        // Draw lighting
        drawLighting(backBufferGraphics);

        // Draw HUD elements
        drawHUD(backBufferGraphics);

        // Draw back buffer to screen
        g.drawImage(backBuffer, 0, 0, null);
    }

    private void drawMap(Graphics2D g2d) {
        gameMap.draw(g2d);

        // Draw room names
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        for (Room room : gameMap.getRooms()) {
            g2d.drawString(room.getName(),
                    (int) (room.getBounds().getCenterX() - g2d.getFontMetrics().stringWidth(room.getName()) / 2),
                    (int) room.getBounds().getCenterY());
        }
    }

    private void drawTasks(Graphics2D g2d) {
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                // Draw task marker
                g2d.setColor(new Color(255, 255, 0, 150));
                double x = task.getPosition().getX();
                double y = task.getPosition().getY();

                // Pulsing effect
                double pulse = (Math.sin(System.currentTimeMillis() / 500.0) + 1) * 5;

                g2d.fill(new Ellipse2D.Double(
                        x - 15 - pulse,
                        y - 15 - pulse,
                        30 + pulse * 2,
                        30 + pulse * 2
                ));

                // Draw task icon
                task.drawIcon(g2d, x, y);
            }
        }
    }

    private void drawPlayers(Graphics2D g2d) {
        // Sort players by Y position for proper layering
        players.sort((p1, p2) -> Double.compare(p1.getY(), p2.getY()));

        for (Player player : players) {
            player.draw(g2d);

            // Draw interaction range indicator for local player
            if (player == localPlayer && !player.isDead()) {
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.draw(new Ellipse2D.Double(
                        player.getX() - 25,
                        player.getY() - 25,
                        50,
                        50
                ));
            }
        }
    }

    private void drawParticles(Graphics2D g2d) {
        particles.removeIf(ParticleEffect::isFinished);
        for (ParticleEffect particle : particles) {
            particle.update();
            particle.draw(g2d);
        }
    }

    private void drawAnimations(Graphics2D g2d) {
        animations.removeIf(Animation::isFinished);
        for (Animation animation : animations) {
            animation.update();
            animation.draw(g2d);
        }
    }

    private void drawLighting(Graphics2D g2d) {
        if (!isEmergencyLighting) {
            // Create radial gradient for player vision
            Point2D center = camera.worldToScreen(localPlayer.getPosition());
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color[] colors = {
                    new Color(0, 0, 0, 0),
                    new Color(0, 0, 0, (int)(lightingAlpha * 128)),
                    new Color(0, 0, 0, (int)(lightingAlpha * 255))
            };

            RadialGradientPaint gradient = new RadialGradientPaint(
                    (float)center.getX(),
                    (float)center.getY(),
                    (float)visionRadius,
                    dist,
                    colors
            );

            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void drawHUD(Graphics2D g2d) {
        // Draw mini-map
        drawMiniMap(g2d);

        // Draw task list
        if (!localPlayer.isImpostor()) {
            drawTaskList(g2d);
        }

        // Draw kill cooldown for impostor
        if (localPlayer.isImpostor()) {
            drawKillCooldown(g2d);
        }

        // Draw emergency button cooldown
        drawEmergencyButton(g2d);
    }

    private void drawMiniMap(Graphics2D g2d) {
        int mapSize = 150;
        int margin = 10;

        // Draw map background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(
                getWidth() - mapSize - margin,
                margin,
                mapSize,
                mapSize,
                10,
                10
        );

        // Scale factor for mini-map
        double scaleX = mapSize / gameMap.getWidth();
        double scaleY = mapSize / gameMap.getHeight();

        // Draw rooms
        g2d.setColor(new Color(100, 100, 100));
        for (Room room : gameMap.getRooms()) {
            Rectangle2D bounds = room.getBounds();
            g2d.fillRect(
                    (int)(getWidth() - mapSize - margin + bounds.getX() * scaleX),
                    (int)(margin + bounds.getY() * scaleY),
                    (int)(bounds.getWidth() * scaleX),
                    (int)(bounds.getHeight() * scaleY)
            );
        }

        // Draw players as dots
        for (Player player : players) {
            if (!player.isDead()) {
                g2d.setColor(player.getColor());
                g2d.fillOval(
                        (int)(getWidth() - mapSize - margin + player.getX() * scaleX - 2),
                        (int)(margin + player.getY() * scaleY - 2),
                        4,
                        4
                );
            }
        }
    }

    private void drawTaskList(Graphics2D g2d) {
        int completed = (int) tasks.stream().filter(Task::isCompleted).count();
        int total = tasks.size();

        String taskText = String.format("Tasks: %d/%d", completed, total);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        g2d.drawString(taskText, 10, getHeight() - 10);
    }

    private void drawKillCooldown(Graphics2D g2d) {
        // Draw kill button cooldown
        int cooldownSize = 40;
        g2d.setColor(new Color(200, 0, 0, 180));
        g2d.fillOval(10, getHeight() - cooldownSize - 10, cooldownSize, cooldownSize);
    }

    private void drawEmergencyButton(Graphics2D g2d) {
        // Draw emergency button
        int buttonSize = 40;
        g2d.setColor(new Color(255, 0, 0, 180));
        g2d.fillRoundRect(
                getWidth() - buttonSize - 10,
                getHeight() - buttonSize - 10,
                buttonSize,
                buttonSize,
                10,
                10
        );
    }




    @Override
    public void removeNotify() {
        dispose();
        super.removeNotify();
    }

    public void dispose() {
        if (backBufferGraphics != null) {
            backBufferGraphics.dispose();
            backBufferGraphics = null;
        }
        if (imageCache != null) {
            imageCache.cleanup();
        }
        if (particles != null) {
            particles.clear();
        }
        if (animations != null) {
            animations.clear();
        }
    }

    public void addParticleEffect(ParticleEffect effect) {
        if (particles != null) {
            particles.add(effect);
        }
    }

    public void addAnimation(Animation animation) {
        if (animations != null) {
            animations.add(animation);
        }
    }

    public void setEmergencyLighting(boolean emergency) {
        this.isEmergencyLighting = emergency;
        repaint();
    }

    public Task getNearbyTask(Point2D.Double position) {
        for (Task task : tasks) {
            if (!task.isCompleted() && task.getPosition().distance(position) < 50) {
                return task;
            }
        }
        return null;
    }

    public Room getCurrentRoom() {
        return gameMap.getRoomAt(localPlayer.getPosition());
    }
}

