package event;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Animation {
    private double x, y;
    private BufferedImage[] frames;
    private int currentFrame;
    private long lastFrameTime;
    private long frameDuration;
    private boolean isLooping;
    private boolean isFinished;

    public Animation(double x, double y, BufferedImage[] frames, long frameDuration) {
        this.x = x;
        this.y = y;
        this.frames = frames;
        this.frameDuration = frameDuration;
        this.currentFrame = 0;
        this.lastFrameTime = System.currentTimeMillis();
        this.isLooping = true;
        this.isFinished = false;
    }

    public Animation(double x, double y, BufferedImage[] frames, long frameDuration, boolean looping) {
        this(x, y, frames, frameDuration);
        this.isLooping = looping;
    }

    public void update() {
        if (isFinished) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= frameDuration) {
            currentFrame++;
            lastFrameTime = currentTime;

            if (currentFrame >= frames.length) {
                if (isLooping) {
                    currentFrame = 0;
                } else {
                    currentFrame = frames.length - 1;
                    isFinished = true;
                }
            }
        }
    }

    public void draw(Graphics2D g2d) {
        if (currentFrame < frames.length && frames[currentFrame] != null) {
            BufferedImage currentImage = frames[currentFrame];
            int width = currentImage.getWidth();
            int height = currentImage.getHeight();

            g2d.drawImage(currentImage,
                    (int)(x - width/2),
                    (int)(y - height/2),
                    width, height, null);
        }
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setFrameDuration(long duration) {
        this.frameDuration = duration;
    }

    public void reset() {
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();
        isFinished = false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setLooping(boolean looping) {
        this.isLooping = looping;
    }

    public static class Builder {
        private double x, y;
        private BufferedImage[] frames;
        private long frameDuration = 100; // default 100ms per frame
        private boolean looping = true;

        public Builder position(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder frames(BufferedImage[] frames) {
            this.frames = frames;
            return this;
        }

        public Builder frameDuration(long duration) {
            this.frameDuration = duration;
            return this;
        }

        public Builder looping(boolean looping) {
            this.looping = looping;
            return this;
        }

        public Animation build() {
            if (frames == null || frames.length == 0) {
                throw new IllegalStateException("Animation frames cannot be null or empty");
            }
            return new Animation(x, y, frames, frameDuration, looping);
        }
    }

    // Utility method to create a simple animation from a sprite sheet
    public static Animation fromSpriteSheet(
            BufferedImage spriteSheet,
            int frameWidth,
            int frameHeight,
            int frameCount,
            long frameDuration,
            double x,
            double y) {

        BufferedImage[] frames = new BufferedImage[frameCount];

        for (int i = 0; i < frameCount; i++) {
            frames[i] = new BufferedImage(
                    frameWidth,
                    frameHeight,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = frames[i].createGraphics();
            g.drawImage(spriteSheet,
                    0, 0, frameWidth, frameHeight,
                    i * frameWidth, 0, (i + 1) * frameWidth, frameHeight,
                    null);
            g.dispose();
        }

        return new Animation(x, y, frames, frameDuration);
    }

    // Utility method to create explosion animation
    public static Animation createExplosion(double x, double y) {
        int size = 64;
        int frameCount = 8;
        BufferedImage[] frames = new BufferedImage[frameCount];

        for (int i = 0; i < frameCount; i++) {
            frames[i] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[i].createGraphics();

            // Draw explosion frame
            float alpha = 1.0f - (i / (float)frameCount);
            g.setColor(new Color(1.0f, 0.5f, 0.0f, alpha));
            int radius = (size/4) + (i * size/8);
            g.fillOval(size/2 - radius, size/2 - radius, radius * 2, radius * 2);

            g.dispose();
        }

        return new Animation(x, y, frames, 50, false);
    }

    // Utility method to create death animation
    public static Animation createDeathAnimation(double x, double y, Color playerColor) {
        int size = 48;
        int frameCount = 6;
        BufferedImage[] frames = new BufferedImage[frameCount];

        for (int i = 0; i < frameCount; i++) {
            frames[i] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[i].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw death animation frame
            float progress = i / (float)(frameCount - 1);
            drawDeathFrame(g, size, progress, playerColor);

            g.dispose();
        }

        return new Animation(x, y, frames, 100, false);
    }

    private static void drawDeathFrame(Graphics2D g, int size, float progress, Color color) {
        // Draw player body falling
        double angle = progress * Math.PI / 2; // Rotate 90 degrees
        g.rotate(angle, size/2, size/2);

        // Make color fade out
        Color frameColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(255 * (1.0f - progress))
        );

        g.setColor(frameColor);
        g.fillRoundRect(size/4, size/4, size/2, size/2, 8, 8);

        // Draw X eyes if fully rotated
        if (progress > 0.8f) {
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(2));
            // Left X
            g.drawLine(size/3, size/3, size/3 + 6, size/3 + 6);
            g.drawLine(size/3 + 6, size/3, size/3, size/3 + 6);
            // Right X
            g.drawLine(2*size/3, size/3, 2*size/3 + 6, size/3 + 6);
            g.drawLine(2*size/3 + 6, size/3, 2*size/3, size/3 + 6);
        }
    }
}