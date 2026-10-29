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

    @Test public void skipsSymbolOnlyLinesWhenChoosingARealHeadline() {
        AsgramFeed.Post post=new AsgramFeed.Post("ABDII_SABAA/45","News","ABDII_SABAA","Abdii Sabaa","🔴\n⚽️\nA meaningful headline\nFull report.","","");
        assertEquals("A meaningful headline",post.title());
    }
    @Test public void featuresARecentRealPhotoWithoutMutatingTheSource() {
        AsgramFeed.Post newest=new AsgramFeed.Post("ABDII_SABAA/46","News","ABDII_SABAA","Abdii Sabaa","Newest text","","2026-10-29T09:00:00Z");
        AsgramFeed.Post picture=new AsgramFeed.Post("ABDII_SABAA/45","News","ABDII_SABAA","Abdii Sabaa","Recent photo report","https://cdn4.telesco.pe/a.jpg","2026-10-29T08:00:00Z");
        List<AsgramFeed.Post> original=java.util.Arrays.asList(newest,picture);
        assertEquals(picture,AsgramFeed.featuredFirst(original).get(0));
        assertEquals(newest,original.get(0));
    }
    @Test public void neverFeaturesAnOldPhotoAheadOfNewReports() {
        AsgramFeed.Post newest=new AsgramFeed.Post("ABDII_SABAA/46","News","ABDII_SABAA","Abdii Sabaa","Newest text","","2026-10-29T09:00:00Z");
        AsgramFeed.Post old=new AsgramFeed.Post("ABDII_SABAA/45","News","ABDII_SABAA","Abdii Sabaa","Old photo","https://cdn4.telesco.pe/a.jpg","2026-10-20T08:00:00Z");
        assertEquals(newest,AsgramFeed.featuredFirst(java.util.Arrays.asList(newest,old)).get(0));
    }
    @Test public void discoversOnlyExplicitEditorialDirectoryEntries() {
        List<String> list=AsgramDirectory.declaredChannels("Contact @advertiser\nASGRAM: @OurSports @OurLearning\nOther @NotOfficial");
        assertEquals(java.util.Arrays.asList("oursports","ourlearning"),list);
        assertTrue(AsgramDirectory.declaredChannels("Join @advertiser for more").isEmpty());
    }
    @Test public void boundsDirectoryEntriesAndRejectsExecutableLinks() {
        assertTrue(AsgramDirectory.declaredChannels("ASGRAM: https://evil.example @a").isEmpty());
        StringBuilder text=new StringBuilder("ASGRAM: ");for(int i=0;i<20;i++)text.append("@channel_").append(i).append(" ");
        assertEquals(8,AsgramDirectory.declaredChannels(text.toString()).size());
    }
    @Test public void combinesAlbumsWithoutHidingTheCaptionOrLosingPhotos() {
        AsgramFeed.Post first=new AsgramFeed.Post("ABDII_SABAA/50","News","ABDII_SABAA","Abdii Sabaa","","","2026-10-29T09:00:00Z");first.groupedId=7;
        AsgramFeed.Post caption=new AsgramFeed.Post("ABDII_SABAA/51","News","ABDII_SABAA","Abdii Sabaa","Real album caption","","2026-10-29T09:00:01Z");caption.groupedId=7;
        org.telegram.tgnet.TLRPC.TL_message a=new org.telegram.tgnet.TLRPC.TL_message();a.id=50;first.nativeMessage=a;
        org.telegram.tgnet.TLRPC.TL_message b=new org.telegram.tgnet.TLRPC.TL_message();b.id=51;caption.nativeMessage=b;
        List<AsgramFeed.Post> result=AsgramFeed.mergeAlbums(java.util.Arrays.asList(first,caption));
        assertEquals(1,result.size());assertEquals("Real album caption",result.get(0).text);assertEquals(2,result.get(0).mediaCount());assertEquals(50,result.get(0).nativeAlbum.get(0).id);
    }
    @Test public void albumIdsDoNotMergeAcrossDifferentChannels() {
        AsgramFeed.Post first=new AsgramFeed.Post("ABDII_SABAA/50","News","ABDII_SABAA","Abdii Sabaa","One","","2026-10-29T09:00:00Z");first.groupedId=7;
        AsgramFeed.Post second=new AsgramFeed.Post("abdii_sabaa_university/51","Education","abdii_sabaa_university","University","Two","","2026-10-29T09:00:01Z");second.groupedId=7;
        List<AsgramFeed.Post> result=AsgramFeed.mergeAlbums(java.util.Arrays.asList(first,second));assertEquals(2,result.size());assertEquals(second.id,result.get(0).id);
    }
}
