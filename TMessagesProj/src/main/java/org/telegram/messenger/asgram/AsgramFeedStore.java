package org.telegram.messenger.asgram;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.asgram.AsgramUi;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class AsgramFeedStore {
    public static List<AsgramFeed.Post> cached(Context context) {
        String data=AsgramUi.prefs(context).getString("public_feed","");
        if (data.isEmpty()) {
            try (InputStream in=context.getAssets().open("asgram-feed.json");ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] bytes=new byte[8192];int n;
                while((n=in.read(bytes))!=-1 && out.size()<1_500_000)out.write(bytes,0,n);
                data=out.toString("UTF-8");
            } catch(Exception ignored) {}
        }
        return decode(data, AsgramChannelConfig.current(context).channels);
    }
    public static synchronized boolean isSaved(Context context,String id) {
        for(AsgramFeed.Post p:saved(context))if(p.id.equalsIgnoreCase(id))return true;
        return false;
    }
    public static synchronized boolean toggleSave(Context context,AsgramFeed.Post post) {
        List<AsgramFeed.Post> posts=saved(context);boolean removed=posts.removeIf(p->p.id.equalsIgnoreCase(post.id));
        if(!removed)posts.add(0,post);
        if(posts.size()>200)posts=new ArrayList<>(posts.subList(0,200));
        AsgramUi.prefs(context).edit().putString("saved_articles",encode(posts)).apply();
        return !removed;
    }
    public static List<AsgramFeed.Post> saved(Context context) {return decode(AsgramUi.prefs(context).getString("saved_articles","[]"), null);}
    public static boolean canLoadPhotos(Context context) {
        if(!AsgramUi.prefs(context).getBoolean("data_saver",true))return true;
        try {
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities cap=cm.getNetworkCapabilities(cm.getActiveNetwork());
            return cap!=null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch(Exception e) {return false;}
    }
    public static String encode(List<AsgramFeed.Post> posts) {
        JSONArray array=new JSONArray();
        for(AsgramFeed.Post p:posts) {
            try {
                JSONObject j=new JSONObject();j.put("id",p.id);j.put("category",p.category);j.put("channel",p.channel);
                j.put("grouped_id",p.groupedId);j.put("author",p.author);j.put("text",p.text);j.put("image",p.imageUrl);j.put("date",p.date);
                if (p.imageUrls.size() > 1) j.put("images", new JSONArray(p.imageUrls));
                array.put(j);
            } catch(Exception ignored) {}
        }
        return array.toString();
    }
    public static void cache(Context c,List<AsgramFeed.Post> posts){AsgramUi.prefs(c).edit().putString("public_feed",encode(posts)).apply();}
    public static List<AsgramFeed.Post> decode(String data) {
        return decode(data, AsgramDirectory.PUBLISHERS);
    }

    static List<AsgramFeed.Post> decode(String data, List<AsgramDirectory.Entry> publishers) {
        java.util.Map<String, String> categories = new java.util.HashMap<>();
        if (publishers != null) {
            for (AsgramDirectory.Entry entry : publishers) {
                categories.put(entry.username.toLowerCase(java.util.Locale.ROOT), entry.category);
            }
        }
        List<AsgramFeed.Post> posts=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(data);
            for(int i=0;i<Math.min(array.length(),200);i++) {
                JSONObject j=array.getJSONObject(i);String channel=j.optString("channel"),id=j.optString("id");
                if (!channel.matches("[A-Za-z][A-Za-z0-9_]{3,31}") ||
                        !id.matches("(?i)" + java.util.regex.Pattern.quote(channel) + "/[0-9]+")) continue;
                String category = publishers == null ? j.optString("category")
                        : categories.get(channel.toLowerCase(java.util.Locale.ROOT));
                if (category == null) continue;
                java.util.Set<String> images = new java.util.LinkedHashSet<>();
                String image = j.optString("image");
                if (AsgramFeed.allowedImage(image)) images.add(image);
                JSONArray album = j.optJSONArray("images");
                if (album != null) {
                    for (int photo = 0; photo < album.length(); photo++) {
                        String url = album.optString(photo);
                        if (AsgramFeed.allowedImage(url)) images.add(url);
                    }
                }
                AsgramFeed.Post post=new AsgramFeed.Post(id,category,channel,j.optString("author"),j.optString("text"),new ArrayList<>(images),j.optString("date"));post.groupedId=j.optLong("grouped_id",0);posts.add(post);
            }
        } catch(Exception ignored) {}
        return posts;
    }
    private AsgramFeedStore() {}
}
