/*
 * เขาวงกตตัวเลข (Number Maze) — รวมโค้ดทั้ง 20 คลาสไว้ในไฟล์เดียว
 * ผู้จัดทำ: 6804062611033 นายวัชรเศรษฐ์ ว่องรัตน์วานิช
 *
 * Java อนุญาตให้มี public class ได้แค่ 1 คลาสต่อไฟล์ จึงให้ NumberMaze (เดิมคือ Main) เป็น public
 * ส่วนคลาสอื่นเป็นคลาสธรรมดา (package-private) ที่อยู่ในไฟล์เดียวกัน การทำงานเหมือนเดิมทุกอย่าง
 *
 * วิธีรัน (ต้องมี Java 17 ขึ้นไป) — รันจากโฟลเดอร์หลักของโปรเจ็ค เพื่อให้หาโฟลเดอร์ images/ เจอ:
 *     java -Dfile.encoding=UTF-8 single-file/NumberMaze.java
 * หรือ compile ก่อนแล้วค่อยรัน:
 *     javac -encoding UTF-8 -d out single-file/NumberMaze.java
 *     java -cp out NumberMaze
 */
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

// ======================================================================
// Main.java  (เดิมชื่อคลาส Main)
// ======================================================================
public class NumberMaze {
    public static void main(String[] args) {
        useThaiFont();

        JFrame frame = new JFrame("Number Maze");      // สร้างหน้าต่าง
        frame.add(new GamePanel());                    // ใส่พื้นที่วาดเกมลงไป
        frame.pack();                                  // ปรับขนาดหน้าต่างให้พอดีกับ GamePanel
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);             // ให้หน้าต่างอยู่กลางจอ
        frame.setVisible(true);
    }

    /**
     * ฟอนต์เริ่มต้นของ Swing ไม่มีตัวอักษรไทย ทำให้กล่องข้อความ (JOptionPane) แสดงเป็นสี่เหลี่ยม □□□
     * จึงเปลี่ยนฟอนต์เริ่มต้นของทุก component เป็น Tahoma ซึ่งรองรับภาษาไทย
     */
    private static void useThaiFont() {
        FontUIResource thai = new FontUIResource("Tahoma", Font.PLAIN, 14);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, thai);
            }
        }
    }
}

// ======================================================================
// GamePanel.java
// ======================================================================
class GamePanel extends JPanel {           // Inheritance: สืบทอดจาก JPanel
    private static final int TILE = 44;           // ขนาดช่องละ 44 พิกเซล
    private static final int HUD_HEIGHT = 112;    // ความสูงแถบข้อมูลด้านล่าง
    private static final Font THAI_FONT = new Font("Tahoma", Font.BOLD, 15);

    private int levelIndex;
    private Level level;                          // Composition: GamePanel "มี" Level
    private Player player;                        // Composition: GamePanel "มี" Player
    private String message = "";                  // ข้อความแจ้งเตือนใน HUD
    private boolean gameWon = false;
    private final Timer timer;                    // นาฬิกาของเกม ใช้ขยับอุปสรรค

