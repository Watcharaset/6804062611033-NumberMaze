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

public class NumberMaze {
    public static void main(String[] args) {
        useThaiFont();

        JFrame frame = new JFrame("Number Maze");
        frame.add(new GamePanel());
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

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

class GamePanel extends JPanel {
    private static final int TILE = 44;
    private static final int HUD_HEIGHT = 112;
    private static final Font THAI_FONT = new Font("Tahoma", Font.BOLD, 15);

    private int levelIndex;
    private Level level;
    private Player player;
    private String message = "";
    private boolean gameWon = false;
    private final Timer timer;

    public GamePanel() {
        loadLevel(0);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameWon) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        gameWon = false;
                        loadLevel(0);
                    }
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP    -> tryMove(0, -1);
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN  -> tryMove(0, 1);
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> tryMove(-1, 0);
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> tryMove(1, 0);
                    case KeyEvent.VK_R -> {
                        loadLevel(levelIndex);
                        message = "เริ่มด่านใหม่แล้ว";
                    }
                }
            }
        });

        timer = new Timer(200, e -> tick());
        timer.start();
    }

    private void loadLevel(int index) {
        levelIndex = index;
        level = Levels.create(index);
        player = new Player(level.getStartX(), level.getStartY());
        message = "";

        setPreferredSize(new Dimension(level.getWidth() * TILE, level.getHeight() * TILE + HUD_HEIGHT));
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
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
            obstacle.update(level);
        }
        checkObstacles();
        repaint();
    }

    private void checkObstacles() {
        for (Obstacle obstacle : level.getObstacles()) {
            if (obstacle.isDangerous() && obstacle.getX() == player.getX() && obstacle.getY() == player.getY()) {
                message = obstacle.getHitMessage();
                player.moveTo(level.getStartX(), level.getStartY());
                return;
            }
        }
    }

    private void tryMove(int dx, int dy) {
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (level.isWall(newX, newY)) {
            return;
        }

        Door door = level.findDoorAt(newX, newY);
        if (door != null && door.isLocked()) {
            timer.stop();
            door.interact(player, this);
            timer.start();
            repaint();
            requestFocusInWindow();
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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

        for (Item item : level.getItems())             item.draw(g, TILE);
        for (Door door : level.getDoors())             door.draw(g, TILE);
        for (Obstacle obstacle : level.getObstacles()) obstacle.draw(g, TILE);
        player.draw(g, TILE);

        drawHud(g);

        if (gameWon) {
            drawWinScreen(g);
        }
    }

    private void drawHud(Graphics g) {
        int top = level.getHeight() * TILE;
        g.setColor(new Color(30, 30, 40));
        g.fillRect(0, top, getWidth(), HUD_HEIGHT);

        g.setFont(THAI_FONT);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(level.getName() + "  (" + (levelIndex + 1) + "/" + Levels.count() + ")", 10, top + 22);

        g.setColor(new Color(160, 160, 170));
        String hint = "R = เริ่มด่านใหม่";
        g.drawString(hint, getWidth() - fm.stringWidth(hint) - 10, top + 22);

        g.setColor(new Color(255, 210, 90));
        g.drawString(message, (getWidth() - fm.stringWidth(message)) / 2, top + 46);

        g.setColor(Color.WHITE);
        g.drawString("กระเป๋า:", 10, top + 88);
        int x = 80;
        for (Item item : player.getInventory().getItems()) {
            item.drawAt(g, x, top + 58, 46);
            x += 46;
        }
    }

    private void drawWinScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 190));
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

class Levels {
    private Levels() { }

    public static int count() {
        return 3;
    }

