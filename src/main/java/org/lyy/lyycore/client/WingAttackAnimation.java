package org.lyy.lyycore.client;

import org.lyy.lyycore.content.wings.FeatherAttack;

/** Wing-specific timing and detached-feather visibility, independent of the mesh loader. */
final class WingAttackAnimation {
    enum Phase {
        PREPARE("attack_prepare"), RELEASE("attack_release"), WAIT("attack_wait"), RETURN("attack_return");
        final String clip;
        Phase(String clip) { this.clip = clip; }
        float seconds(float age, float returnedAt) {
            return switch (this) {
                case PREPARE -> age;
                case RELEASE -> age - FeatherAttack.PREPARE;
                case WAIT -> 0;
                case RETURN -> age - returnedAt;
            };
        }
    }
    @FunctionalInterface interface Sampler {
        void sample(String clip, int bone, float seconds, float[] result);
    }
    private final int[] bones;
    WingAttackAnimation(int[] bones) { this.bones = bones.clone(); }

    static Phase phase(int feather, float age, int count) {
        if (feather < 0 || feather >= count || age < 0 || age >= FeatherAttack.duration(count)) return null;
        if (age < FeatherAttack.PREPARE) return Phase.PREPARE;
        if (age < FeatherAttack.PREPARE + FeatherAttack.RELEASE) return Phase.RELEASE;
        float returned = FeatherAttack.returnedAt(feather, count);
        if (age < returned) return Phase.WAIT;
        return age < returned + FeatherAttack.RETURN ? Phase.RETURN : null;
    }
    static float seconds(Phase phase, int feather, float age, int count) {
        return phase.seconds(age, FeatherAttack.returnedAt(feather, count));
    }
    static int legacyFeather(String bone) {
        return switch (bone) {
            case "left_primary_04" -> 0;
            case "right_primary_04" -> 1;
            case "left_primary_06" -> 2;
            case "right_primary_06" -> 3;
            default -> -1;
        };
    }
    void apply(float[] pose, boolean[] hidden, float age, int count, Sampler sampler) {
        for (int feather = 0; feather < Math.min(count, bones.length); feather++) {
            var phase = phase(feather, age, count);
            if (phase == null) continue;
            int bone = bones[feather];
            sampler.sample(phase.clip, bone, seconds(phase, feather, age, count), pose);
            if (hidden != null) hidden[bone] = age >= FeatherAttack.release(feather, count)
                    && age < FeatherAttack.returnedAt(feather, count);
        }
    }
}