    public GamePanel() {
        loadLevel(0);
        setFocusable(true);                      // ต้องเปิด ไม่งั้น JPanel จะรับปุ่มกดไม่ได้

        // ฟังการกดปุ่มคีย์บอร์ด
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameWon) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        gameWon = false;
                        loadLevel(0);             // เล่นใหม่ตั้งแต่ด่านแรก
                    }
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP    -> tryMove(0, -1);   // ขึ้น
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN  -> tryMove(0, 1);    // ลง
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> tryMove(-1, 0);   // ซ้าย
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> tryMove(1, 0);    // ขวา
                    case KeyEvent.VK_R -> {                                    // เริ่มด่านใหม่
                        loadLevel(levelIndex);
                        message = "เริ่มด่านใหม่แล้ว";
                    }
                }
            }
        });

        // ทุก 0.2 วินาที ให้อุปสรรคขยับ แล้วเช็คว่าผู้เล่นโดนหรือไม่
        timer = new Timer(200, e -> tick());
        timer.start();
    }

    /** โหลดด่านใหม่: สร้างแผนที่และผู้เล่นใหม่ (กระเป๋าว่าง) */
    private void loadLevel(int index) {
        levelIndex = index;
        level = Levels.create(index);
        player = new Player(level.getStartX(), level.getStartY());
        message = "";

        // แต่ละด่านขนาดไม่เท่ากัน → ปรับขนาดหน้าต่างให้พอดีกับแผนที่
        setPreferredSize(new Dimension(level.getWidth() * TILE, level.getHeight() * TILE + HUD_HEIGHT));
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {                     // ตอนเริ่มเกมครั้งแรกยังไม่มีหน้าต่าง Main จะ pack() ให้เอง
            window.pack();
            window.setLocationRelativeTo(null);
        }
        repaint();
    }

    private void tick() {
        if (gameWon) {
            return;
        }
        for (Obstacle obstacle : level.getObstacles()) {
            obstacle.update(level);               // Polymorphism: หนามกับผู้พิทักษ์ขยับไม่เหมือนกัน
        }
        checkObstacles();
        repaint();
    }

    /** ถ้าผู้เล่นยืนอยู่ช่องเดียวกับอุปสรรคที่กำลังอันตราย → ส่งกลับจุดเริ่มต้น (ไอเทมยังอยู่ในกระเป๋า) */
    private void checkObstacles() {
        for (Obstacle obstacle : level.getObstacles()) {
            if (obstacle.isDangerous() && obstacle.getX() == player.getX() && obstacle.getY() == player.getY()) {
                message = obstacle.getHitMessage();
                player.moveTo(level.getStartX(), level.getStartY());
                return;
            }
        }
    }

    /** พยายามเดินไป dx, dy ช่อง ถ้าเป็นกำแพงจะเดินไม่ได้ ถ้าเป็นประตูที่ล็อกอยู่จะเรียก interact() */
    private void tryMove(int dx, int dy) {
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (level.isWall(newX, newY)) {
            return;                               // ชนกำแพง → ไม่ขยับ
        }

        Door door = level.findDoorAt(newX, newY);
        if (door != null && door.isLocked()) {
            timer.stop();                         // หยุดเวลาระหว่างแก้สมการ ผู้พิทักษ์จะได้ไม่เดินมาชน
            door.interact(player, this);          // Polymorphism: PuzzleDoor เปิดหน้าต่างสมการ, ExitDoor จบด่าน
            timer.start();
            repaint();
            requestFocusInWindow();               // เอาโฟกัสคีย์บอร์ดกลับมาที่เกมหลังปิดหน้าต่าง
            return;
        }

        player.moveTo(newX, newY);

        Item item = level.takeItemAt(newX, newY);
        if (item != null) {
            player.getInventory().add(item);
            message = "เก็บ " + item.getSymbol() + " แล้ว";
        }
        checkObstacles();
        repaint();
    }

    /** ExitDoor เรียกเมธอดนี้เมื่อผู้เล่นเข้าประตูชัย */
    public void completeLevel() {
        if (levelIndex + 1 < Levels.count()) {
            JOptionPane.showMessageDialog(this, "ผ่าน" + level.getName() + " แล้ว!\nไปด่านต่อไปกันเลย",
                    "ผ่านด่าน", JOptionPane.INFORMATION_MESSAGE);
            loadLevel(levelIndex + 1);
        } else {
            gameWon = true;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {   // Polymorphism: override เมธอดของ JPanel
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1) พื้นและกำแพง
        for (int row = 0; row < level.getHeight(); row++) {
            for (int col = 0; col < level.getWidth(); col++) {
                boolean wall = level.isWall(col, row);
                Image tileImage = wall ? Sprites.WALL : Sprites.FLOOR;
                if (tileImage != null) {
                    g.drawImage(tileImage, col * TILE, row * TILE, TILE, TILE, null);
                } else {
                    g.setColor(wall ? new Color(60, 60, 70) : new Color(205, 205, 200));
                    g.fillRect(col * TILE, row * TILE, TILE, TILE);
                }
            }
        }

        // 2) วาดทุกอย่างในด่าน — แต่ละ Object รู้วิธีวาดตัวเอง (Polymorphism)
        for (Item item : level.getItems())             item.draw(g, TILE);
        for (Door door : level.getDoors())             door.draw(g, TILE);
        for (Obstacle obstacle : level.getObstacles()) obstacle.draw(g, TILE);
        player.draw(g, TILE);

        // 3) แถบข้อมูลด้านล่าง
        drawHud(g);

        // 4) หน้าจอชนะ
        if (gameWon) {
            drawWinScreen(g);
        }
    }

    private void drawHud(Graphics g) {
        int top = level.getHeight() * TILE;
        g.setColor(new Color(30, 30, 40));
        g.fillRect(0, top, getWidth(), HUD_HEIGHT);

        // บรรทัดที่ 1: ชื่อด่าน (ซ้าย) + วิธีเริ่มใหม่ (ขวา)
        g.setFont(THAI_FONT);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(level.getName() + "  (" + (levelIndex + 1) + "/" + Levels.count() + ")", 10, top + 22);

        g.setColor(new Color(160, 160, 170));
        String hint = "R = เริ่มด่านใหม่";
        g.drawString(hint, getWidth() - fm.stringWidth(hint) - 10, top + 22);

        // บรรทัดที่ 2: ข้อความแจ้งเตือน (กึ่งกลาง)
        g.setColor(new Color(255, 210, 90));
        g.drawString(message, (getWidth() - fm.stringWidth(message)) / 2, top + 46);

        // บรรทัดที่ 3: Inventory
        g.setColor(Color.WHITE);
        g.drawString("กระเป๋า:", 10, top + 88);
        int x = 80;
        for (Item item : player.getInventory().getItems()) {
            item.drawAt(g, x, top + 58, 46);
            x += 46;
        }
    }

    private void drawWinScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 190));                  // พื้นดำโปร่งแสง
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setFont(new Font("Arial", Font.BOLD, 56));
        FontMetrics fm = g.getFontMetrics();
        String title = "YOU SURVIVED!";
        int y = getHeight() / 2 - 10;
        g.setColor(new Color(255, 215, 0));
        g.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, y);

        g.setFont(new Font("Tahoma", Font.PLAIN, 20));
        fm = g.getFontMetrics();
        String sub = "คุณหนีออกจากเขาวงกตได้สำเร็จ — กด Enter เพื่อเล่นใหม่";
        g.setColor(Color.WHITE);
        g.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, y + 50);
    }
}

