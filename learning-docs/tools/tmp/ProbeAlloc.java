import java.util.*;

public class ProbeAlloc {
    public static void main(String[] a) {
        long mb = 1024 * 1024;
        Runtime rt = Runtime.getRuntime();
        System.out.println("maxHeap=" + rt.maxMemory() / mb + "MB");
        List<byte[]> l = new ArrayList<>();
        try {
            for (int i = 0; i < 400; i++) {
                byte[] b = new byte[(int) mb];
                for (int p = 0; p < b.length; p += 4096) {
                    b[p] = 1;
                }
                l.add(b);
                if (i % 50 == 0) {
                    System.out.println("alloc " + (i + 1) + "MB used=" + (rt.totalMemory() - rt.freeMemory()) / mb + "MB total=" + rt.totalMemory() / mb + "MB");
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OOM at " + l.size() + "MB allocated; used=" + (rt.totalMemory() - rt.freeMemory()) / mb + "MB total=" + rt.totalMemory() / mb + "MB");
        }
    }
}
