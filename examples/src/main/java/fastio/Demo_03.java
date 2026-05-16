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
 * FastIO Demo 03 — SIMD Log Grepper
 * Demonstrates pattern searching with AVX2/POPCNT vs Standard Java.
 */
public class Demo_03 extends Canvas {
    
    private static final long TOTAL_SIZE = 1024L * 1024 * 1024; // 1GB
    private static final String SEARCH_PATTERN = "CRITICAL_ERROR_CORE_FAILURE";
    private static final Color BG_COLOR = new Color(8, 8, 8);
    private static final Color TEXT_COLOR = new Color(0xF0, 0xF0, 0xF0);
    
    private final ProgressInfo javaInfo = new ProgressInfo("Standard Java (readLine + contains)");
    private final ProgressInfo fastInfo = new ProgressInfo("FastIO SIMD (nativeSearch)");
    
    private String vectorizationMode = "Detecting...";
    private BufferedImage bakedBackground;
    private boolean running = true;

    public Demo_03() {
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
        
        // Detect CPU features
        FastIO.init();
        int features = FastIO.nativeGetCPUFeatures();
        if ((features & 2) != 0) vectorizationMode = "AVX2 (32-byte SIMD)";
        else if ((features & 1) != 0) vectorizationMode = "POPCNT (Bit-magic)";
        else vectorizationMode = "Scalar (Standard)";

        new Thread(() -> {
            while (running) {
                FastDWM.waitForVSync();
                render(bs);
            }
        }, "Render-Loop").start();

        new Thread(() -> runGrepperBenchmark(), "Benchmark-Thread").start();
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

        // Header
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.setColor(new Color(150, 150, 150));
        g2.drawString("LOG PATTERN SEARCH: \"" + SEARCH_PATTERN + "\"", 60, 50);
        g2.setColor(new Color(0x3e, 0xcc, 0xcc));
        g2.drawString("MODE: " + vectorizationMode, w - 300, 50);

        int rowH = (h - 100) / 2;
        drawProgressRow(g2, javaInfo, 100, rowH, w, new Color(0x1a, 0x4d, 0x8c), new Color(0x3e, 0xcc, 0xcc));
        drawProgressRow(g2, fastInfo, 100 + rowH, rowH, w, new Color(0x7c, 0x3a, 0xed), new Color(0x3e, 0xcc, 0xcc));
        
        g2.dispose();
        bs.show();
    }

    private void drawProgressRow(Graphics2D g2, ProgressInfo info, int yOffset, int rowH, int w, Color colorStart, Color colorEnd) {
        int padding = 60;
        int barW = w - (padding * 2);
        int barH = 100;
        int barX = padding;
        int barY = yOffset + (rowH / 2) - (barH / 2);

        // Bar Background
        g2.setColor(new Color(25, 25, 25));
        g2.fillRoundRect(barX, barY, barW, barH, 15, 15);

        // Progress Fill
        double progress = (double) info.processed.get() / TOTAL_SIZE;
        if (progress > 0) {
            int fillW = (int) (barW * Math.min(1.0, progress));
            Shape oldClip = g2.getClip();
            g2.setClip(new RoundRectangle2D.Double(barX, barY, barW, barH, 15, 15));
            g2.setPaint(new GradientPaint(barX, barY, colorStart, barX + barW, barY, colorEnd));
            g2.fillRoundRect(barX, barY, fillW, barH, 15, 15);
            g2.setClip(oldClip);
        }

        // Labels
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.setColor(TEXT_COLOR);
        g2.drawString(info.name, barX, barY - 15);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g2.setColor(new Color(180, 180, 180));
        String stats = String.format("%.2f GB/s — Matches: %d", info.speed / 1024.0, info.matches.get());
        g2.drawString(stats, barX, barY + barH + 30);
    }