// ======================================================================
// Levels.java
// ======================================================================
/**
 * เก็บข้อมูลแผนที่ของทุกด่าน
 * create() สร้าง Level ใหม่ทุกครั้ง จึงใช้ตอน "เริ่มด่านใหม่" ได้ด้วย (ไอเทมและประตูกลับมาครบ)
 */
class Levels {

    private Levels() { }                        // คลาสนี้ไม่ต้องสร้าง Object

    public static int count() {
        return 3;
    }

    public static Level create(int index) {
        return switch (index) {
            // ด่าน 1: ไม่มีอุปสรรค ฝึกเดินสำรวจและเก็บไอเทม
            // a: 3 + 4 = 7   |   b: 2 * 5 = 10 (ต้องเก็บ 2 ไว้ใช้ในห้องสุดท้าย)
            case 0 -> new Level("ด่าน 1: ห้องฝึกหัด", new String[]{
                "###############",
                "#P....#.......#",
                "####..#.#####.#",
                "#+....#.#3..#.#",
                "###.###.#.###.#",
                "#...#...#.....#",
                "#.###.#########",
                "#......4....a.#",
                "#####.#######.#",
                "#2......#E.b*5#",
                "###############",
            }, Map.of('a', 7, 'b', 10));

            // ด่าน 2: กับดักหนาม ต้องรอจังหวะหนามหด
            // a: 9 - 6 / 2 = 6   |   b: 3 + 1 = 4
            case 1 -> new Level("ด่าน 2: ทางหนาม", new String[]{
                "###############",
                "#P..#.....^^^9#",
                "#.#.#.#########",
                "#.#...#-......#",
                "#.#####.#####.#",
                "#.....^^#6..#.#",
                "#####.#.#.###.#",
                "#2....#...#...#",
                "###.#######a###",
                "#/..#Eb..3+..1#",
                "###############",
            }, Map.of('a', 6, 'b', 4));

            // ด่าน 3: แผนที่ใหญ่ 21x13, หนาม + ผู้พิทักษ์ 2 ตัว, มีไอเทมหลอก
            // a: 7 * 3 - 5 = 16   |   b: 9 + 8 / 2 = 13 (ต้องเก็บ 2 จากห้องแรกไว้)   |   c: 6 * 4 - 7 + 1 = 18
            case 2 -> new Level("ด่าน 3: ผู้พิทักษ์", new String[]{
                "#####################",
                "#P..#.^^.2#..+..#.^8#",
                "#.#.#.##..#.###.#.###",
                "#.#...#-..a...G.....#",
                "#.###.#.####.#.#.#.##",
                "#7..*.#3+5##/#6#9#.##",
                "##################b##",
                "#E#..#4.#...^^^.....#",
                "#.#..#.##.#.###.###.#",
                "#....c..G.........-.#",
                "#.##.##.#.#####.#.#.#",
                "#....#1.#*....#7..#/#",
                "#####################",
            }, Map.of('a', 16, 'b', 13, 'c', 18));

            default -> throw new IllegalArgumentException("ไม่มีด่านที่ " + index);
        };
    }
}

// ======================================================================
// Level.java
// ======================================================================
/**
 * ด่านหนึ่งด่าน — อ่านแผนที่จากข้อความแล้วสร้าง Object ต่างๆ ในด่าน
 *
 * สัญลักษณ์ในแผนที่:
 *   #  กำแพง            .  ทางเดิน          P  จุดเริ่มต้นผู้เล่น
 *   0-9  ตัวเลข          + - * /  เครื่องหมาย
 *   a-z  ประตูภารกิจ (เป้าหมายกำหนดใน doorTargets)
 *   E  ประตูชัย          ^  กับดักหนาม       G  ผู้พิทักษ์
 */
class Level {
    private final String name;
    private final char[][] grid;
    private final List<Item> items = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private int startX, startY;

    public Level(String name, String[] map, Map<Character, Integer> doorTargets) {
        this.name = name;
        grid = new char[map.length][];
        for (int row = 0; row < map.length; row++) {
            grid[row] = map[row].toCharArray();
            for (int col = 0; col < grid[row].length; col++) {
                char c = grid[row][col];
                if (c == '#' || c == '.') {
                    continue;                                   // กำแพง/ทางเดิน เก็บไว้ใน grid ตามเดิม
                }

                if (c == 'P') {
                    startX = col;
                    startY = row;
                } else if (Character.isDigit(c)) {
                    items.add(new NumberItem(col, row, c - '0'));
                } else if ("+-*/".indexOf(c) >= 0) {
                    items.add(new OperatorItem(col, row, c));
                } else if (c >= 'a' && c <= 'z') {
                    Integer target = doorTargets.get(c);
                    if (target == null) {
                        throw new IllegalStateException("ประตู '" + c + "' ใน " + name + " ยังไม่ได้กำหนดเป้าหมาย");
                    }
                    doors.add(new PuzzleDoor(col, row, target));
                } else if (c == 'E') {
                    doors.add(new ExitDoor(col, row));
                } else if (c == '^') {
                    obstacles.add(new Spike(col, row));
                } else if (c == 'G') {
                    obstacles.add(new Guard(col, row));
                } else {
                    throw new IllegalStateException("ไม่รู้จักสัญลักษณ์ '" + c + "' ใน " + name);
                }
                grid[row][col] = '.';                           // ช่องที่มี Object อยู่ ถือเป็นทางเดิน
            }
        }
    }

