package de.cadentem.cave_dweller.entities.goals;
public enum Roll {
    CHASE(0), STARE(1), FLEE(2), STROLL(3);
    public final int rollValue;
    Roll(int value) { rollValue = value; }
    public static Roll fromValue(int value) { return values()[value]; }
}
