package event;

public class AnimationManager {
    private static final int MAX_PARTICLES = 1000;
    private static final int MAX_ANIMATIONS = 100;

    public static void addParticle(List<ParticleEffect> particles, ParticleEffect particle) {
        // Remove old particles if limit reached
        while (particles.size() >= MAX_PARTICLES) {
            particles.remove(0);
        }
        particles.add(particle);
    }

    public static void addAnimation(List<Animation> animations, Animation animation) {
        // Remove old animations if limit reached
        while (animations.size() >= MAX_ANIMATIONS) {
            animations.remove(0);
        }
        animations.add(animation);
    }

    public static void updateAnimations(List<Animation> animations, List<ParticleEffect> particles) {
        // Remove finished animations and particles
        animations.removeIf(Animation::isFinished);
        particles.removeIf(ParticleEffect::isFinished);

        // Update remaining animations
        for (Animation anim : animations) {
            anim.update();
        }

        // Update remaining particles
        for (ParticleEffect particle : particles) {
            particle.update();
        }
    }
}