    public String getName()   { return name; }
    public int getWidth()     { return grid[0].length; }
    public int getHeight()    { return grid.length; }
    public int getStartX()    { return startX; }
    public int getStartY()    { return startY; }

    public boolean isWall(int x, int y) {
        return grid[y][x] == '#';
    }

    /** ผู้พิทักษ์เดินผ่านกำแพงและประตูไม่ได้ (จะได้อยู่แต่ในห้องของตัวเอง) */
    public boolean isBlockedForGuard(int x, int y) {
        return isWall(x, y) || findDoorAt(x, y) != null;
    }

    /** หยิบไอเทมที่ (x, y) ออกจากแผนที่ ถ้าไม่มีคืนค่า null */
    public Item takeItemAt(int x, int y) {
        for (Item item : items) {
            if (item.getX() == x && item.getY() == y) {
                items.remove(item);
                return item;
            }
        }
        return null;
    }

    public Door findDoorAt(int x, int y) {
        for (Door door : doors) {
            if (door.getX() == x && door.getY() == y) {
                return door;
            }
        }
        return null;
    }

    public List<Item> getItems()         { return Collections.unmodifiableList(items); }
    public List<Door> getDoors()         { return Collections.unmodifiableList(doors); }
    public List<Obstacle> getObstacles() { return Collections.unmodifiableList(obstacles); }
}

// ======================================================================
// GameObject.java
// ======================================================================
/**
 * คลาสแม่ของทุกสิ่งในเกม (ผู้เล่น, ไอเทม, ประตู ...)
 * เป็น abstract เพราะ "วัตถุในเกม" เฉยๆ ไม่มีหน้าตา ต้องให้คลาสลูกกำหนดเองว่าจะวาดอย่างไร
 */
abstract class GameObject {
    protected int x, y;   // ตำแหน่งบนตาราง (x = คอลัมน์, y = แถว) — protected ให้คลาสลูกใช้ได้

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    /** คลาสลูกทุกตัวต้อง implement เมธอดนี้ (Abstract method) */
    public abstract void draw(Graphics g, int tileSize);
}

// ======================================================================
// Player.java
// ======================================================================
/** นักสำรวจ — ตัวละครที่ผู้เล่นควบคุม */
class Player extends GameObject {       // Inheritance: สืบทอดจาก GameObject
    private final Inventory inventory = new Inventory();   // Composition: ผู้เล่น "มี" กระเป๋า

    public Player(int x, int y) {
        super(x, y);                            // เรียก Constructor ของคลาสแม่
    }

    public void moveTo(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void draw(Graphics g, int tile) {
        if (Sprites.PLAYER != null) {
            // ตัวละครสูงกว่าช่อง จึงวาดสูง 1.3 เท่า โดยเท้าอยู่ขอบล่างของช่อง
            int h = (int) (tile * 1.3);
            Sprites.drawFit(g, Sprites.PLAYER, x * tile, (y + 1) * tile - h, tile, h);
        } else {
            g.setColor(Color.BLUE);             // ไม่มีรูป → วาดวงกลมสีน้ำเงินแทน
            g.fillOval(x * tile + 6, y * tile + 6, tile - 12, tile - 12);
        }
    }
}

// ======================================================================
// Inventory.java
// ======================================================================
/** กระเป๋าเก็บไอเทมของผู้เล่น */
class Inventory {
    private final List<Item> items = new ArrayList<>();   // Encapsulation: ข้างนอกแก้ list ตรงๆ ไม่ได้

    public void add(Item item) {
        items.add(item);
    }

    public void remove(Item item) {
        items.remove(item);
    }

    /** คืน list แบบอ่านอย่างเดียว เพื่อไม่ให้คลาสอื่นแอบเพิ่ม/ลบไอเทมเอง */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }
}

// ======================================================================
// Item.java
// ======================================================================
/**
 * ไอเทมที่เก็บได้ในเขาวงกต (ตัวเลข หรือ เครื่องหมาย)
 * วิธีวาดเหมือนกันทุกไอเทม แต่ "สัญลักษณ์" และ "สี" ให้คลาสลูกเป็นคนกำหนด
 */
abstract class Item extends GameObject {

    public Item(int x, int y) {
        super(x, y);
    }

    /** ข้อความที่แสดงบนไอเทม เช่น "3" หรือ "+" */
    public abstract String getSymbol();

    /** สีพื้นหลังของไอเทม */
    public abstract Color getColor();

    /** วาดไอเทมบนแผนที่ตามตำแหน่งในตาราง */
    @Override
    public void draw(Graphics g, int tile) {
        drawAt(g, x * tile, y * tile, tile);
    }

    /** วาดไอเทมที่ตำแหน่งพิกเซลใดก็ได้ (ใช้ทั้งบนแผนที่และในแถบ Inventory) */
    public void drawAt(Graphics g, int px, int py, int size) {
        int pad = size / 8;
        g.setColor(getColor());                               // Polymorphism: แต่ละคลาสลูกให้สีต่างกัน
        g.fillRoundRect(px + pad, py + pad, size - pad * 2, size - pad * 2, 10, 10);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, size / 2));
        FontMetrics fm = g.getFontMetrics();
        String s = getSymbol();                               // Polymorphism: แต่ละคลาสลูกให้สัญลักษณ์ต่างกัน
        int textX = px + (size - fm.stringWidth(s)) / 2;      // จัดให้อยู่กึ่งกลางช่อง
        int textY = py + (size + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(s, textX, textY);
    }
}

