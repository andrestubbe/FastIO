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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FastIO Demo 02 — Processing Speed Comparison (mmap + nativeScan)
 * Demonstrates the massive throughput difference when CPU-bound.
 */
public class Demo_02 extends Canvas {
    
    private static final long TOTAL_SIZE = 1024L * 1024 * 1024; // 1GB
    private static final Color BG_COLOR = new Color(8, 8, 8);
    private static final Color JAVA_COLOR_START = new Color(0x1a, 0x4d, 0x8c); // Deep Blue
    private static final Color JAVA_COLOR_END = new Color(0x3e, 0xcc, 0xcc);   // Aqua
    private static final Color FAST_COLOR_START = new Color(0x7c, 0x3a, 0xed); // Purple
    private static final Color FAST_COLOR_END = new Color(0x3e, 0xcc, 0xcc);   // Aqua
    private static final Color TEXT_COLOR = new Color(0xF0, 0xF0, 0xF0);
    
    private final ProgressInfo javaInfo = new ProgressInfo("Java NIO (Manual Scan)");
    private final ProgressInfo fastInfo = new ProgressInfo("FastIO (Native mmap + Scan)");
    
    private BufferedImage bakedBackground;
    private boolean running = true;

    public Demo_02() {
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

        // Comparison Task
        new Thread(() -> runBenchmark(), "Benchmark-Thread").start();
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

        int rowH = h / 2;
        drawProgressRow(g2, javaInfo, 0, rowH, w, JAVA_COLOR_START, JAVA_COLOR_END);
        drawProgressRow(g2, fastInfo, rowH, rowH, w, FAST_COLOR_START, FAST_COLOR_END);
        
        g2.dispose();
        bs.show();
    }

    private void drawProgressRow(Graphics2D g2, ProgressInfo info, int yOffset, int rowH, int w, Color colorStart, Color colorEnd) {
        int padding = 60;
        int barW = w - (padding * 2);
        int barH = 120;
        int barX = padding;
        int barY = yOffset + (rowH / 2) - (barH / 2) - 20;

        // Background
        g2.setColor(new Color(20, 20, 20));
        g2.fillRoundRect(barX, barY, barW, barH, 20, 20);
        g2.setColor(new Color(255, 255, 255, 15));
        g2.drawRoundRect(barX, barY, barW, barH, 20, 20);

        // Progress
        double progress = (double) info.processed.get() / TOTAL_SIZE;
        if (progress > 0) {
            int fillW = (int) (barW * Math.min(1.0, progress));
            Shape oldClip = g2.getClip();
            g2.setClip(new RoundRectangle2D.Double(barX, barY, barW, barH, 20, 20));
            
            g2.setPaint(new GradientPaint(barX, barY, colorStart, barX + barW, barY, colorEnd));
            g2.fillRoundRect(barX, barY, fillW, barH, 20, 20);
            
            g2.setPaint(new GradientPaint(0, barY, new Color(255, 255, 255, 30), 0, barY + barH/2, new Color(255, 255, 255, 0)));
            g2.fillRect(barX, barY, fillW, barH / 2);
            g2.setClip(oldClip);
        }

        // Stats
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(TEXT_COLOR);
        g2.drawString(info.name, barX, barY + barH + 40);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        g2.setColor(new Color(200, 200, 200));
        String stats = String.format("— %.2f GB processed (%.1f GB/s)", 
            info.processed.get() / (1024.0 * 1024.0 * 1024.0), 
            info.speed / 1024.0);
        
        int labelW = g2.getFontMetrics(new Font("Segoe UI", Font.BOLD, 22)).stringWidth(info.name);
        g2.drawString(stats, barX + labelW + 10, barY + barH + 40);
    }

    private void runBenchmark() {
        try {
            Path testFile = Files.createTempFile("fastio_perf_test_", ".bin");
            System.out.println("Generating 1GB dummy text file...");
            byte[] line = "This is a performance test line for the FastJava ecosystem benchmark.\n".getBytes();
            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(testFile))) {
                long written = 0;
                while (written < TOTAL_SIZE) {
                    os.write(line);
                    written += line.length;
                }
            }

            System.out.println("Starting Processing Benchmark (Line Counting)...");
            
            Thread javaThread = new Thread(() -> runJavaScan(testFile));
            Thread fastThread = new Thread(() -> runFastScan(testFile));
            
            javaThread.start();
            fastThread.start();
            
            javaThread.join();
            fastThread.join();

            Files.deleteIfExists(testFile);
            System.out.println("Benchmark complete.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runJavaScan(Path path) {
        long startTime = System.nanoTime();
        long count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                javaInfo.processed.addAndGet(line.length() + 1);
                updateSpeed(javaInfo, startTime);
                if (javaInfo.processed.get() % (1024 * 1024) == 0) {
                    // Update UI frequently, but log only every 128MB
                    if (javaInfo.processed.get() % (128 * 1024 * 1024) == 0) {
                        System.out.printf("[DEBUG] Standard Java Scan: %.1f MB/s\n", javaInfo.speed);
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        long elapsed = System.nanoTime() - startTime;
        double finalSpeed = (TOTAL_SIZE / (1024.0 * 1024.0)) / (elapsed / 1_000_000_000.0);
        System.out.printf("[RESULT] Standard Java Final Speed: %.1f MB/s\n", finalSpeed);
        System.out.println("Java Count: " + count);
    }

    private void runFastScan(Path path) {
        FastIO.init();
        FastIO.DEMO_MODE = true;
        long startTime = System.nanoTime();
        long count = 0;
        try {
            ByteBuffer mmap = FastIO.mapFile(path.toString(), TOTAL_SIZE);
            int chunkSize = 4 * 1024 * 1024; // 4MB chunks for liquid-smooth animation
            
            while (mmap.hasRemaining()) {
                int pos = mmap.position();
                int rem = Math.min(mmap.remaining(), chunkSize);
                
                count += FastIO.nativeCount(mmap, pos, rem, (byte)'\n');
                
                mmap.position(pos + rem);
                fastInfo.processed.addAndGet(rem);
                updateSpeed(fastInfo, startTime);
                
                if (fastInfo.processed.get() % (512 * 1024 * 1024) == 0) {
                    System.out.printf("[DEBUG] FastIO Scan: %.1f GB/s\n", fastInfo.speed / 1024.0);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        long elapsed = System.nanoTime() - startTime;
        double finalSpeed = (TOTAL_SIZE / (1024.0 * 1024.0)) / (elapsed / 1_000_000_000.0);
        System.out.printf("[RESULT] FastIO Final Speed: %.1f GB/s\n", finalSpeed / 1024.0);
        System.out.println("FastIO Count: " + count);
    }

    private void updateSpeed(ProgressInfo info, long startNano) {
        long elapsed = System.nanoTime() - startNano;
        if (elapsed > 0) {
            double seconds = elapsed / 1_000_000_000.0;
            info.speed = (info.processed.get() / (1024.0 * 1024.0)) / seconds;
        }
    }

    private static class ProgressInfo {
        String name;
        AtomicLong processed = new AtomicLong(0);
        double speed = 0; // MB/s
        ProgressInfo(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("FastIO — Processing Throughput (mmap + nativeScan)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 500);
        
        Demo_02 demo = new Demo_02();
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
