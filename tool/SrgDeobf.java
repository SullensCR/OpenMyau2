import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Notch -> SRG jar remapper (compile-only helper).
 *  Usage: SrgDeobf <notch.jar> <joined.srg> <out-srg.jar>
 *  Handles CL/FD/MD lines; drops META-INF signatures. */
public class SrgDeobf {
    public static void main(String[] args) throws Exception {
        Map<String, String> classes = new HashMap<>();
        Map<String, String> fields = new HashMap<>();
        Map<String, String> methods = new HashMap<>();
        for (String line : Files.readAllLines(Paths.get(args[1]))) {
            if (line.startsWith("CL: ")) {
                String[] p = line.substring(4).split(" ");
                classes.put(p[0], p[1]);
            } else if (line.startsWith("FD: ")) {
                String[] p = line.substring(4).split(" ");
                fields.put(p[0], p[1].substring(p[1].lastIndexOf('/') + 1));
            } else if (line.startsWith("MD: ")) {
                String[] p = line.substring(4).split(" ");
                methods.put(p[0] + " " + p[1], p[2].substring(p[2].lastIndexOf('/') + 1));
            }
        }
        System.out.println("maps: classes=" + classes.size() + " fields=" + fields.size() + " methods=" + methods.size());
        Remapper remapper = new Remapper() {
            @Override public String map(String name) {
                String v = classes.get(name);
                if (v != null) return v;
                int d = name.indexOf('$');
                if (d > 0) {
                    String outer = classes.get(name.substring(0, d));
                    if (outer != null) return outer + name.substring(d);
                }
                return name;
            }
            @Override public String mapFieldName(String owner, String name, String desc) {
                return fields.getOrDefault(owner + "/" + name, name);
            }
            @Override public String mapMethodName(String owner, String name, String desc) {
                return methods.getOrDefault(owner + "/" + name + " " + desc, name);
            }
        };
        int remapped = 0, copied = 0;
        try (ZipFile in = new ZipFile(args[0]);
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(args[2]))))) {
            Enumeration<? extends ZipEntry> en = in.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory()) continue;
                if (name.startsWith("META-INF/")) continue;
                byte[] data = in.getInputStream(e).readAllBytes();
                if (name.endsWith(".class")) {
                    ClassReader reader = new ClassReader(data);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassRemapper(writer, remapper), 0);
                    out.putNextEntry(new ZipEntry(remapper.mapType(reader.getClassName()) + ".class"));
                    out.write(writer.toByteArray());
                    out.closeEntry();
                    remapped++;
                } else {
                    out.putNextEntry(new ZipEntry(name));
                    out.write(data);
                    out.closeEntry();
                    copied++;
                }
            }
        }
        System.out.println("remapped=" + remapped + " copied=" + copied);
    }
}