// ======================================================================
// NumberItem.java
// ======================================================================
/** ไอเทมตัวเลข 0-9 */
class NumberItem extends Item {
    private final int value;

    public NumberItem(int x, int y, int value) {
        super(x, y);
        this.value = value;
    }

    public int getValue() { return value; }

    @Override
    public String getSymbol() { return String.valueOf(value); }

    @Override
    public Color getColor() { return new Color(255, 230, 120); }   // สีเหลือง
}

// ======================================================================
// OperatorItem.java
// ======================================================================
/** ไอเทมเครื่องหมาย + - * / */
class OperatorItem extends Item {
    private final char operator;

    public OperatorItem(int x, int y, char operator) {
        super(x, y);
        this.operator = operator;
    }

    public char getOperator() { return operator; }

    @Override
    public String getSymbol() { return String.valueOf(operator); }

    @Override
    public Color getColor() { return new Color(140, 200, 255); }   // สีฟ้า
}

// ======================================================================
// Interactable.java
// ======================================================================
/** สิ่งที่ผู้เล่นเดินชนแล้ว "มีปฏิกิริยา" เช่น ประตูรหัสผ่าน, ประตูชัย */
interface Interactable {
    void interact(Player player, GamePanel game);
}

// ======================================================================
// Door.java
// ======================================================================
/**
 * คลาสแม่ของประตูทุกชนิด
 * ประตูทุกบานมีสถานะ ล็อก/ไม่ล็อก แต่ "ชนแล้วเกิดอะไรขึ้น" ให้คลาสลูกกำหนดเอง
 */
abstract class Door extends GameObject implements Interactable {
    private boolean locked = true;              // ประตูเริ่มต้นล็อกเสมอ

    public Door(int x, int y) {
        super(x, y);
    }

    public boolean isLocked() {
        return locked;
    }

    /** protected: ให้เฉพาะคลาสลูก (เช่น PuzzleDoor) เป็นคนปลดล็อกได้ คลาสอื่นปลดล็อกเองไม่ได้ */
    protected void unlock() {
        locked = false;
    }
}

// ======================================================================
// PuzzleDoor.java
// ======================================================================
/** ประตูภารกิจ — ต้องจัดเรียงสมการให้ได้ผลลัพธ์เท่ากับ target จึงจะเปิด */
class PuzzleDoor extends Door {
    private final int target;

    public PuzzleDoor(int x, int y, int target) {
        super(x, y);
        this.target = target;
    }

    public int getTarget() {
        return target;
    }

    /** เปิดหน้าต่างแก้สมการ ถ้าแก้ถูกประตูจะปลดล็อก */
    @Override
    public void interact(Player player, GamePanel game) {
        Window owner = SwingUtilities.getWindowAncestor(game);
        PuzzleDialog dialog = new PuzzleDialog(owner, target, player.getInventory());
        dialog.setVisible(true);                // หน้าต่างแบบ modal: โค้ดจะหยุดรอจนกว่าจะปิดหน้าต่าง
        if (dialog.isSolved()) {
            unlock();
        }
    }

    @Override
    public void draw(Graphics g, int tile) {
        int px = x * tile, py = y * tile;
        if (Sprites.PUZZLE_DOOR != null) {
            drawWithSprite((Graphics2D) g, px, py, tile);
            return;
        }
        if (isLocked()) {
            // ประตูปิด: สีส้ม + แสดงเป้าหมาย
            g.setColor(Color.ORANGE);
            g.fillRect(px, py, tile, tile);
            g.setColor(new Color(120, 60, 0));
            g.drawRect(px + 2, py + 2, tile - 5, tile - 5);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            String s = "=" + target;
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, px + (tile - fm.stringWidth(s)) / 2, py + (tile + fm.getAscent()) / 2 - 2);
        } else {
            // ประตูเปิด: เหลือแค่กรอบประตู เดินผ่านได้
            g.setColor(new Color(160, 100, 40));
            g.fillRect(px, py, 6, tile);
            g.fillRect(px + tile - 6, py, 6, tile);
        }
    }

    private void drawWithSprite(Graphics2D g, int px, int py, int tile) {
        if (!isLocked()) {
            // ประตูเปิดแล้ว: วาดจางๆ ให้รู้ว่าเคยมีประตูตรงนี้
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            Sprites.drawFit(g, Sprites.PUZZLE_DOOR, px, py, tile, tile);
            g.setComposite(old);
            return;
        }
        Sprites.drawFit(g, Sprites.PUZZLE_DOOR, px, py, tile, tile);

        // ป้ายเป้าหมายสีดำด้านล่างประตู เช่น "=16"
        String s = "=" + target;
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s) + 8, h = 15;
        int lx = px + (tile - w) / 2, ly = py + tile - h;
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(lx, ly, w, h, 8, 8);
        g.setColor(new Color(255, 215, 0));
        g.drawString(s, lx + 4, ly + h - 3);
    }
}

// ======================================================================
// ExitDoor.java
// ======================================================================
/** ประตูชัย — ไม่ต้องไขรหัส เดินชนแล้วผ่านด่านทันที */
class ExitDoor extends Door {

