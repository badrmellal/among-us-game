package event;

import java.awt.*;
import java.awt.geom.*;

public class ParticleEffect {
    private double x, y;
    private double velocityX, velocityY;
    private double lifespan;
    private double age;
    private Color color;
    private double size;

    public ParticleEffect(double x, double y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.lifespan = 1.0; // 1 second
        this.age = 0;
        this.size = 5;

        // Random velocity
        double angle = Math.random() * Math.PI * 2;
        double speed = Math.random() * 2 + 1;
        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        age += 0.016; // Assuming 60 FPS
    }

    public void draw(Graphics2D g2d) {
        float alpha = (float) (1.0 - age / lifespan);
        if (alpha <= 0) return;

        Color particleColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(255 * alpha)
        );
        g2d.setColor(particleColor);
        g2d.fill(new Ellipse2D.Double(x - size/2, y - size/2, size, size));
    }

    public boolean isFinished() {
        return age >= lifespan;
    }
}
