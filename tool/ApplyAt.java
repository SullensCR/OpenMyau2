import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Applies Forge AccessTransformer rules (widen access) to a jar.
 *  Usage: ApplyAt <in.jar> <forge_at.cfg> <out.jar>
 *  Supports lines: public|protected|private <class> [<member>[<desc>]]
 *  (+f for final, -f to remove final — final flags ignored except kept as-is). */
public class ApplyAt {
    static class Rule {
        int access;
        String cls, member, desc;
    }

    public static void main(String[] args) throws Exception {
        List<Rule> rules = new ArrayList<>();
        for (String raw : Files.readAllLines(Paths.get(args[1]))) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            Rule r = new Rule();
            String acc = p[0].split("[-+]")[0];
            r.access = acc.equals("public") ? Opcodes.ACC_PUBLIC
                     : acc.equals("protected") ? Opcodes.ACC_PROTECTED
                     : acc.equals("private") ? Opcodes.ACC_PRIVATE : 0;
            r.cls = p[1].replace('.', '/');
            if (p.length > 2) {
                String m = p[2];
                int d = m.indexOf('(');
                if (d >= 0) { r.member = m.substring(0, d); r.desc = m.substring(d); }
                else r.member = m;
            }
            rules.add(r);
        }
        Map<String, List<Rule>> byClass = new HashMap<>();
        for (Rule r : rules) byClass.computeIfAbsent(r.cls, k -> new ArrayList<>()).add(r);
        System.out.println("rules=" + rules.size());
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile in = new ZipFile(args[0])) {
            Enumeration<? extends ZipEntry> en = in.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.isDirectory()) entries.put(e.getName(), in.getInputStream(e).readAllBytes());
            }
        }
        int widened = 0;
        for (Map.Entry<String, List<Rule>> e : byClass.entrySet()) {
            String path = e.getKey() + ".class";
            byte[] bytes = entries.get(path);
            if (bytes == null) continue;
            final int[] n = {0};
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor v = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override public void visit(int ver, int acc, String name, String sig, String sup, String[] ifs) {
                    for (Rule r : e.getValue()) {
                        if (r.member == null) acc = widen(acc, r.access);
                    }
                    super.visit(ver, acc, name, sig, sup, ifs);
                }
                @Override public FieldVisitor visitField(int acc, String name, String desc, String sig, Object val) {
                    for (Rule r : e.getValue()) {
                        if (r.member != null && r.desc == null && r.member.equals(name)) acc = widen(acc, r.access);
                    }
                    n[0] += 0;
                    return super.visitField(acc, name, desc, sig, val);
                }
                @Override public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                    for (Rule r : e.getValue()) {
                        if (r.member != null && r.member.equals(name) && (r.desc == null || r.desc.equals(desc))) {
                            acc = widen(acc, r.access);
                            n[0]++;
                        }
                    }
                    return super.visitMethod(acc, name, desc, sig, ex);
                }
            };
            reader.accept(v, 0);
            entries.put(path, writer.toByteArray());
            widened += n[0];
        }
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(args[2]))))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue());
                out.closeEntry();
            }
        }
        System.out.println("methodsWidened=" + widened + " (fields/classes applied silently)");
    }

    static int widen(int acc, int target) {
        acc &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC);
        return acc | target;
    }
}
