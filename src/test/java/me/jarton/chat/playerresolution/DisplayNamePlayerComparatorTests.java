package me.jarton.chat.playerresolution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.List;

class DisplayNamePlayerComparatorTest {

    private DisplayNamePlayerComparator comparator;

    @BeforeEach
    void setup() {
        comparator = new DisplayNamePlayerComparator();
    }

    @Test
    void testShortestEffectiveNameWins() {
        Player shortName = mock(Player.class);
        when(shortName.getName()).thenReturn("alice");
        when(shortName.getDisplayName()).thenReturn("alice");

        Player longName = mock(Player.class);
        when(longName.getName()).thenReturn("charlie");
        when(longName.getDisplayName()).thenReturn("charlie");

        assertTrue(comparator.compare(shortName, longName) < 0, "Shorter effective name should come first");
    }

    @Test
    void testNicknameShorterThanUsername() {
        Player usernameLongNickShort = mock(Player.class);
        when(usernameLongNickShort.getName()).thenReturn("charlie");
        when(usernameLongNickShort.getDisplayName()).thenReturn("bob"); // nickname shorter

        Player usernameShort = mock(Player.class);
        when(usernameShort.getName()).thenReturn("alice");
        when(usernameShort.getDisplayName()).thenReturn("alice");

        assertTrue(comparator.compare(usernameLongNickShort, usernameShort) < 0,
                "Shorter nickname wins over longer username");
    }

    @Test
    void testUsernamePreferredWhenTieLength() {
        Player username = mock(Player.class);
        when(username.getName()).thenReturn("john");
        when(username.getDisplayName()).thenReturn("john"); // same as username

        Player nickname = mock(Player.class);
        when(nickname.getName()).thenReturn("jack");
        when(nickname.getDisplayName()).thenReturn("john"); // same display length

        assertTrue(comparator.compare(username, nickname) < 0, "Username should win over nickname when lengths equal");
    }

    @Test
    void testColorCodesAreIgnored() {
        Player colorNick = mock(Player.class);
        when(colorNick.getName()).thenReturn("player");
        when(colorNick.getDisplayName()).thenReturn(ChatColor.RED + "red"); // length 3 after strip

        Player normal = mock(Player.class);
        when(normal.getName()).thenReturn("bob");
        when(normal.getDisplayName()).thenReturn("bob");

        assertTrue(comparator.compare(colorNick, normal) > 0,
                "Username should win tie even if nickname has color codes");
    }

    @Test
    void testSortingList() {
        Player a = mock(Player.class);
        when(a.getName()).thenReturn("alice");
        when(a.getDisplayName()).thenReturn("ali");

        Player b = mock(Player.class);
        when(b.getName()).thenReturn("bob");
        when(b.getDisplayName()).thenReturn("bobby");

        Player c = mock(Player.class);
        when(c.getName()).thenReturn("charlie");
        when(c.getDisplayName()).thenReturn("charlie");

        List<Player> players = Arrays.asList(c, b, a);
        players.sort(comparator);

        assertTrue(players.get(0) == a);
        assertTrue(players.get(1) == b);
        assertTrue(players.get(2) == c);
    }
}
