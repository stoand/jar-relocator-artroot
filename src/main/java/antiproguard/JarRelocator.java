package antiproguard;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

public class JarRelocator {

    private final File inputJar;
    private final File outputJar;
    private final CustomRemapper remapper;

    public JarRelocator(File inputJar, File outputJar, Map<String, String> relocations) {
        this.inputJar = inputJar;
        this.outputJar = outputJar;
        this.remapper = new CustomRemapper(relocations);
    }

    private ClassWriter generateClassWriter(ClassReader classReader, File inputJar) {
        return  new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
//                ClassLoader classLoader = this.getClassLoader();

                URL url;

                try {
                    url = inputJar.toURI().toURL();
                    URL[] urls = new URL[]{url};

                    // 2. Create a URLClassLoader
                    URLClassLoader classLoader = new URLClassLoader(urls, JarRelocator.class.getClassLoader());

                    Class<?> class1;
                    try {
                        class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
                    } catch (Error | Exception e) {
//                        throw new TypeNotPresentException(type1, e);
                        return "error/type/not/preset";
                    }

                    Class<?> class2;
                    try {
                        class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
                    } catch (Error | Exception e) {
//                        throw new TypeNotPresentException(type2, e);
                        return "error/type/not/preset";
                    }

                    if (class1.isAssignableFrom(class2)) {
                        return type1;
                    } else if (class2.isAssignableFrom(class1)) {
                        return type2;
                    } else if (!class1.isInterface() && !class2.isInterface()) {
                        do {
                            class1 = class1.getSuperclass();
                        } while(!class1.isAssignableFrom(class2));

                        return class1.getName().replace('.', '/');
                    } else {
                        return "java/lang/Object";
                    }

                } catch (MalformedURLException e) {
                    return "java/lang/Object";
                }
            }
        };
    }

    public void relocate() throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();

                // Skip manifest, it will be generated for the new JAR
                if (entryName.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }

                // If it's a class file, remap it
                if (entryName.endsWith(".class")) {
                    // Read the original class bytes
                    ClassReader classReader = new ClassReader(jis);
                    ClassWriter classWriter = this.generateClassWriter(classReader, inputJar);// Compute frames/maxs automatically if needed

                    // Chain our remapping visitor
                    RelocationClassVisitor relocationVisitor = new RelocationClassVisitor(classWriter, remapper);
                    classReader.accept(relocationVisitor, SKIP_DEBUG | SKIP_FRAMES); // Faster parsing options

                    // Create a new entry name for the relocated class
                    String newClassName = remapper.map(entryName.substring(0, entryName.length() - ".class".length()));
                    JarEntry newEntry = new JarEntry(newClassName + ".class");
                    jos.putNextEntry(newEntry);
                    jos.write(classWriter.toByteArray());

                } else {
                    // For non-class files, just copy them, but remap their path if they are resource files
                    String remappedEntryName = remapper.map(entryName);
                    JarEntry newEntry = new JarEntry(remappedEntryName);
                    jos.putNextEntry(newEntry);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = jis.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                }
                jos.closeEntry();
                jis.closeEntry(); // Ensure the current input entry is closed
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File input = new File(args[0]);
        File output = new File(args[1]);

//        // Create a dummy input JAR for demonstration
//        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(input))) {
//            jos.putNextEntry(new JarEntry("MyRootClass.class"));
//            jos.write(new byte[]{/* dummy bytecode for MyRootClass */});
//            jos.closeEntry();
//            jos.putNextEntry(new JarEntry("com/example/MyPackageClass.class"));
//            jos.write(new byte[]{/* dummy bytecode for MyPackageClass */});
//            jos.closeEntry();
//        }
//        System.out.println("Created dummy input JAR: " + input.getName());

        Map<String, String> relocations = new HashMap<>();
        // Relocate classes in the root package to "com/relocated/root/"
        // ASM Remapper expects internal names (slashes, not dots)
        relocations.put("", "artroot/"); // Empty string for root package

        // Example: Relocate another specific package
//        relocations.put("com/example/", "org/newexample/");

        JarRelocator relocator = new JarRelocator(input, output, relocations);
        relocator.relocate();

        System.out.println("Relocation complete. Output JAR: " + output.getName());
    }
}