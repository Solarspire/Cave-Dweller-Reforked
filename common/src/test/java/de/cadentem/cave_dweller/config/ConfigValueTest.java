package de.cadentem.cave_dweller.config;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ConfigValueTest {
    @Test void rejectsOutOfRangeAndFractionalInteger() {
        var value = new ConfigValue<>("test_distance", 16, 0, 64);
        for (String json : List.of("-1", "65", "1.5", "null", "true", "\"16\"")) {
            assertThrows(IllegalArgumentException.class, () -> value.read(JsonParser.parseString(json)), json);
        }
        value.read(JsonParser.parseString("64")); assertEquals(64, value.get());
        value.read(null); assertEquals(16, value.get());
    }
    @Test void validatesDimensionIds() {
        var value = new ConfigValue<List<String>>("test_dimensions", List.of("minecraft:overworld"));
        assertThrows(IllegalArgumentException.class, () -> value.read(JsonParser.parseString("[\"not an id\"]")));
        assertThrows(IllegalArgumentException.class, () -> value.read(JsonParser.parseString("[1]")));
        value.read(JsonParser.parseString("[\"custom:cave/deep\"]"));
        assertEquals(List.of("custom:cave/deep"), value.get());
    }
    @Test void configRoundTripAndMissingValues(@TempDir Path directory) throws Exception {
        var distance = new ConfigValue<>("test_round_trip", 16, 0, 64);
        Path file = directory.resolve("settings.json");
        ConfigValue.load(file); assertTrue(Files.readString(file).contains("test_round_trip"));
        Files.writeString(file, "{\"test_round_trip\":32}");
        ConfigValue.load(file); assertEquals(32, distance.get());
        Files.writeString(file, "{}"); ConfigValue.load(file); assertEquals(16, distance.get());
    }
}