    public ExitDoor(int x, int y) {
        super(x, y);
    }

    @Override
    public void interact(Player player, GamePanel game) {
        game.completeLevel();
    }

    @Override
    public void draw(Graphics g, int tile) {
        int px = x * tile, py = y * tile;
        if (Sprites.EXIT_DOOR != null) {
            Sprites.drawFit(g, Sprites.EXIT_DOOR, px, py, tile, tile);
            return;
        }
        g.setColor(new Color(40, 180, 70));
        g.fillRect(px, py, tile, tile);
        g.setColor(new Color(255, 215, 0));                  // ขอบสีทอง
        g.drawRect(px + 2, py + 2, tile - 5, tile - 5);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("EXIT", px + (tile - fm.stringWidth("EXIT")) / 2, py + (tile + fm.getAscent()) / 2 - 2);
    }
}

// ======================================================================
// Obstacle.java
// ======================================================================
/**
 * คลาสแม่ของอุปสรรค ถ้าผู้เล่นโดนตอนที่อุปสรรค "อันตราย" จะถูกส่งกลับจุดเริ่มต้นของด่าน
 * อุปสรรคแต่ละแบบเปลี่ยนสถานะตามเวลาต่างกัน จึงให้คลาสลูก implement update() เอง
 */
abstract class Obstacle extends GameObject {

    public Obstacle(int x, int y) {
        super(x, y);
    }

    /** ถูกเรียกทุก tick (ทุก 0.2 วินาที) */
    public abstract void update(Level level);

    /** ตอนนี้ถ้าผู้เล่นอยู่ช่องเดียวกันจะโดนหรือไม่ */
    public abstract boolean isDangerous();

    /** ข้อความที่แสดงเมื่อผู้เล่นโดน */
    public abstract String getHitMessage();
}

// ======================================================================
// Spike.java
// ======================================================================
/** กับดักหนาม — โผล่ขึ้น-หดลงเป็นจังหวะ ต้องรอจังหวะที่หนามหดแล้วค่อยเดินผ่าน */
class Spike extends Obstacle {
    private static final int UP_TICKS = 5;      // หนามโผล่ 5 tick (1 วินาที)
    private static final int CYCLE_TICKS = 11;  // ครบรอบ 11 tick (หดลง 1.2 วินาที)

    private int tick = 0;

    public Spike(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(Level level) {
        tick = (tick + 1) % CYCLE_TICKS;
    }

    @Override
    public boolean isDangerous() {
        return tick < UP_TICKS;
    }

    @Override
    public String getHitMessage() {
        return "โดนหนาม! กลับไปจุดเริ่มต้น";
    }

    @Override
    public void draw(Graphics g, int tile) {
        int px = x * tile, py = y * tile;
        g.setColor(new Color(150, 130, 120));                // พื้นกับดัก
        g.fillRect(px + 2, py + 2, tile - 4, tile - 4);

        if (isDangerous()) {
            // วาดหนามสามเหลี่ยม 3 อัน
            g.setColor(new Color(220, 40, 40));
            int w = tile / 3;
            for (int i = 0; i < 3; i++) {
                int left = px + i * w;
                g.fillPolygon(new int[]{left + 2, left + w / 2, left + w - 2},
                              new int[]{py + tile - 6, py + 8, py + tile - 6}, 3);
            }
        } else {
            // หนามหด เหลือแค่รู
            g.setColor(new Color(90, 80, 70));
            for (int i = 0; i < 3; i++) {
                g.fillOval(px + 6 + i * (tile / 3), py + tile / 2 - 3, 6, 6);
            }
        }
    }
}

// ======================================================================
// Guard.java
// ======================================================================
/** ผู้พิทักษ์ — เดินลาดตระเวนซ้าย-ขวา ชนกำแพงหรือประตูแล้วเดินกลับ ห้ามเดินชน! */
class Guard extends Obstacle {
    private static final int MOVE_EVERY = 2;    // ขยับทุก 2 tick (0.4 วินาที)

    private int direction = 1;                  // 1 = ไปทางขวา, -1 = ไปทางซ้าย
    private int tick = 0;

    public Guard(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(Level level) {
        tick++;
        if (tick % MOVE_EVERY != 0) {
            return;
        }
        if (level.isBlockedForGuard(x + direction, y)) {
            direction = -direction;             // ข้างหน้าตัน → หันกลับ
        }
        if (!level.isBlockedForGuard(x + direction, y)) {
            x += direction;
        }
    }

    @Override
    public boolean isDangerous() {
        return true;                            // ผู้พิทักษ์อันตรายตลอดเวลา
    }

    @Override
    public String getHitMessage() {
        return "ผู้พิทักษ์จับได้! กลับไปจุดเริ่มต้น";
    }

    @Override
    public void draw(Graphics g, int tile) {
        int px = x * tile, py = y * tile;
        if (Sprites.GUARD != null) {
            Sprites.drawFit(g, Sprites.GUARD, px + 2, py + 2, tile - 4, tile - 4);
            return;
        }
        g.setColor(new Color(200, 30, 30));
        g.fillRoundRect(px + 5, py + 5, tile - 10, tile - 10, 12, 12);

        // ดวงตามองไปทางที่กำลังเดิน
        g.setColor(Color.WHITE);
        int eyeY = py + tile / 3;
        g.fillOval(px + tile / 3 - 4 + direction * 3, eyeY, 8, 8);
        g.fillOval(px + 2 * tile / 3 - 4 + direction * 3, eyeY, 8, 8);
    }
}

// ======================================================================
// PuzzleDialog.java
// ======================================================================
/** หน้าต่างให้ผู้เล่นเลือกไอเทมจาก Inventory มาเรียงเป็นสมการ */
class PuzzleDialog extends JDialog {
    private static final Font THAI_FONT = new Font("Tahoma", Font.PLAIN, 16);
    private static final Font BIG_FONT = new Font("Arial", Font.BOLD, 28);

