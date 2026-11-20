package org.telegram.messenger.asgram;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Bundled publishers and supported editorial categories. */
public final class AsgramDirectory {
    public static final class Entry {
        public final String username,title,category;
        public Entry(String username, String title, String category) {
            this.username = username;
            this.title = title;
            this.category = category;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Entry)) return false;
            Entry entry = (Entry) other;
            return username.equals(entry.username) && title.equals(entry.title) && category.equals(entry.category);
        }
        @Override public int hashCode() { return Objects.hash(username, title, category); }
    }
    public static final String AMHARIC = "አማርኛ";
    static final List<String> CATEGORIES = Collections.unmodifiableList(Arrays.asList("News", AMHARIC, "Education", "Sports"));
    public static final List<Entry> PUBLISHERS = Collections.unmodifiableList(Arrays.asList(
            new Entry("abdii_sabaa", "Abdii Sabaa", "News"),
            new Entry("abdii_sabaa_university", "Abdii Sabaa University", "Education"),
            new Entry("abdiisabaaispoortii", "Abdii Sabaa Ispoortii", "Sports"),
            new Entry("abdiisabaa", "Abdii Sabaa · አማርኛ", AMHARIC)));

    public static Entry publisher(String username) {
        for (Entry entry : PUBLISHERS) {
            if (entry.username.equalsIgnoreCase(username)) return entry;
        }
        return null;
    }

    private AsgramDirectory() {}
}
