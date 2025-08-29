package antiproguard;

import org.objectweb.asm.commons.Remapper;

import java.util.Map;

public class CustomRemapper extends Remapper {

    private final Map<String, String> relocations;

    public CustomRemapper(Map<String, String> relocations) {
        // The keys of the map should be the internal names (e.g., "com/example/").
        // The values should be the relocated internal names (e.g., "org/relocated/").
        this.relocations = relocations;
    }

    @Override
    public String map(String internalName) {

        if (internalName == null) {
            return null;
        }

        // Find a relocation pattern that matches the internal name.
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            String originalPrefix = entry.getKey();

            if (!internalName.contains("/")) {
                String newPrefix = entry.getValue();
                return newPrefix + internalName.substring(originalPrefix.length());
            }
        }

        // If no relocation applies, return the original name.
        return internalName;
    }
}