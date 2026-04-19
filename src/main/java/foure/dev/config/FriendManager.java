package foure.dev.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * FriendManager — stores friends and enemies, adapted from SocialsManager.
 */
public class FriendManager {
    private static final FriendManager INSTANCE = new FriendManager();
    public static FriendManager getInstance() { return INSTANCE; }

    private final List<String> friends = new ArrayList<>();
    private final List<String> enemies = new ArrayList<>();

    public void addFriend(String name)    { if (!friends.contains(name)) friends.add(name); }
    public void removeFriend(String name) { friends.remove(name); }
    public boolean isFriend(String name)  { return friends.contains(name); }
    public List<String> getFriends()      { return friends; }

    public void addEnemy(String name)    { if (!enemies.contains(name)) enemies.add(name); }
    public void removeEnemy(String name) { enemies.remove(name); }
    public boolean isEnemy(String name)  { return enemies.contains(name); }
    public List<String> getEnemies()     { return enemies; }

    /** Returns colored name for rendering (cyan=friend, red=enemy, default otherwise). */
    public Color getColor(String name, Color defaultColor) {
        if (isFriend(name)) return Color.CYAN;
        if (isEnemy(name))  return Color.RED;
        return defaultColor;
    }

    public Color getColor(String name) { return getColor(name, Color.WHITE); }

    /** 1=friend, -1=enemy, 0=neutral */
    public int getType(String name) {
        if (isFriend(name)) return 1;
        if (isEnemy(name))  return -1;
        return 0;
    }
}

