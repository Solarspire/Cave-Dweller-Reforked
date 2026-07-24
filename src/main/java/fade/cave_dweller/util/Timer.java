package fade.cave_dweller.util;

import fade.cave_dweller.CaveDweller;
import fade.cave_dweller.config.ServerConfig;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;

public class Timer {
    @Nullable
    public Entity currentVictim;
    public int currentSpawn;
    public int currentNoise;
    public int targetSpawn;
    public int targetNoise;

    public Timer() {
        this.resetSpawnTimer();
        this.resetNoiseTimer();
    }

    public Timer(@Nullable Entity currentVictim) {
        this.currentVictim = currentVictim;
    }

    public boolean isSpawnTimerReached() {
        if (Utils.isOnSurface(this.currentVictim)) {
            return this.currentSpawn >= (int) (this.targetSpawn * ServerConfig.surfaceTimerMultiplier);
        }
        return this.currentSpawn >= this.targetSpawn;
    }

    public boolean isNoiseTimerReached() {
        if (Utils.isOnSurface(this.currentVictim)) {
            return this.currentNoise >= (int) (this.targetNoise * ServerConfig.surfaceTimerMultiplier);
        }
        return this.currentNoise >= this.targetNoise;
    }

    public void resetNoiseTimer() {
        int min = ServerConfig.resetNoiseMin;
        int max = ServerConfig.resetNoiseMax;
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
            CaveDweller.LOG.error(
                "Config for RESET_NOISE was invalid - max [{}] was smaller than min [{}], values were swapped",
                temp,
                min
            );
        }

        int noiseTimer = randomRangeTicks(min, max);
        this.currentNoise = 0;
        this.targetNoise = noiseTimer;
    }

    public void resetSpawnTimer() {
        int spawnTimer;
        if (CaveDweller.RANDOM.nextDouble() <= ServerConfig.canSpawnCooldownChance) {
            spawnTimer = Utils.secondsToTicks(ServerConfig.canSpawnCooldown);
        } else {
            int min = ServerConfig.canSpawnMin;
            int max = ServerConfig.canSpawnMax;
            if (max < min) {
                int temp = min;
                min = max;
                max = temp;
                CaveDweller.LOG.error(
                    "Config for RESET_CALM was invalid - max [{}] was smaller than min [{}], values were swapped",
                    temp,
                    min
                );
            }

            spawnTimer = randomRangeTicks(min, max);
        }

        this.currentSpawn = 0;
        this.targetSpawn = spawnTimer;
    }

    private static int randomRangeTicks(int minSeconds, int maxSeconds) {
        int minTicks = Utils.secondsToTicks(minSeconds);
        int maxTicks = Utils.secondsToTicks(maxSeconds + 1);
        if (maxTicks <= minTicks) {
            return minTicks;
        }
        return CaveDweller.RANDOM.nextInt(maxTicks - minTicks) + minTicks;
    }

    @Override
    public String toString() {
        String name = this.currentVictim != null ? this.currentVictim.getName() : "NONE";
        return name + " | " + this.currentSpawn + "/" + this.targetSpawn + " | " + this.currentNoise + "/" + this.targetNoise;
    }
}