    private final int target;
    private final Inventory inventory;
    private final List<Item> equation = new ArrayList<>();   // ไอเทมที่เลือกมาเรียงแล้ว
    private boolean solved = false;

    private final JLabel equationLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));

    public PuzzleDialog(Window owner, int target, Inventory inventory) {
        super(owner, "ประตูรหัสผ่าน", ModalityType.APPLICATION_MODAL);
        this.target = target;
        this.inventory = inventory;

        // ส่วนบน: เป้าหมาย
        JLabel targetLabel = new JLabel("จัดเรียงให้ได้ผลลัพธ์ = " + target, SwingConstants.CENTER);
        targetLabel.setFont(new Font("Tahoma", Font.BOLD, 20));
        if (Sprites.PUZZLE_DOOR != null) {                    // รูปประตูภารกิจอยู่ด้านบนข้อความ
            Image icon = Sprites.PUZZLE_DOOR.getScaledInstance(80, 90, Image.SCALE_SMOOTH);
            targetLabel.setIcon(new ImageIcon(icon));
            targetLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            targetLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        // ส่วนกลาง: สมการที่กำลังเรียง + ข้อความแจ้งเตือน + ปุ่มไอเทม
        equationLabel.setFont(BIG_FONT);
        messageLabel.setFont(THAI_FONT);
        messageLabel.setForeground(new Color(200, 0, 0));

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 8));
        center.add(equationLabel);
        center.add(messageLabel);
        center.add(itemPanel);

        // ส่วนล่าง: ปุ่มควบคุม
        JButton undoButton = makeButton("ลบตัวสุดท้าย");
        JButton clearButton = makeButton("ล้าง");
        JButton submitButton = makeButton("ตกลง");
        JButton cancelButton = makeButton("ยกเลิก");

        undoButton.addActionListener(e -> {
            if (!equation.isEmpty()) {
                equation.remove(equation.size() - 1);
            }
            refresh();
        });
        clearButton.addActionListener(e -> {
            equation.clear();
            refresh();
        });
        submitButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.add(undoButton);
        buttons.add(clearButton);
        buttons.add(submitButton);
        buttons.add(cancelButton);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        root.add(targetLabel, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        refresh();
        setMinimumSize(new Dimension(480, 0));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSolved() {
        return solved;
    }

    /** อัปเดตหน้าจอ: แสดงสมการปัจจุบัน และสร้างปุ่มไอเทมใหม่ */
    private void refresh() {
        // แสดงสมการ
        if (equation.isEmpty()) {
            equationLabel.setText("?");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Item item : equation) {
                sb.append(item.getSymbol()).append(' ');
            }
            equationLabel.setText(sb.toString().trim());
        }

        // สร้างปุ่มจากไอเทมใน Inventory — ไอเทมที่เลือกไปแล้วจะกดซ้ำไม่ได้
        itemPanel.removeAll();
        if (inventory.size() == 0) {
            JLabel empty = new JLabel("กระเป๋าว่างเปล่า — ไปเก็บตัวเลขและเครื่องหมายก่อน");
            empty.setFont(THAI_FONT);
            itemPanel.add(empty);
        }
        for (Item item : inventory.getItems()) {
            JButton b = new JButton(item.getSymbol());
            b.setFont(new Font("Arial", Font.BOLD, 20));
            b.setBackground(item.getColor());
            b.setFocusPainted(false);
            b.setEnabled(!equation.contains(item));
            b.addActionListener(e -> {
                equation.add(item);
                messageLabel.setText(" ");
                refresh();
            });
            itemPanel.add(b);
        }
        itemPanel.revalidate();
        itemPanel.repaint();
    }

    /** ตรวจคำตอบ: ถ้าถูก ลบไอเทมที่ใช้ออกจาก Inventory แล้วปิดหน้าต่าง */
    private void submit() {
        try {
            double result = ExpressionEvaluator.evaluate(equation);
            if (Math.abs(result - target) < 1e-9) {
                for (Item item : equation) {
                    inventory.remove(item);          // ไอเทมที่ใช้แล้วหายไป
                }
                solved = true;
                JOptionPane.showMessageDialog(this, "ถูกต้อง! ประตูเปิดแล้ว", "สำเร็จ",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                messageLabel.setText("ได้ " + ExpressionEvaluator.format(result)
                        + " ยังไม่ตรงกับเป้าหมาย " + target);
            }
        } catch (IllegalArgumentException | ArithmeticException ex) {
            messageLabel.setText(ex.getMessage());   // สมการผิดรูปแบบ หรือ หารด้วยศูนย์
        }
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(THAI_FONT);
        return b;
    }
}

// ======================================================================
// ExpressionEvaluator.java
// ======================================================================
/**
 * คำนวณสมการจากไอเทม ตามลำดับความสำคัญทางคณิตศาสตร์จริง (คูณ/หาร ก่อน บวก/ลบ)
 * เช่น 3 + 2 * 4 = 11 (ไม่ใช่ 20)
 */
class ExpressionEvaluator {

