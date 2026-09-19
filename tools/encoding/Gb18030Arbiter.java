import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 用 Java（Android 端口 `Charset.forName("GB18030")` 所用的实现）裁决
 * ICU 与 CPython 在无争议之外的分歧码位。
 *
 * 输入：.workbuddy/_gb_divergences.txt，每行 "<2|4> <HEX> <CPython> <ICU>"
 * 输出：每行追加 Java 的解码结果。
 */
public class Gb18030Arbiter {
    public static void main(String[] args) throws Exception {
        Charset cs = Charset.forName("GB18030");
        CharsetDecoder dec = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        System.out.println("JAVA_CHARSET=" + cs.name());
        System.out.println("JAVA_VERSION=" + System.getProperty("java.version"));

        Path in = Path.of(args.length > 0 ? args[0] : ".workbuddy/_gb_divergences.txt");
        List<String> rows = Files.readAllLines(in);
        StringBuilder sb = new StringBuilder();
        int agreeIcu = 0, agreeCpy = 0, neither = 0;
        for (String row : rows) {
            String[] f = row.trim().split("\\s+");
            if (f.length < 4) continue;
            String hex = f[1];
            int[] bytes = new int[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            String s = decode(dec, bytes);
            String java = (s == null) ? "UNDEF" : String.format("U+%04X", s.codePointAt(0));
            String icu = f[3];
            String cpy = f[2];
            if (java.equals(icu)) agreeIcu++;
            else if (java.equals(cpy)) agreeCpy++;
            else neither++;
            sb.append(hex).append(" cpython=").append(cpy)
              .append(" icu=").append(icu)
              .append(" java=").append(java).append('\n');
        }
        System.out.print(sb);
        System.out.println("JAVA_AGREES_ICU=" + agreeIcu);
        System.out.println("JAVA_AGREES_CPYTHON=" + agreeCpy);
        System.out.println("JAVA_AGREES_NEITHER=" + neither);
    }

    static String decode(CharsetDecoder dec, int[] bytes) {
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) b[i] = (byte) bytes[i];
        try {
            CharBuffer cb = dec.reset().decode(ByteBuffer.wrap(b));
            if (cb.length() == 0) return null;
            return cb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
