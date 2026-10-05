package de.cadentem.cave_dweller.config;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Loader-independent values; ranges come directly from the reference Forge config. */
public final class ConfigValue<T> {
    private static final Map<String, ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private final String key;
    private final T defaultValue;
    private final double min, max;
    private T value;
    public ConfigValue(String key, T value) { this(key, value, -Double.MAX_VALUE, Double.MAX_VALUE); }
    public ConfigValue(String key, T value, double min, double max) {
        this.key = key; this.defaultValue = value; this.value = value; this.min = min; this.max = max;
        VALUES.put(key, this);
    }
    public T get() { return value; }
    @SuppressWarnings("unchecked")
    void read(JsonElement input) {
        if (input == null) { value = defaultValue; return; }
        Object parsed;
        if (defaultValue instanceof Boolean) {
            if (!input.isJsonPrimitive() || !input.getAsJsonPrimitive().isBoolean()) throw invalid();
            parsed = input.getAsBoolean();
        } else if (defaultValue instanceof Number) {
            if (!input.isJsonPrimitive() || !input.getAsJsonPrimitive().isNumber()) throw invalid();
            double number = input.getAsDouble();
            if (!Double.isFinite(number) || number < min || number > max) throw invalid();
            if (defaultValue instanceof Integer) {
                if (number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw invalid();
                parsed = (int)number;
            } else parsed = number;
        } else {
            if (!input.isJsonArray()) throw invalid();
            List<String> list = new ArrayList<>();
            for (JsonElement element : input.getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                        || !element.getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw invalid();
                list.add(element.getAsString());
            }
            parsed = List.copyOf(list);
        }
        value = (T)parsed;
    }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid cave_dweller setting: " + key); }
    public static void load(Path path) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(path)) {
                JsonElement document = JsonParser.parseString(Files.readString(path));
                if (!document.isJsonObject()) throw new IllegalArgumentException("Expected JSON object in " + path);
                JsonObject json = document.getAsJsonObject();
                VALUES.values().forEach(setting -> setting.read(json.get(setting.key)));
            } else {
                Files.createDirectories(path.getParent());
                JsonObject json = new JsonObject();
                VALUES.values().forEach(setting -> json.add(setting.key, gson.toJsonTree(setting.defaultValue)));
                Files.writeString(path, gson.toJson(json) + "\n");
            }
        } catch (IOException exception) { throw new IllegalStateException("Cannot load " + path, exception); }
    }
}