    /** ตรวจรูปแบบสมการ: ต้องเป็น ตัวเลข เครื่องหมาย ตัวเลข เครื่องหมาย ... ตัวเลข */
    public static void validate(List<Item> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("ยังไม่ได้เลือกไอเทมเลย");
        }
        for (int i = 0; i < tokens.size(); i++) {
            boolean shouldBeNumber = (i % 2 == 0);          // ตำแหน่งคู่ = ตัวเลข, ตำแหน่งคี่ = เครื่องหมาย
            Item token = tokens.get(i);
            if (shouldBeNumber && !(token instanceof NumberItem)) {
                throw new IllegalArgumentException("ต้องสลับ ตัวเลข กับ เครื่องหมาย และขึ้นต้นด้วยตัวเลข");
            }
            if (!shouldBeNumber && !(token instanceof OperatorItem)) {
                throw new IllegalArgumentException("ตัวเลขสองตัวติดกันไม่ได้ ต้องมีเครื่องหมายคั่น");
            }
        }
        if (tokens.size() % 2 == 0) {
            throw new IllegalArgumentException("สมการต้องจบด้วยตัวเลข");
        }
        if (tokens.size() < 3) {
            throw new IllegalArgumentException("ต้องใช้เครื่องหมายอย่างน้อย 1 ตัว");
        }
    }

    /**
     * คำนวณ 2 รอบ
     * รอบที่ 1: ไล่จากซ้ายไปขวา ทำ * และ / ทันที ส่วน + และ - เก็บไว้ก่อน
     * รอบที่ 2: นำตัวเลขที่เหลือมา + และ - จากซ้ายไปขวา
     */
    public static double evaluate(List<Item> tokens) {
        validate(tokens);

        List<Double> numbers = new ArrayList<>();
        List<Character> operators = new ArrayList<>();
        numbers.add(valueOf(tokens.get(0)));

        // รอบที่ 1: * และ /
        for (int i = 1; i < tokens.size(); i += 2) {
            char op = ((OperatorItem) tokens.get(i)).getOperator();
            double next = valueOf(tokens.get(i + 1));
            int last = numbers.size() - 1;

            if (op == '*') {
                numbers.set(last, numbers.get(last) * next);
            } else if (op == '/') {
                if (next == 0) {
                    throw new ArithmeticException("หารด้วยศูนย์ไม่ได้");
                }
                numbers.set(last, numbers.get(last) / next);
            } else {
                operators.add(op);          // + หรือ - เก็บไว้ทำรอบที่ 2
                numbers.add(next);
            }
        }

        // รอบที่ 2: + และ -
        double result = numbers.get(0);
        for (int i = 0; i < operators.size(); i++) {
            if (operators.get(i) == '+') {
                result += numbers.get(i + 1);
            } else {
                result -= numbers.get(i + 1);
            }
        }
        return result;
    }

    /** แสดงผลลัพธ์: ถ้าเป็นจำนวนเต็มไม่ต้องมีทศนิยม เช่น 11 แทน 11.0 */
    public static String format(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    private static double valueOf(Item item) {
        return ((NumberItem) item).getValue();
    }
}

// ======================================================================
// Sprites.java
// ======================================================================
/**
 * โหลดรูปภาพทั้งหมดของเกมจากโฟลเดอร์ images/ เพียงครั้งเดียวตอนเริ่มโปรแกรม
 * ถ้าหารูปไม่เจอ ค่าจะเป็น null และแต่ละคลาสจะวาดเป็นรูปทรงแทน (เกมยังเล่นได้)
 */
class Sprites {
    public static final BufferedImage PLAYER      = load("player.png");
    public static final BufferedImage PUZZLE_DOOR = load("puzzle_door.png");
    public static final BufferedImage EXIT_DOOR   = load("exit_door.png");
    public static final BufferedImage GUARD       = load("guard.png");
    public static final BufferedImage WALL        = load("wall.png");
    public static final BufferedImage FLOOR       = load("floor.png");

    private Sprites() { }

    private static BufferedImage load(String name) {
        try {
            URL url = Sprites.class.getResource("/images/" + name);   // กรณีรันจากไฟล์ .jar
            if (url != null) {
                return ImageIO.read(url);
            }
            File file = new File("images", name);                     // กรณีรันจาก VS Code
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (IOException e) {
            System.err.println("โหลดรูป " + name + " ไม่ได้: " + e.getMessage());
        }
        System.err.println("ไม่พบรูป images/" + name + " — จะวาดเป็นรูปทรงแทน");
        return null;
    }

    /**
     * วาดรูปให้พอดีกรอบ w x h โดยไม่บิดสัดส่วน (จัดกึ่งกลางแนวนอน ชิดขอบล่าง)
     */
    public static void drawFit(Graphics g, Image img, int x, int y, int w, int h) {
        int iw = img.getWidth(null), ih = img.getHeight(null);
        double scale = Math.min((double) w / iw, (double) h / ih);
        int dw = (int) (iw * scale), dh = (int) (ih * scale);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, x + (w - dw) / 2, y + (h - dh), dw, dh, null);
    }
}
