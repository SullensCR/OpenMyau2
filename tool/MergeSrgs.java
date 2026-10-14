import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class MergeSrgs {
    public static void main(String[] args) throws Exception {
        String srgBin = args[0], forgeBin = args[1], out = args[2];
        Map<String, byte[]> srg = readJar(srgBin);
        Map<String, byte[]> forge = readJar(forgeBin);
        int mergedMethods = 0, mergedFields = 0, addedClasses = 0;
        Map<String, byte[]> result = new LinkedHashMap<>(srg);
        for (Map.Entry<String, byte[]> e : forge.entrySet()) {
            String name = e.getKey();
            if (!name.endsWith(".class")) continue;
            if (!name.startsWith("net/minecraft/")) {
                if (!result.containsKey(name)) { result.put(name, e.getValue()); addedClasses++; }
                continue;
            }
            byte[] srgBytes = result.get(name);
            if (srgBytes == null) { result.put(name, e.getValue()); addedClasses++; continue; }
            ClassNode srgCn = read(srgBytes), forgeCn = read(e.getValue());
            Set<String> srgM = new HashSet<>(), srgF = new HashSet<>();
            for (MethodNode m : srgCn.methods) srgM.add(m.name + m.desc);
            for (FieldNode f : srgCn.fields) srgF.add(f.name + ":" + f.desc);
            boolean dirty = false;
            for (FieldNode f : forgeCn.fields) {
                if (srgF.add(f.name + ":" + f.desc)) { srgCn.fields.add(f); mergedFields++; dirty = true; }
            }
            for (MethodNode m : forgeCn.methods) {
                if (m.name.startsWith("<")) continue;
                if (srgM.add(m.name + m.desc)) { srgCn.methods.add(m); mergedMethods++; dirty = true; }
            }
            if (dirty) {
                ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                srgCn.accept(w);
                result.put(name, w.toByteArray());
            }
        }
        try (ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(out))))) {
            for (Map.Entry<String, byte[]> e : result.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue());
                z.closeEntry();
            }
        }
        System.out.println("mergedMethods=" + mergedMethods + " mergedFields=" + mergedFields + " addedClasses=" + addedClasses);
    }
    static Map<String, byte[]> readJar(String p) throws Exception {
        Map<String, byte[]> m = new LinkedHashMap<>();
        try (ZipFile z = new ZipFile(p)) {
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                m.put(e.getName(), z.getInputStream(e).readAllBytes());
            }
        }
        return m;
    }
    static ClassNode read(byte[] b) {
        ClassNode n = new ClassNode();
        new ClassReader(b).accept(n, 0);
        return n;
    }
}
