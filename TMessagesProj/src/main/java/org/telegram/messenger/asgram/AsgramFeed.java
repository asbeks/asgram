package org.telegram.messenger.asgram;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Public, publisher-owned news previews. Never reads private Telegram chats. */
public final class AsgramFeed {
    public static final class Post {
        public final String id, category, channel, author, text, imageUrl, date, url;
        public final List<String> imageUrls;
        public TLRPC.Photo nativePhoto;
        public TLRPC.Message nativeMessage;
        public int nativeAccount;
        public long groupedId;
        public final List<TLRPC.Message> nativeAlbum = new ArrayList<>();
        public boolean hasVideo;
        public boolean hasPhoto(){return nativePhoto!=null||!imageUrl.isEmpty();}
        public Post(String id, String category, String channel, String author, String text, String imageUrl, String date) {
            this(id, category, channel, author, text, imageUrl.isEmpty()
                    ? java.util.Collections.emptyList() : java.util.Collections.singletonList(imageUrl), date);
        }
        public Post(String id, String category, String channel, String author, String text, List<String> imageUrls, String date) {
            this.id=id; this.category=category; this.channel=channel; this.author=author;
            this.imageUrls = java.util.Collections.unmodifiableList(new ArrayList<>(imageUrls));
            this.text=normalizeText(text); this.imageUrl=imageUrls.isEmpty() ? "" : imageUrls.get(0); this.date=date; this.url="https://t.me/"+id;
        }
        public List<TLRPC.Message> nativeMedia() {
            List<TLRPC.Message> source = nativeAlbum.isEmpty() && nativeMessage != null
                    ? java.util.Collections.singletonList(nativeMessage) : nativeAlbum;
            List<TLRPC.Message> media = new ArrayList<>();
            for (TLRPC.Message message : source) {
                if (message.media != null && (message.media.photo != null || MessageObject.isVideoDocument(message.media.document))) {
                    media.add(message);
                }
            }
            return media;
        }
        public long timestamp() {
            try { return OffsetDateTime.parse(date).toInstant().toEpochMilli(); } catch (Exception e) { return 0L; }
        }
        public String body() {
            String heading=title();
            int at=text.indexOf(heading);
            return at<0 ? text : normalizeText(text.substring(0,at)+text.substring(at+heading.length()));
        }
        public String title() {
            String[] lines=text.split("\\n");
            for (String line:lines) {
                line=line.trim();
                if (!line.isEmpty() && line.codePoints().anyMatch(Character::isLetterOrDigit) && !line.startsWith("#") && !line.startsWith("SABA GUDDAA") && !line.startsWith("MIIDIYAA @")) {
                    return line;
                }
            }
            return text.isEmpty() ? (hasVideo?"Video update":"Photo update") : text.substring(0,Math.min(160,text.length()));
        }
    }
    private static String normalizeText(String text) {
        if(text==null)return "";
        return text.replace("\r\n","\n").replace('\r','\n')
            .replaceAll("[\\t ]+\\n","\n").replaceAll("\\n[\\t ]+","\n")
            .replaceAll("\\n{3,}","\n\n").trim();
    }
    public static boolean allowedChannel(String channel) {
        return "ABDII_SABAA".equalsIgnoreCase(channel) || "abdii_sabaa_university".equalsIgnoreCase(channel);
    }
    public static boolean allowedImage(String value) {
        try {
            URI uri=new URI(value);
            String host=uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host!=null &&
                (host.toLowerCase(Locale.ROOT).endsWith(".telesco.pe") || host.equalsIgnoreCase("telesco.pe")) &&
                uri.getRawUserInfo()==null;
        } catch (Exception e) { return false; }
    }
    public static List<Post> parse(String html, String channel) {
        if (!allowedChannel(channel)) throw new IllegalArgumentException("Unrecognized publisher.");
        Document doc=Jsoup.parse(html);
        List<Post> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for (Element message:doc.select(".tgme_widget_message[data-post]")) {
            String id=message.attr("data-post");
            if (!id.matches("(?i)"+channel+"/[0-9]+") || !seen.add(id)) continue;
            Element body=message.selectFirst(".tgme_widget_message_text");
            Element time=message.selectFirst("time[datetime]");
            String text="";
            if (body!=null) {
                body.select("br").append("\\n");
                text=body.wholeText().replace("\\n","\n").trim();
            }
            Set<String> images = new LinkedHashSet<>();
            for (Element photo : message.select(".tgme_widget_message_photo_wrap")) {
                java.util.regex.Matcher m=java.util.regex.Pattern.compile("url\\(['\"]?([^'\")]+)").matcher(photo.attr("style"));
                if (m.find() && allowedImage(m.group(1))) images.add(m.group(1));
            }
            if (text.isEmpty() && images.isEmpty()) continue;
            String category=channel.equalsIgnoreCase("ABDII_SABAA") ? "News" : "Education";
            result.add(new Post(id,category,channel,category.equals("News")?"Abdii Sabaa":"Abdii Sabaa University",text,new ArrayList<>(images),time==null?"":time.attr("datetime")));
        }
        result.sort((a,b)->Long.compare(b.timestamp(),a.timestamp()));
        return result;
    }
    /** One editorial post per Telegram album; caption and every media item are preserved. */
    public static List<Post> mergeAlbums(List<Post> source) {
        java.util.Map<String,List<Post>> groups=new java.util.LinkedHashMap<>();
        for(Post p:source){String key=p.groupedId==0?p.id:p.channel.toLowerCase(Locale.ROOT)+"/album/"+p.groupedId;groups.computeIfAbsent(key,k->new ArrayList<>()).add(p);}
        List<Post> result=new ArrayList<>();
        for(List<Post> group:groups.values()) {
            Post caption=group.get(0);long newest=caption.timestamp();String newestDate=caption.date;
            java.util.Map<Integer,TLRPC.Message> media=new java.util.TreeMap<>();
            Set<String> images = new LinkedHashSet<>();
            for(Post p:group){
                if((caption.text.isEmpty()&&!p.text.isEmpty())||(caption.nativeMessage==null&&p.nativeMessage!=null&&!p.text.isEmpty()))caption=p;
                if(p.timestamp()>newest){newest=p.timestamp();newestDate=p.date;}
                if(p.nativeMessage!=null)media.put(p.nativeMessage.id,p.nativeMessage);
                for(TLRPC.Message m:p.nativeAlbum)media.put(m.id,m);
                images.addAll(p.imageUrls);
            }
            Post album=new Post(caption.id,caption.category,caption.channel,caption.author,caption.text,new ArrayList<>(images),newestDate);
            album.nativeMessage=caption.nativeMessage;album.nativeAccount=caption.nativeAccount;album.nativePhoto=caption.nativePhoto;album.groupedId=caption.groupedId;album.hasVideo=caption.hasVideo;
            for(TLRPC.Message m:media.values()){album.nativeAlbum.add(m);if(m.media!=null){if(album.nativePhoto==null&&m.media.photo!=null)album.nativePhoto=m.media.photo;if(MessageObject.isVideoDocument(m.media.document))album.hasVideo=true;}}
            result.add(album);
        }
        result.sort((a,b)->Long.compare(b.timestamp(),a.timestamp()));return result;
    }
    /** Choose a recent real photo lead, preserving every other story's chronological order. */
    public static List<Post> featuredFirst(List<Post> source) {
        List<Post> result=new ArrayList<>(source);
        if(result.isEmpty())return result;
        long newest=result.get(0).timestamp();
        for(int i=0;i<Math.min(result.size(),8);i++) {
            Post post=result.get(i);
            if(post.hasPhoto()&&!post.text.isEmpty()&&newest-post.timestamp()<=48L*60*60*1000) {
                if(i>0){result.remove(i);result.add(0,post);}break;
            }
        }
        return result;
    }
    private AsgramFeed() {}
}
