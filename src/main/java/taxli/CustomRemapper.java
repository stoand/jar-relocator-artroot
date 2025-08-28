package taxli;

import org.objectweb.asm.commons.Remapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomRemapper extends Remapper {

    private final Map<String, String> relocations;

    public CustomRemapper(Map<String, String> relocations) {
        this.relocations = relocations;
    }

    @Override
    public String map(String internalName) {
        for (Map.Entry<String, String> entry : relocations.entrySet()) {
            String originalPrefix = entry.getKey(); // e.g., "org/example/oldpackage/"
            String newPrefix = entry.getValue();    // e.g., "com/new/package/"

            if (internalName.startsWith(originalPrefix)) {
                return newPrefix + internalName.substring(originalPrefix.length());
            }
        }
        return internalName; // No relocation applied
    }

    // ASM also calls mapFieldName, mapMethodName, mapDesc etc.
    // The default implementation in Remapper typically calls map(String internalName)
    // for relevant parts of the string, which is why our map() override is crucial.
    // If you need very specific remapping for field names or method names that
    // differ from class names, you would override those too.
}