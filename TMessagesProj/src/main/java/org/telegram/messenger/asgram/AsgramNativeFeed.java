package org.telegram.messenger.asgram;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.asgram.AsgramUi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Read-only public publisher feed using the signed-in client's own Telegram connection. */
public final class AsgramNativeFeed {
    public interface Callback { void complete(List<AsgramFeed.Post> posts,boolean complete); }
    public static void refresh(Context context,int account,int guid,Callback callback) {
        if(!UserConfig.getInstance(account).isClientActivated()){callback.complete(AsgramFeedStore.cached(context),false);return;}
        AtomicBoolean delivered=new AtomicBoolean();
        Callback once=(posts,complete)->{if(delivered.compareAndSet(false,true))callback.complete(posts,complete);};
        AndroidUtilities.runOnUIThread(()->once.complete(AsgramFeedStore.cached(context),false),24_000);
        MessagesController controller=MessagesController.getInstance(account);
        controller.getUserNameResolver().resolve("ABDII_SABAA",peerId->AndroidUtilities.runOnUIThread(()->{
            if(peerId==null||peerId>=0){loadAll(context,account,guid,once);return;}
            TLRPC.Chat chat=controller.getChat(-peerId);
            if(chat==null){loadAll(context,account,guid,once);return;}
            TLRPC.TL_channels_getFullChannel req=new TLRPC.TL_channels_getFullChannel();req.channel=MessagesController.getInputChannel(chat);
            int id=ConnectionsManager.getInstance(account).sendRequest(req,(response,error)->AndroidUtilities.runOnUIThread(()->{
                if(response instanceof TLRPC.TL_messages_chatFull) {
                    String about=((TLRPC.TL_messages_chatFull)response).full_chat.about;
                    AsgramUi.prefs(context).edit().putString("directory_declared",new JSONArray(AsgramDirectory.declaredChannels(about)).toString()).apply();
                }
                loadAll(context,account,guid,once);
            }));ConnectionsManager.getInstance(account).bindRequestToGuid(id,guid);
        }));
    }
    public static List<AsgramDirectory.Entry> entries(Context c) {
        List<AsgramDirectory.Entry> list=new ArrayList<>();
        list.add(new AsgramDirectory.Entry("ABDII_SABAA","Abdii Sabaa","News"));
        list.add(new AsgramDirectory.Entry("abdii_sabaa_university","University","Education"));
        try {JSONArray array=new JSONArray(AsgramUi.prefs(c).getString("directory_verified","[]"));
            for(int i=0;i<Math.min(array.length(),8);i++){JSONObject j=array.getJSONObject(i);list.add(new AsgramDirectory.Entry(j.getString("username"),j.getString("title"),"News"));}
        }catch(Exception ignored){}return list;
    }
    private static void loadAll(Context c,int account,int guid,Callback callback) {
        List<AsgramDirectory.Entry> sources=new ArrayList<>();
        sources.add(new AsgramDirectory.Entry("ABDII_SABAA","Abdii Sabaa","News"));sources.add(new AsgramDirectory.Entry("abdii_sabaa_university","Abdii Sabaa University","Education"));
        try{JSONArray names=new JSONArray(AsgramUi.prefs(c).getString("directory_declared","[]"));for(int i=0;i<Math.min(8,names.length());i++)sources.add(new AsgramDirectory.Entry(names.getString(i),names.getString(i),"News"));}catch(Exception ignored){}
        Map<String,AsgramFeed.Post> merged=new LinkedHashMap<>();
        // Only the two original public publishers can enter the HTML/cache fallback.
        for(AsgramFeed.Post p:AsgramFeedStore.cached(c))merged.put(p.id,p);
        JSONArray verified=new JSONArray();int[] remaining={sources.size()},successes={0};AtomicBoolean finished=new AtomicBoolean();
        Runnable finish=()->{if(!finished.compareAndSet(false,true))return;
            List<AsgramFeed.Post> result=AsgramFeed.mergeAlbums(new ArrayList<>(merged.values()));
            if(result.size()>100)result=new ArrayList<>(result.subList(0,100));
            AsgramUi.prefs(c).edit().putString("directory_verified",verified.toString()).apply();
            if(successes[0]>0)AsgramUi.prefs(c).edit().putLong("last_sync",System.currentTimeMillis()).apply();
            if(successes[0]>0)AsgramFeedStore.cache(c,result);
            callback.complete(result,successes[0]==sources.size());
        };
        Runnable oneDone=()->{if(--remaining[0]==0)finish.run();};
        AndroidUtilities.runOnUIThread(finish,18_000);
        MessagesController controller=MessagesController.getInstance(account);
        for(AsgramDirectory.Entry entry:sources)controller.getUserNameResolver().resolve(entry.username,peerId->AndroidUtilities.runOnUIThread(()->{
            if(finished.get())return;
            TLRPC.Chat chat=peerId!=null&&peerId<0?controller.getChat(-peerId):null;
            // A bot/user or a private invite can never become an official publisher through discovery.
            if(chat==null||!ChatObject.isChannelAndNotMegaGroup(chat)||chat.username==null||!chat.username.equalsIgnoreCase(entry.username)){oneDone.run();return;}
            if(!entry.username.equalsIgnoreCase("ABDII_SABAA")&&!entry.username.equalsIgnoreCase("abdii_sabaa_university"))try{JSONObject j=new JSONObject();j.put("username",chat.username);j.put("title",chat.title);verified.put(j);}catch(Exception ignored){}
            TLRPC.TL_messages_getHistory req=new TLRPC.TL_messages_getHistory();req.peer=controller.getInputPeer(-chat.id);req.limit=20;
            int id=ConnectionsManager.getInstance(account).sendRequest(req,(response,error)->AndroidUtilities.runOnUIThread(()->{
                if(finished.get())return;
                if(response instanceof TLRPC.messages_Messages){TLRPC.messages_Messages res=(TLRPC.messages_Messages)response;
                    controller.putUsers(res.users,false);controller.putChats(res.chats,false);successes[0]++;
                    for(TLRPC.Message message:res.messages){
                        if(message instanceof TLRPC.TL_messageService||message.id<=0)continue;
                        String text=message.message==null?"":message.message;
                        TLRPC.Photo photo=message.media==null?null:message.media.photo;
                        boolean video=message.media!=null&&message.media.document!=null;
                        if(text.isEmpty()&&photo==null&&!video)continue;
                        String postId=chat.username+"/"+message.id;
                        AsgramFeed.Post post=new AsgramFeed.Post(postId,entry.category,chat.username,chat.title,text,"",Instant.ofEpochSecond(message.date).toString());
                        post.nativePhoto=photo;post.nativeAccount=account;post.nativeMessage=message;post.groupedId=message.grouped_id;post.hasVideo=video;merged.put(postId,post);
                    }
                }oneDone.run();
            }));ConnectionsManager.getInstance(account).bindRequestToGuid(id,guid);
        }));
    }
    private AsgramNativeFeed(){}
}
