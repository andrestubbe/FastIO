package fastio;

import fasttheme.FastTheme;
import fastdwm.FastDWM;
import fastwindow.FastWindow;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FastIO Demo — Java NIO vs FastIO 1GB Copy Comparison
 */
public class Demo extends Canvas {
    
    private static final long TOTAL_SIZE = 1024L * 1024 * 1024; // 1GB
    private static final Color BG_COLOR = new Color(8, 8, 8);
    private static final Color BRAND_BLUE = new Color(0x1a, 0x4d, 0x8c);
    private static final Color BRAND_AQUA = new Color(0x3e, 0xcc, 0xcc);
    private static final Color BRAND_PURPLE = new Color(0x7c, 0x3a, 0xed);
    private static final Color TEXT_COLOR = new Color(0xF0, 0xF0, 0xF0);
    
    private final ProgressInfo javaInfo = new ProgressInfo("Java NIO");
    private final ProgressInfo fastInfo = new ProgressInfo("FastIO");
    
    private BufferedImage bakedBackground;
    private boolean running = true;

    public Demo() {
        setBackground(BG_COLOR);
    }

    private void precomputeBackground(int w, int h) {
        bakedBackground = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bakedBackground.createGraphics();
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    public void start() {
        createBufferStrategy(2);
        BufferStrategy bs = getBufferStrategy();
        
        // Render Loop
        new Thread(() -> {
            while (running) {
                FastDWM.waitForVSync();
                render(bs);
            }
        }, "Render-Loop").start();

        // Copy Tasks
        new Thread(() -> runComparison(), "Comparison-Thread").start();
    }

    private void render(BufferStrategy bs) {
        Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
        int w = getWidth();
        int h = getHeight();
        
        if (bakedBackground == null || bakedBackground.getWidth() != w) {
            precomputeBackground(w, h);
        }
        
        g2.drawImage(bakedBackground, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw Rows
        int rowH = h / 2;
        drawProgressRow(g2, javaInfo, 0, rowH, w, BRAND_BLUE, BRAND_AQUA);
        drawProgressRow(g2, fastInfo, rowH, rowH, w, BRAND_PURPLE, BRAND_AQUA);
        
        // Separator removed as per request

        g2.dispose();
        bs.show();
    }

    private void drawProgressRow(Graphics2D g2, ProgressInfo info, int yOffset, int rowH, int w, Color colorStart, Color colorEnd) {
        int padding = 60;
        int barW = w - (padding * 2);
        int barH = 120;
        int barX = padding;
        int barY = yOffset + (rowH / 2) - (barH / 2) - 20;

        // Progress Bar Background
        g2.setColor(new Color(20, 20, 20));
        g2.fillRoundRect(barX, barY, barW, barH, 20, 20);
        g2.setColor(new Color(255, 255, 255, 15));
        g2.drawRoundRect(barX, barY, barW, barH, 20, 20);

        // Progress Bar Fill (Masked Growth)
        double progress = (double) info.current.get() / TOTAL_SIZE;
        if (progress > 0) {
            int fillW = (int) (barW * progress);
            if (fillW < 20) fillW = 20;
            
            Shape oldClip = g2.getClip();
            g2.setClip(new RoundRectangle2D.Double(barX, barY, barW, barH, 20, 20));
            
            GradientPaint fillGrad = new GradientPaint(barX, barY, colorStart, barX + barW, barY, colorEnd);
            g2.setPaint(fillGrad);
            g2.fillRoundRect(barX, barY, fillW, barH, 20, 20);
            
            g2.setPaint(new GradientPaint(0, barY, new Color(255, 255, 255, 30), 0, barY + barH/2, new Color(255, 255, 255, 0)));
            g2.fillRect(barX, barY, fillW, barH / 2);
            
            g2.setClip(oldClip);
        }

        // Stats Text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(TEXT_COLOR);
        String label = info.name;
        g2.drawString(label, barX, barY + barH + 40);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        g2.setColor(new Color(200, 200, 200));
        String stats = String.format("— %d MB / %d MB (%.1f MB/s)", 
            info.current.get() / (1024 * 1024), 
            TOTAL_SIZE / (1024 * 1024),
            info.speed);
        
        int labelW = g2.getFontMetrics(new Font("Segoe UI", Font.BOLD, 22)).stringWidth(label);
        g2.drawString(stats, barX + labelW + 10, barY + barH + 40);
    }

    private void runComparison() {
        try {
            Path sourceFile = Files.createTempFile("fastio_demo_src_", ".bin");
            Path javaTarget = Files.createTempFile("fastio_demo_java_", ".bin");
            Path fastTarget = Files.createTempFile("fastio_demo_fast_", ".bin");
            
            System.out.println("Creating 1GB test file...");
            byte[] chunk = new byte[1024 * 1024]; // 1MB
            try (OutputStream os = Files.newOutputStream(sourceFile)) {
                for (int i = 0; i < 1024; i++) os.write(chunk);
            }

            Thread javaThread = new Thread(() -> runJavaCopy(sourceFile, javaTarget));
            Thread fastThread = new Thread(() -> runFastIOCopy(sourceFile, fastTarget));

            System.out.println("Starting Benchmark Comparison...");
            javaThread.start();
            fastThread.start();

            javaThread.join();
            fastThread.join();

            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(javaTarget);
            Files.deleteIfExists(fastTarget);
            
            System.out.println("Comparison complete.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runJavaCopy(Path src, Path dst) {
        long startTime = System.currentTimeMillis();
        try (InputStream is = Files.newInputStream(src);
             OutputStream os = Files.newOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
                javaInfo.current.addAndGet(n);
                updateSpeed(javaInfo, startTime);
                if (javaInfo.current.get() % (100 * 1024 * 1024) == 0) {
                    System.out.printf("[DEBUG] Java NIO: %.1f MB/s\n", javaInfo.speed);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void runFastIOCopy(Path src, Path dst) {
        long startTime = System.currentTimeMillis();
        try {
            FastIO.init();
            
            // Peak Performance: Memory Map the source file
            ByteBuffer sourceMap = FastIO.mapFile(src.toString(), TOTAL_SIZE);
            
            try (var writer = FastIO.openWrite(dst.toString())) {
                // Use a larger buffer for native write
                ByteBuffer buffer = FastFile.allocateAlignedBuffer(2 * 1024 * 1024); // 2MB
                
                while (sourceMap.hasRemaining()) {
                    int toRead = Math.min(sourceMap.remaining(), buffer.capacity());
                    buffer.clear();
                    
                    // Zero-copy: Direct transfer from mapped memory to unbuffered writer
                    int oldLimit = sourceMap.limit();
                    sourceMap.limit(sourceMap.position() + toRead);
                    buffer.put(sourceMap);
                    sourceMap.limit(oldLimit);
                    
                    buffer.flip();
                    
                    writer.write(buffer);
                    fastInfo.current.addAndGet(toRead);
                    updateSpeed(fastInfo, startTime);
                    
                    if (fastInfo.current.get() % (100 * 1024 * 1024) == 0) {
                        System.out.printf("[DEBUG] FastIO (Mapped): %.1f MB/s\n", fastInfo.speed);
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void updateSpeed(ProgressInfo info, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 0) {
            info.speed = (info.current.get() / (1024.0 * 1024.0)) / (elapsed / 1000.0);
        }
    }

    private static class ProgressInfo {
        String name;
        AtomicLong current = new AtomicLong(0);
        double speed = 0;
        ProgressInfo(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        FastIO.DEMO_MODE = true; // Engage high-performance demo pipeline
        
        JFrame frame = new JFrame("FastIO — Native Performance Comparison");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 500);
        
        Demo demo = new Demo();
        frame.add(demo);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Timer styleTimer = new Timer(100, e -> {
            FastWindow window = FastWindow.attach(frame);
            window.setConstraints(800, 400, 0, 0);
            window.setBackgroundColor(8, 8, 8);
            
            long hwnd = window.getHWND();
            if (hwnd != 0) {
                FastTheme.setTitleBarDarkMode(hwnd, true);
                FastTheme.setTitleBarColor(hwnd, 8, 8, 8);
                FastTheme.setWindowTransparency(hwnd, 230);
            }
        });
        styleTimer.setRepeats(false);
        styleTimer.start();

        demo.start();
    }
}
