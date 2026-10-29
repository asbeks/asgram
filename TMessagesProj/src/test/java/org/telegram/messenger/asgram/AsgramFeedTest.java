package org.telegram.messenger.asgram;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class AsgramFeedTest {
    @Test public void parsesOnlyTheRequestedPublisherAndDeduplicatesMessages() {
        String block="<div class='tgme_widget_message' data-post='ABDII_SABAA/42'><div class='tgme_widget_message_text'>A real update<br>Full story &amp; details</div><time datetime='2026-10-28T09:00:00Z'></time></div>";
        String foreign="<div class='tgme_widget_message' data-post='somebody_else/43'><div class='tgme_widget_message_text'>Foreign post</div></div>";
        List<AsgramFeed.Post> posts=AsgramFeed.parse(block+block+foreign,"ABDII_SABAA");
        assertEquals(1,posts.size());
        assertEquals("https://t.me/ABDII_SABAA/42",posts.get(0).url);
        assertTrue(posts.get(0).text.contains("Full story & details"));
        assertTrue(posts.get(0).timestamp()>0);
    }
    @Test public void rejectsUnapprovedPublishers() {
        try { AsgramFeed.parse("","attacker.example");fail("Publisher must be approved"); }
        catch(IllegalArgumentException expected) {}
    }
    @Test public void acceptsOnlyHttpsTelegramMediaHostsWithoutCredentials() {
        assertTrue(AsgramFeed.allowedImage("https://cdn4.telesco.pe/file/preview.jpg"));
        assertFalse(AsgramFeed.allowedImage("http://cdn4.telesco.pe/file/preview.jpg"));
        assertFalse(AsgramFeed.allowedImage("https://cdn4.telesco.pe.evil.example/a.jpg"));
        assertFalse(AsgramFeed.allowedImage("https://user:password@cdn4.telesco.pe/a.jpg"));
        assertFalse(AsgramFeed.allowedImage("file:///data/private"));
    }
    @Test public void preservesEditorialTextInsteadOfExecutingMarkup() {
        List<AsgramFeed.Post> posts=AsgramFeed.parse("<div class='tgme_widget_message' data-post='Abdii_Sabaa_University/12'><div class='tgme_widget_message_text'>Student update <a href='javascript:alert(1)'>read more</a></div><time datetime='2026-10-28T07:00:00Z'></time></div>","abdii_sabaa_university");
        assertEquals(1,posts.size());assertEquals("Education",posts.get(0).category);
        assertEquals("Student update read more",posts.get(0).text);
        assertEquals("",posts.get(0).imageUrl);
    }
    @Test public void acceptsOffsetTimestampsAndSortsNewestFirst() {
        String old="<div class='tgme_widget_message' data-post='ABDII_SABAA/42'><div class='tgme_widget_message_text'>Older</div><time datetime='2026-10-28T12:00:16+00:00'></time></div>";
        String fresh="<div class='tgme_widget_message' data-post='ABDII_SABAA/43'><div class='tgme_widget_message_text'>Newer</div><time datetime='2026-10-28T16:00:16+03:00'></time></div>";
        List<AsgramFeed.Post> posts=AsgramFeed.parse(old+fresh,"ABDII_SABAA");
        assertEquals("ABDII_SABAA/43",posts.get(0).id);
        assertTrue(posts.get(0).timestamp()>posts.get(1).timestamp());
    }
    @Test public void keepsFullHeadingAndSeparatesReadableParagraphs() {
        String heading=String.join("",java.util.Collections.nCopies(24,"Headline ")).trim();
        AsgramFeed.Post post=new AsgramFeed.Post("ABDII_SABAA/44","News","ABDII_SABAA","Abdii Sabaa",heading+"\n\n\n\nBody words.\n\n\nLater paragraph.","","");
        assertEquals(heading,post.title());
        assertEquals("Body words.\n\nLater paragraph.",post.body());
        assertFalse(post.text.contains("\n\n\n"));
    }

}
