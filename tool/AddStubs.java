import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Adds compile-only stub members (Forge-added vanilla methods/fields that
 *  exist in the game at runtime but not in any published jar).
 *  Usage: AddStubs <in.jar> <members.txt> <out.jar>
 *
 *  members.txt format (blank lines and # comments ignored):
 *    CLASS <internalName>
 *    M <acc...> <name> <desc>        (method; acc words: public protected private static)
 *    F <acc...> <name> <desc> [=const] (field; const forms: I:5 J:6 F:1.5 D:2.5 S:text Z:true C:x B:1)
 *
 *  Stub bodies return type defaults. Field ConstantValues are honored when given. */
public class AddStubs {
    static class Member {
        boolean method;
        int access;
        String name, desc, constant;
    }

    public static void main(String[] args) throws Exception {
        Map<String, List<Member>> members = new LinkedHashMap<>();
        String current = null;
        for (String raw : Files.readAllLines(Paths.get(args[1]))) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            if (p[0].equals("CLASS")) {
                current = p[1];
                members.putIfAbsent(current, new ArrayList<>());
            } else if ((p[0].equals("M") || p[0].equals("F")) && current != null) {
                Member m = new Member();
                m.method = p[0].equals("M");
                int i = 1;
                int acc = 0;
                while (i < p.length && isAccWord(p[i])) acc |= accWord(p[i++]);
                m.access = acc == 0 ? Opcodes.ACC_PUBLIC : acc;
                m.name = p[i++];
                StringBuilder desc = new StringBuilder(p[i++]);
                while (i < p.length && !p[i].startsWith("=")) desc.append(p[i++]);
                m.desc = desc.toString();
                if (i < p.length && p[i].startsWith("=")) m.constant = p[i].substring(1);
                members.get(current).add(m);
            } else {
                throw new IllegalArgumentException("bad line: " + raw);
            }
        }
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile in = new ZipFile(args[0])) {
            Enumeration<? extends ZipEntry> en = in.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.isDirectory()) entries.put(e.getName(), in.getInputStream(e).readAllBytes());
            }
        }
        int added = 0;
        for (Map.Entry<String, List<Member>> e : members.entrySet()) {
            String path = e.getKey() + ".class";
            byte[] bytes = entries.get(path);
            if (bytes == null) throw new IllegalStateException("class not in jar: " + path);
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            final Set<String> have = new HashSet<>();
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] ex) {
                    have.add("M " + n + " " + d);
                    return null;
                }
                @Override public FieldVisitor visitField(int a, String n, String d, String s, Object v) {
                    have.add("F " + n + " " + d);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            final List<Member> pending = new ArrayList<>();
            for (Member m : e.getValue()) {
                if (!have.contains((m.method ? "M " : "F ") + m.name + " " + m.desc)) pending.add(m);
            }
            if (pending.isEmpty()) continue;
            final int[] count = {0};
            ClassVisitor both = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override public void visitEnd() {
                    for (Member m : pending) {
                        if (m.method) {
                            MethodVisitor mv = cv.visitMethod(m.access, m.name, m.desc, null, null);
                            mv.visitCode();
                            emitDefault(mv, Type.getReturnType(m.desc));
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        } else {
                            cv.visitField(m.access, m.name, m.desc, null, parseConst(m));
                        }
                        count[0]++;
                    }
                    super.visitEnd();
                }
            };
            reader.accept(both, 0);
            entries.put(path, writer.toByteArray());
            added += count[0];
        }
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(args[2]))))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue());
                out.closeEntry();
            }
        }
        System.out.println("stubsAdded=" + added);
    }

    static boolean isAccWord(String w) {
        return w.equals("public") || w.equals("protected") || w.equals("private") || w.equals("static") || w.equals("final");
    }

    static int accWord(String w) {
        switch (w) {
            case "public": return Opcodes.ACC_PUBLIC;
            case "protected": return Opcodes.ACC_PROTECTED;
            case "private": return Opcodes.ACC_PRIVATE;
            case "static": return Opcodes.ACC_STATIC;
            case "final": return Opcodes.ACC_FINAL;
            default: return 0;
        }
    }

    static void emitDefault(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.VOID: mv.visitInsn(Opcodes.RETURN); break;
            case Type.BOOLEAN: case Type.BYTE: case Type.CHAR: case Type.SHORT: case Type.INT:
                mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN); break;
            case Type.LONG: mv.visitInsn(Opcodes.LCONST_0); mv.visitInsn(Opcodes.LRETURN); break;
            case Type.FLOAT: mv.visitInsn(Opcodes.FCONST_0); mv.visitInsn(Opcodes.FRETURN); break;
            case Type.DOUBLE: mv.visitInsn(Opcodes.DCONST_0); mv.visitInsn(Opcodes.DRETURN); break;
            default: mv.visitInsn(Opcodes.ACONST_NULL); mv.visitInsn(Opcodes.ARETURN); break;
        }
    }

    static Object parseConst(Member m) {
        if (m.constant == null) return null;
        String c = m.constant;
        try {
            if (c.startsWith("I:")) return Integer.parseInt(c.substring(2));
            if (c.startsWith("J:")) return Long.parseLong(c.substring(2));
            if (c.startsWith("F:")) return Float.parseFloat(c.substring(2));
            if (c.startsWith("D:")) return Double.parseDouble(c.substring(2));
            if (c.startsWith("S:")) return c.substring(2);
            if (c.startsWith("Z:")) return c.substring(2).equalsIgnoreCase("true") ? 1 : 0;
            if (c.startsWith("C:")) return c.charAt(2);
            if (c.startsWith("B:")) return (byte) Integer.parseInt(c.substring(2));
        } catch (Exception e) {
            throw new IllegalArgumentException("bad const: " + c);
        }
        throw new IllegalArgumentException("bad const: " + c);
    }
}
