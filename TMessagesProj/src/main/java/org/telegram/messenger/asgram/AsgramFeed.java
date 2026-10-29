package org.telegram.messenger.asgram;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Public, publisher-owned news previews. Never reads private Telegram chats. */
public final class AsgramFeed {
    public static final class Post {
        public final String id, category, channel, author, text, imageUrl, date, url;
        public Post(String id, String category, String channel, String author, String text, String imageUrl, String date) {
            this.id=id; this.category=category; this.channel=channel; this.author=author;
            this.text=normalizeText(text); this.imageUrl=imageUrl; this.date=date; this.url="https://t.me/"+id;
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
                if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("SABA GUDDAA") && !line.startsWith("MIIDIYAA @")) {
                    return line;
                }
            }
            return text.isEmpty() ? "Photo update" : text.substring(0,Math.min(160,text.length()));
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
            String image="";
            Element photo=message.selectFirst(".tgme_widget_message_photo_wrap");
            if (photo!=null) {
                java.util.regex.Matcher m=java.util.regex.Pattern.compile("url\\(['\"]?([^'\")]+)").matcher(photo.attr("style"));
                if (m.find() && allowedImage(m.group(1))) image=m.group(1);
            }
            if (text.isEmpty() && image.isEmpty()) continue;
            String category=channel.equalsIgnoreCase("ABDII_SABAA") ? "News" : "Education";
            result.add(new Post(id,category,channel,category.equals("News")?"Abdii Sabaa":"Abdii Sabaa University",text,image,time==null?"":time.attr("datetime")));
        }
        result.sort((a,b)->Long.compare(b.timestamp(),a.timestamp()));
        return result;
    }
    private AsgramFeed() {}
}
