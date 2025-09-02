package antiproguard;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.ClassRemapper; // For convenience in chaining

import static org.objectweb.asm.Opcodes.ASM9;


public class RelocationClassVisitor extends ClassRemapper {

    private final Remapper remapper;

    protected String className;

    public RelocationClassVisitor(ClassVisitor classVisitor, Remapper remapper) {
        super(ASM9, classVisitor, remapper);
        this.remapper = remapper;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        String maybeChangedSignature = signature;
        if (signature != null && !signature.startsWith("<")) {
            maybeChangedSignature = remapper.mapSignature(signature, true);
        }
        int newAccess = access;
        String newName = remapper.map(name);
        if (!newName.equals(name)) {
            newAccess = access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED & ~Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC;
        }
        super.visit(version, newAccess, newName, maybeChangedSignature, remapper.map(superName), remapper.mapTypes(interfaces));
    }

    // This method is called for each annotation on the class
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return super.visitAnnotation(remapper.mapDesc(descriptor), visible);
    }

    // This method is called for each field
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        // Remap the field's descriptor and signature
        return super.visitField(access, remapper.mapFieldName(className, name, descriptor), remapper.mapDesc(descriptor), remapper.mapSignature(signature, false), remapper.mapValue(value));
    }

    // This method is called for each method
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

        if (!remapper.map(className).equals(className)) {
            access = access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC;
        }

        // Remap the method's name, descriptor, signature, and exceptions
        return super.visitMethod(access, remapper.mapMethodName(className, name, descriptor), remapper.mapMethodDesc(descriptor), remapper.mapSignature(signature, false), exceptions == null ? null : remapper.mapTypes(exceptions));
    }
}