    private void runGrepperBenchmark() {
        try {
            Path testFile = Files.createTempFile("fastio_log_test_", ".log");
            System.out.println("Generating 1GB dummy log file...");
            byte[] normalLine = "INFO  [2026-05-02] System working as expected. Normal operations.\n".getBytes();
            byte[] errorLine = ("CRITICAL  [2026-05-02] " + SEARCH_PATTERN + " at sector 0x7F22\n").getBytes();
            
            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(testFile))) {
                long written = 0;
                long lastError = 0;
                while (written < TOTAL_SIZE) {
                    os.write(normalLine);
                    written += normalLine.length;
                    if (written - lastError > 5 * 1024 * 1024) { 
                        os.write(errorLine);
                        written += errorLine.length;
                        lastError = written;
                    }
                }
            }

            System.out.println("Starting Grepper Benchmark...");
            Thread javaThread = new Thread(() -> runJavaGrep(testFile));
            Thread fastThread = new Thread(() -> runFastSIMDGrep(testFile));
            
            javaThread.start();
            fastThread.start();
            
            javaThread.join();
            fastThread.join();
            
            Files.deleteIfExists(testFile);
            System.out.println("Benchmark complete.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runJavaGrep(Path path) {
        long startTime = System.nanoTime();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(SEARCH_PATTERN)) {
                    javaInfo.matches.incrementAndGet();
                }
                javaInfo.processed.addAndGet(line.length() + 1);
                updateSpeed(javaInfo, startTime);
            }
        } catch (IOException e) { e.printStackTrace(); }
        long elapsed = System.nanoTime() - startTime;
        double finalSpeed = (TOTAL_SIZE / (1024.0 * 1024.0)) / (elapsed / 1_000_000_000.0);
        System.out.printf("[RESULT] Java Grep: %.1f MB/s (Matches: %d)\n", finalSpeed, javaInfo.matches.get());
    }

    private void runFastSIMDGrep(Path path) {
        long startTime = System.nanoTime();
        byte[] pattern = SEARCH_PATTERN.getBytes();
        try {
            ByteBuffer mmap = FastIO.mapFile(path.toString(), TOTAL_SIZE);
            int chunkSize = 4 * 1024 * 1024; // 4MB chunks for smooth updates
            
            while (mmap.hasRemaining()) {
                int pos = mmap.position();
                int rem = Math.min(mmap.remaining(), chunkSize);
                
                // Native SIMD Search
                int offset = 0;
                while (offset < rem) {
                    int found = FastIO.nativeSearch(mmap, pos + offset, rem - offset, pattern);
                    if (found == -1) break;
                    fastInfo.matches.incrementAndGet();
                    offset = found + pattern.length;
                }
                
                mmap.position(pos + rem);
                fastInfo.processed.addAndGet(rem);
                updateSpeed(fastInfo, startTime);
            }
        } catch (IOException e) { e.printStackTrace(); }
        long elapsed = System.nanoTime() - startTime;
        double finalSpeed = (TOTAL_SIZE / (1024.0 * 1024.0)) / (elapsed / 1_000_000_000.0);
        System.out.printf("[RESULT] FastIO SIMD Grep: %.1f GB/s (Matches: %d)\n", finalSpeed / 1024.0, fastInfo.matches.get());
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
        AtomicLong matches = new AtomicLong(0);
        double speed = 0; 
        ProgressInfo(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("FastIO — SIMD Log Grepper Comparison");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        Demo_03 demo = new Demo_03();
        frame.add(demo);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Timer styleTimer = new Timer(100, e -> {
            FastWindow window = FastWindow.attach(frame);
            window.setBackgroundColor(8, 8, 8);
            long hwnd = window.getHWND();
            if (hwnd != 0) {
                FastTheme.setTitleBarDarkMode(hwnd, true);
                FastTheme.setTitleBarColor(hwnd, 8, 8, 8);
                FastTheme.setWindowTransparency(hwnd, 240);
            }
        });
        styleTimer.setRepeats(false);
        styleTimer.start();
        demo.start();
    }
}
