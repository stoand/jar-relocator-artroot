package taxli;

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
        // Find a relocation pattern that matches the internal name.
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            String originalPrefix = entry.getKey();

            if (internalName.startsWith(originalPrefix)) {
                String newPrefix = entry.getValue();
                return newPrefix + internalName.substring(originalPrefix.length());
            }
        }

        // If no relocation applies, return the original name.
        return internalName;
    }

    // You should rely on the default Remapper methods for other types of mapping,
    // as they correctly identify what should and should not be mapped.
    // For example, mapSignature will internally call map() only on the class names.
    // So, you should NOT try to manually parse the signature.
}