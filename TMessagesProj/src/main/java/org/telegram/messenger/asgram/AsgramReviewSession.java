package org.telegram.messenger.asgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Review data exists only in this instance, independently of Telegram accounts and storage. */
public final class AsgramReviewSession {
    public enum Kind {
        PRIVATE("Private"), GROUP("Group"), CHANNEL("Channel"), BOT("Bot");
        public final String label;
        Kind(String label) { this.label = label; }
    }

    public static final class Message {
        public final String body;
        public final boolean outgoing;
        private Message(String body, boolean outgoing) {
            this.body = body;
            this.outgoing = outgoing;
        }
    }

    public static final class Chat {
        public final String title;
        public final Kind kind;
        private final List<Message> messages = new ArrayList<>();

        private Chat(String title, Kind kind, String greeting) {
            this.title = title;
            this.kind = kind;
            messages.add(new Message(greeting, false));
        }

        public List<Message> messages() {
            return Collections.unmodifiableList(messages);
        }
    }

    private final List<Chat> chats = new ArrayList<>();

    public AsgramReviewSession() {
        chats.add(new Chat("Amina", Kind.PRIVATE, "Hi! Send a message to try this sample conversation."));
        chats.add(new Chat("Weekend Plans", Kind.GROUP, "Let’s meet at the library on Saturday."));
        chats.add(new Chat("Learning Circle", Kind.CHANNEL, "This week’s reading list is ready."));
        chats.add(new Chat("Practice Bot", Kind.BOT, "Send a message to try a sample bot reply."));
    }

    public List<Chat> find(String query, Kind kind) {
        String term = query.trim().toLowerCase(Locale.ROOT);
        List<Chat> matching = new ArrayList<>();
        for (Chat chat : chats) {
            if ((kind == null || chat.kind == kind) && chat.title.toLowerCase(Locale.ROOT).contains(term)) {
                matching.add(chat);
            }
        }
        return matching;
    }

    public Chat create(String title, Kind kind) {
        String name = title.trim();
        if (name.isEmpty() || name.length() > 64 || chats.size() >= 24) return null;
        Chat chat = new Chat(name, kind, "Sample space created.");
        chats.add(0, chat);
        return chat;
    }

    public boolean send(Chat chat, String body) {
        String text = body.trim();
        if (!chats.contains(chat) || text.isEmpty() || text.length() > 1024 || chat.messages.size() >= 80) return false;
        chat.messages.add(new Message(text, true));
        if (chat.kind == Kind.BOT) chat.messages.add(new Message("Sample reply received.", false));
        return true;
    }

    private final List<AsgramFeed.Post> savedStories = new ArrayList<>();

    public List<AsgramFeed.Post> savedStories() { return Collections.unmodifiableList(savedStories); }

    public boolean isSaved(AsgramFeed.Post story) {
        for (AsgramFeed.Post saved : savedStories) if (saved.id.equals(story.id)) return true;
        return false;
    }

    public boolean toggleSave(AsgramFeed.Post story) {
        if (savedStories.removeIf(saved -> saved.id.equals(story.id))) return false;
        savedStories.add(0, story);
        return true;
    }
}
