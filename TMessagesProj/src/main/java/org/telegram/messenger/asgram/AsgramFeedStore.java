package org.telegram.messenger.asgram;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.telegram.ui.asgram.AsgramUi;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AsgramFeedStore {
    public static final class Result {
        public final List<AsgramFeed.Post> posts; public final boolean complete;
        public Result(List<AsgramFeed.Post> posts,boolean complete) {this.posts=posts;this.complete=complete;}
    }
    public static List<AsgramFeed.Post> cached(Context context) {
        String data=AsgramUi.prefs(context).getString("public_feed","");
        if (data.isEmpty()) {
            try (InputStream in=context.getAssets().open("asgram-feed.json");ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] bytes=new byte[8192];int n;
                while((n=in.read(bytes))!=-1 && out.size()<1_500_000)out.write(bytes,0,n);
                data=out.toString("UTF-8");
            } catch(Exception ignored) {}
        }
        return decode(data);
    }
    public static Result refresh(Context context) {
        Map<String,AsgramFeed.Post> merged=new LinkedHashMap<>();
        for(AsgramFeed.Post p:cached(context))merged.put(p.id,p);
        int successes=0;
        for(String channel:new String[]{"ABDII_SABAA","abdii_sabaa_university"}) {
            try {
                String html=Jsoup.connect("https://t.me/s/"+channel).userAgent("Mozilla/5.0 ASGRAM/0.1").timeout(12_000).maxBodySize(1_500_000).get().outerHtml();
                List<AsgramFeed.Post> fresh=AsgramFeed.parse(html,channel);
                if(fresh.isEmpty())continue;
                successes++;
                for(AsgramFeed.Post p:fresh)merged.put(p.id,p);
            } catch(Exception ignored) {}
        }
        List<AsgramFeed.Post> posts=new ArrayList<>(merged.values());
        posts.sort((a,b)->Long.compare(b.timestamp(),a.timestamp()));
        if(posts.size()>60)posts=new ArrayList<>(posts.subList(0,60));
        if(successes>0)AsgramUi.prefs(context).edit().putString("public_feed",encode(posts)).putLong("last_sync",System.currentTimeMillis()).apply();
        return new Result(posts,successes==2);
    }
    public static synchronized boolean isSaved(Context context,String id) {
        for(AsgramFeed.Post p:saved(context))if(p.id.equals(id))return true;
        return false;
    }
    public static synchronized boolean toggleSave(Context context,AsgramFeed.Post post) {
        List<AsgramFeed.Post> posts=saved(context);boolean removed=posts.removeIf(p->p.id.equals(post.id));
        if(!removed)posts.add(0,post);
        if(posts.size()>200)posts=new ArrayList<>(posts.subList(0,200));
        AsgramUi.prefs(context).edit().putString("saved_articles",encode(posts)).apply();
        return !removed;
    }
    public static List<AsgramFeed.Post> saved(Context context) {return decode(AsgramUi.prefs(context).getString("saved_articles","[]"));}
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
                j.put("author",p.author);j.put("text",p.text);j.put("image",p.imageUrl);j.put("date",p.date);array.put(j);
            } catch(Exception ignored) {}
        }
        return array.toString();
    }
    public static List<AsgramFeed.Post> decode(String data) {
        List<AsgramFeed.Post> posts=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(data);
            for(int i=0;i<Math.min(array.length(),200);i++) {
                JSONObject j=array.getJSONObject(i);String channel=j.optString("channel"),id=j.optString("id");
                if(!AsgramFeed.allowedChannel(channel)||!id.matches("(?i)"+channel+"/[0-9]+"))continue;
                String image=j.optString("image");if(!AsgramFeed.allowedImage(image))image="";
                posts.add(new AsgramFeed.Post(id,j.optString("category"),channel,j.optString("author"),j.optString("text"),image,j.optString("date")));
            }
        } catch(Exception ignored) {}
        return posts;
    }
    private AsgramFeedStore() {}
}