    public static Level create(int index) {
        return switch (index) {
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
                    continue;
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
                grid[row][col] = '.';
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

    public boolean isBlockedForGuard(int x, int y) {
        return isWall(x, y) || findDoorAt(x, y) != null;
    }

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

abstract class GameObject {
    protected int x, y;

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public abstract void draw(Graphics g, int tileSize);
}

class Player extends GameObject {
    private final Inventory inventory = new Inventory();

    public Player(int x, int y) {
        super(x, y);
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
            int h = (int) (tile * 1.3);
            Sprites.drawFit(g, Sprites.PLAYER, x * tile, (y + 1) * tile - h, tile, h);
        } else {
            g.setColor(Color.BLUE);
            g.fillOval(x * tile + 6, y * tile + 6, tile - 12, tile - 12);
        }
    }
}

class Inventory {
    private final List<Item> items = new ArrayList<>();

    public void add(Item item) {
        items.add(item);
    }

    public void remove(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }
}

abstract class Item extends GameObject {
    public Item(int x, int y) {
        super(x, y);
    }

    public abstract String getSymbol();

    public abstract Color getColor();

    @Override
    public void draw(Graphics g, int tile) {
        drawAt(g, x * tile, y * tile, tile);
    }

    public void drawAt(Graphics g, int px, int py, int size) {
        int pad = size / 8;
        g.setColor(getColor());
        g.fillRoundRect(px + pad, py + pad, size - pad * 2, size - pad * 2, 10, 10);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, size / 2));
        FontMetrics fm = g.getFontMetrics();
        String s = getSymbol();
        int textX = px + (size - fm.stringWidth(s)) / 2;
        int textY = py + (size + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(s, textX, textY);
    }
}

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
    public Color getColor() { return new Color(255, 230, 120); }
}

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
    public Color getColor() { return new Color(140, 200, 255); }
}

interface Interactable {
    void interact(Player player, GamePanel game);
}

abstract class Door extends GameObject implements Interactable {
    private boolean locked = true;

    public Door(int x, int y) {
        super(x, y);
    }

    public boolean isLocked() {
        return locked;
    }

    protected void unlock() {
        locked = false;
    }
}

class PuzzleDoor extends Door {
    private final int target;

    public PuzzleDoor(int x, int y, int target) {
        super(x, y);
        this.target = target;
    }

    public int getTarget() {
        return target;
    }

    @Override
    public void interact(Player player, GamePanel game) {
        Window owner = SwingUtilities.getWindowAncestor(game);
        PuzzleDialog dialog = new PuzzleDialog(owner, target, player.getInventory());
        dialog.setVisible(true);
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
            g.setColor(new Color(160, 100, 40));
            g.fillRect(px, py, 6, tile);
            g.fillRect(px + tile - 6, py, 6, tile);
        }
    }

    private void drawWithSprite(Graphics2D g, int px, int py, int tile) {
        if (!isLocked()) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            Sprites.drawFit(g, Sprites.PUZZLE_DOOR, px, py, tile, tile);
            g.setComposite(old);
            return;
        }
        Sprites.drawFit(g, Sprites.PUZZLE_DOOR, px, py, tile, tile);

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
        g.setColor(new Color(255, 215, 0));
        g.drawRect(px + 2, py + 2, tile - 5, tile - 5);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("EXIT", px + (tile - fm.stringWidth("EXIT")) / 2, py + (tile + fm.getAscent()) / 2 - 2);
    }
}

abstract class Obstacle extends GameObject {
    public Obstacle(int x, int y) {
        super(x, y);
    }

    public abstract void update(Level level);

    public abstract boolean isDangerous();

    public abstract String getHitMessage();
}

class Spike extends Obstacle {
    private static final int UP_TICKS = 5;
    private static final int CYCLE_TICKS = 11;

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
        g.setColor(new Color(150, 130, 120));
        g.fillRect(px + 2, py + 2, tile - 4, tile - 4);

        if (isDangerous()) {
            g.setColor(new Color(220, 40, 40));
            int w = tile / 3;
            for (int i = 0; i < 3; i++) {
                int left = px + i * w;
                g.fillPolygon(new int[]{left + 2, left + w / 2, left + w - 2},
                              new int[]{py + tile - 6, py + 8, py + tile - 6}, 3);
            }
        } else {
            g.setColor(new Color(90, 80, 70));
            for (int i = 0; i < 3; i++) {
                g.fillOval(px + 6 + i * (tile / 3), py + tile / 2 - 3, 6, 6);
            }
        }
    }
}

