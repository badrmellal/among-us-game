package event;


import java.awt.*;
import java.awt.geom.Point2D;

public class Camera {
    private Player target;
    private double x, y;
    private double zoom;

    public Camera(Player target) {
        this.target = target;
        this.zoom = 1.0;
    }

    public void apply(Graphics2D g2d) {
        // Center camera on target
        x = -target.getX() + GamePanel.WINDOW_WIDTH / 2;
        y = -target.getY() + GamePanel.WINDOW_HEIGHT / 2;

        // Apply transform
        g2d.translate(x, y);
        g2d.scale(zoom, zoom);
    }

    public Point2D.Double worldToScreen(Point2D.Double worldPoint) {
        return new Point2D.Double(
                worldPoint.x + x,
                worldPoint.y + y
        );
    }
}