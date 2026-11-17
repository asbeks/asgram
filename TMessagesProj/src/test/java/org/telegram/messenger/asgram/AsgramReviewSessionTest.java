package org.telegram.messenger.asgram;

import org.junit.Test;
import static org.junit.Assert.*;

public class AsgramReviewSessionTest {
    @Test public void messagesAndCreatedSpacesStayInOneDemo() {
        AsgramReviewSession first = new AsgramReviewSession();
        AsgramReviewSession next = new AsgramReviewSession();
        AsgramReviewSession.Chat chat = first.create("  Review group  ", AsgramReviewSession.Kind.GROUP);
        assertNotNull(chat);
        assertTrue(first.send(chat, "  Hello  "));
        assertEquals("Hello", chat.messages().get(1).body);
        assertTrue(chat.messages().get(1).outgoing);
        assertEquals(1, first.find("review", AsgramReviewSession.Kind.GROUP).size());
        assertTrue(next.find("review", null).isEmpty());
        assertFalse(next.send(chat, "Cross-session message"));
    }

    @Test public void botRepliesLocallyAndBlankInputsAreRejected() {
        AsgramReviewSession session = new AsgramReviewSession();
        AsgramReviewSession.Chat chat = session.find("practice", AsgramReviewSession.Kind.BOT).get(0);
        assertFalse(session.send(chat, " "));
        assertNull(session.create(" ", AsgramReviewSession.Kind.CHANNEL));
        assertTrue(session.send(chat, "Test"));
        assertEquals(3, chat.messages().size());
        assertFalse(chat.messages().get(2).outgoing);
    }

    @Test public void storySavesAreIndependentOfOtherDemoSessions() {
        AsgramReviewSession first = new AsgramReviewSession();
        AsgramReviewSession next = new AsgramReviewSession();
        AsgramFeed.Post story = new AsgramFeed.Post("demo/1", "News", "demo", "Sample", "Read later", "", "");
        assertTrue(first.toggleSave(story));
        assertTrue(first.isSaved(story));
        assertTrue(next.savedStories().isEmpty());
        assertFalse(first.toggleSave(story));
        assertFalse(first.isSaved(story));
    }
}
