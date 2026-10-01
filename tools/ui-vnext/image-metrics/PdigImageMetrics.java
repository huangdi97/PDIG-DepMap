import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintStream;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * PdigImageMetrics — No-Vision 盲实现用的截图量化取证（JDK ImageIO，零新增依赖）。
 *
 * 指标只用于发现：空白页 / 渲染失败 / 全黑 / 严重布局异常；不判断美观。
 * 计算：width, height, meanLuminance, darkPixelRatio, brightPixelRatio,
 *       nearMonochromeRatio, contentBoundingBox, edgeDensity, blankAreaRatio,
 *       dominantColors(top3, quantized 32-level buckets)。
 *
 * 用法：java PdigImageMetrics <png...|dir> [outJson]
 */
public final class PdigImageMetrics {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: java PdigImageMetrics <file|dir> [outJson]");
            System.exit(2);
        }
        List<File> files = new ArrayList<>();
        File target = new File(args[0]);
        if (target.isDirectory()) {
            collect(target, files);
        } else if (target.isFile()) {
            files.add(target);
        }
        files.sort(Comparator.comparing(File::getAbsolutePath));

        PrintStream out = System.out;
        File outJson = args.length > 1 ? new File(args[1]) : null;
        if (outJson != null) out = new PrintStream(outJson, StandardCharsets.UTF_8);

        out.println("[");
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            BufferedImage img = ImageIO.read(f);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("file", f.getName());
            m.put("path", f.getAbsolutePath());
            if (img == null) {
                m.put("error", "unreadable");
            } else {
                Metrics mm = compute(img);
                m.put("width", mm.width);
                m.put("height", mm.height);
                m.put("meanLuminance", round3(mm.meanLuma));
                m.put("darkPixelRatio", round4(mm.darkRatio));
                m.put("brightPixelRatio", round4(mm.brightRatio));
                m.put("nearMonochromeRatio", round4(mm.monoRatio));
                m.put("contentBoundingBox", mm.bbox);
                m.put("edgeDensity", round4(mm.edgeDensity));
                m.put("blankAreaRatio", round4(mm.blankRatio));
                m.put("dominantColors", mm.dominants);
            }
            out.println("  " + toJson(m) + (i < files.size() - 1 ? "," : ""));
        }
        out.println("]");
        if (outJson != null) out.close();
    }

    private static void collect(File dir, List<File> acc) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            if (c.isDirectory()) collect(c, acc);
            else if (c.getName().endsWith(".png")) acc.add(c);
        }
    }

    private static final class Metrics {
        int width, height;
        double meanLuma, darkRatio, brightRatio, monoRatio, edgeDensity, blankRatio;
        String bbox = "";
        List<String> dominants = new ArrayList<>();
    }

    private static Metrics compute(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        Metrics m = new Metrics();
        m.width = w;
        m.height = h;
        long lumaSum = 0;
        long dark = 0, bright = 0, mono = 0;
        long edge = 0;
        Map<Integer, Long> colorBuckets = new HashMap<>();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        long nonBg = 0;
        long total = (long) w * h;
        int[] row = new int[w];
        int[] prev = null;
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int rgb = row[x];
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                double luma = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                lumaSum += (long) Math.round(luma * 1000);
                if (luma < 0.25) dark++;
                if (luma > 0.85) bright++;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                if (max - min < 24) mono++;
                // 边缘密度：水平梯度（简化）
                if (x > 0) {
                    int pr = (row[x - 1] >> 16) & 0xFF;
                    int pg = (row[x - 1] >> 8) & 0xFF;
                    int pb = row[x - 1] & 0xFF;
                    double gx = (r - pr + g - pg + b - pb) / 3.0 / 255.0;
                    if (Math.abs(gx) > 0.12) edge++;
                }
                int bucket = (r >> 5) << 10 | (g >> 5) << 5 | (b >> 5);
                colorBuckets.merge(bucket, 1L, Long::sum);
                boolean isBg = isBackground(rgb);
                if (!isBg) {
                    nonBg++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
            prev = row.clone();
        }
        m.meanLuma = lumaSum / 1000.0 / total;
        m.darkRatio = (double) dark / total;
        m.brightRatio = (double) bright / total;
        m.monoRatio = (double) mono / total;
        m.edgeDensity = (double) edge / total;
        m.blankRatio = 1.0 - (double) nonBg / total;
        if (maxX >= 0) {
            m.bbox = minX + "," + minY + "," + (maxX - minX + 1) + "x" + (maxY - minY + 1);
        } else {
            m.bbox = "empty";
        }
        colorBuckets.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .forEach(e -> m.dominants.add("#" + String.format("%06x", e.getKey() << 5)));
        return m;
    }

    /** 背景判定：接近 vNext 深空画布色（#061225 / #030A18 / #0B1A33）的容差。 */
    private static boolean isBackground(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int[][] bg = {{6, 18, 37}, {3, 10, 24}, {11, 26, 51}};
        for (int[] c : bg) {
            if (Math.abs(r - c[0]) < 14 && Math.abs(g - c[1]) < 14 && Math.abs(b - c[2]) < 14) return true;
        }
        return false;
    }

    private static double round3(double v) { return Math.round(v * 1000) / 1000.0; }
    private static double round4(double v) { return Math.round(v * 10000) / 10000.0; }

    private static String toJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": ");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else if (v instanceof List) {
                sb.append("[");
                for (int i = 0; i < ((List<?>) v).size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(jsonEscape(((List<?>) v).get(i).toString())).append("\"");
                }
                sb.append("]");
            } else sb.append("\"").append(jsonEscape(v.toString())).append("\"");
        }
        return sb.append("}").toString();
    }

    /** JSON 字符串转义（Windows 路径反斜杠 / 引号 / 控制字符；保证 outJson 可被标准解析）。 */
    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}