class Guard extends Obstacle {
    private static final int MOVE_EVERY = 2;

    private int direction = 1;
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
            direction = -direction;
        }
        if (!level.isBlockedForGuard(x + direction, y)) {
            x += direction;
        }
    }

    @Override
    public boolean isDangerous() {
        return true;
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

        g.setColor(Color.WHITE);
        int eyeY = py + tile / 3;
        g.fillOval(px + tile / 3 - 4 + direction * 3, eyeY, 8, 8);
        g.fillOval(px + 2 * tile / 3 - 4 + direction * 3, eyeY, 8, 8);
    }
}

class PuzzleDialog extends JDialog {
    private static final Font THAI_FONT = new Font("Tahoma", Font.PLAIN, 16);
    private static final Font BIG_FONT = new Font("Arial", Font.BOLD, 28);

    private final int target;
    private final Inventory inventory;
    private final List<Item> equation = new ArrayList<>();
    private boolean solved = false;

    private final JLabel equationLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));

    public PuzzleDialog(Window owner, int target, Inventory inventory) {
        super(owner, "ประตูรหัสผ่าน", ModalityType.APPLICATION_MODAL);
        this.target = target;
        this.inventory = inventory;

        JLabel targetLabel = new JLabel("จัดเรียงให้ได้ผลลัพธ์ = " + target, SwingConstants.CENTER);
        targetLabel.setFont(new Font("Tahoma", Font.BOLD, 20));
        if (Sprites.PUZZLE_DOOR != null) {
            Image icon = Sprites.PUZZLE_DOOR.getScaledInstance(80, 90, Image.SCALE_SMOOTH);
            targetLabel.setIcon(new ImageIcon(icon));
            targetLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            targetLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        equationLabel.setFont(BIG_FONT);
        messageLabel.setFont(THAI_FONT);
        messageLabel.setForeground(new Color(200, 0, 0));

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 8));
        center.add(equationLabel);
        center.add(messageLabel);
        center.add(itemPanel);

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

    private void refresh() {
        if (equation.isEmpty()) {
            equationLabel.setText("?");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Item item : equation) {
                sb.append(item.getSymbol()).append(' ');
            }
            equationLabel.setText(sb.toString().trim());
        }

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

    private void submit() {
        try {
            double result = ExpressionEvaluator.evaluate(equation);
            if (Math.abs(result - target) < 1e-9) {
                for (Item item : equation) {
                    inventory.remove(item);
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
            messageLabel.setText(ex.getMessage());
        }
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(THAI_FONT);
        return b;
    }
}

class ExpressionEvaluator {
    public static void validate(List<Item> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("ยังไม่ได้เลือกไอเทมเลย");
        }
        for (int i = 0; i < tokens.size(); i++) {
            boolean shouldBeNumber = (i % 2 == 0);
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

    public static double evaluate(List<Item> tokens) {
        validate(tokens);

        List<Double> numbers = new ArrayList<>();
        List<Character> operators = new ArrayList<>();
        numbers.add(valueOf(tokens.get(0)));

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
                operators.add(op);
                numbers.add(next);
            }
        }

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
            URL url = Sprites.class.getResource("/images/" + name);
            if (url != null) {
                return ImageIO.read(url);
            }
            File file = new File("images", name);
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (IOException e) {
            System.err.println("โหลดรูป " + name + " ไม่ได้: " + e.getMessage());
        }
        System.err.println("ไม่พบรูป images/" + name + " — จะวาดเป็นรูปทรงแทน");
        return null;
    }

    public static void drawFit(Graphics g, Image img, int x, int y, int w, int h) {
        int iw = img.getWidth(null), ih = img.getHeight(null);
        double scale = Math.min((double) w / iw, (double) h / ih);
        int dw = (int) (iw * scale), dh = (int) (ih * scale);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, x + (w - dw) / 2, y + (h - dh), dw, dh, null);
    }
}
