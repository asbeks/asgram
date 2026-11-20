package org.telegram.messenger.asgram;

import android.content.Context;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
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
    public static void refresh(Context c, int account, int guid, List<AsgramDirectory.Entry> sources,
                               Callback callback) {
        if (sources.isEmpty()) { callback.complete(new ArrayList<>(), true); return; }
        if (!UserConfig.getInstance(account).isClientActivated()) {
            callback.complete(AsgramFeedStore.cached(c), false);
            return;
        }
        Map<String,AsgramFeed.Post> merged=new LinkedHashMap<>();
        // Only known public publishers can enter the HTML/cache fallback.
        for (AsgramFeed.Post post : AsgramFeedStore.cached(c)) merged.put(post.id.toLowerCase(java.util.Locale.ROOT), post);
        int[] remaining={sources.size()},successes={0};AtomicBoolean finished=new AtomicBoolean();
        Runnable finish=()->{if(!finished.compareAndSet(false,true))return;
            List<AsgramFeed.Post> result=AsgramFeed.mergeAlbums(new ArrayList<>(merged.values()));
            if(result.size()>100)result=new ArrayList<>(result.subList(0,100));
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
            if(chat==null||!ChatObject.isChannelAndNotMegaGroup(chat)||!entry.username.equalsIgnoreCase(ChatObject.getPublicUsername(chat))){oneDone.run();return;}
            TLRPC.TL_messages_getHistory req=new TLRPC.TL_messages_getHistory();req.peer=controller.getInputPeer(-chat.id);
            // Include the tail of any album crossing the 20-message feed boundary.
            req.limit=29;
            int id=ConnectionsManager.getInstance(account).sendRequest(req,(response,error)->AndroidUtilities.runOnUIThread(()->{
                if(finished.get())return;
                if(response instanceof TLRPC.messages_Messages){TLRPC.messages_Messages res=(TLRPC.messages_Messages)response;
                    controller.putUsers(res.users,false);controller.putChats(res.chats,false);successes[0]++;
                    long boundaryAlbum = res.messages.size() >= 20 ? res.messages.get(19).grouped_id : 0;
                    for (int index = 0; index < res.messages.size(); index++) {
                        TLRPC.Message message = res.messages.get(index);
                        if (index >= 20 && (boundaryAlbum == 0 || message.grouped_id != boundaryAlbum)) break;
                        if(message instanceof TLRPC.TL_messageService||message.id<=0)continue;
                        String text=message.message==null?"":message.message;
                        TLRPC.Photo photo=message.media==null?null:message.media.photo;
                        TLRPC.Document document=message.media==null?null:message.media.document;
                        boolean video=MessageObject.isVideoDocument(document);
                        if(text.isEmpty()&&photo==null&&document==null)continue;
                        String postId=entry.username.toLowerCase(java.util.Locale.ROOT)+"/"+message.id;
                        AsgramFeed.Post previous = merged.get(postId);
                        AsgramFeed.Post post=new AsgramFeed.Post(postId,entry.category,entry.username,chat.title,text,
                                previous == null ? java.util.Collections.emptyList() : previous.imageUrls,Instant.ofEpochSecond(message.date).toString());
                        post.nativePhoto=photo;post.nativeAccount=account;post.nativeMessage=message;post.groupedId=message.grouped_id;post.hasVideo=video;merged.put(postId,post);
                    }
                }oneDone.run();
            }));ConnectionsManager.getInstance(account).bindRequestToGuid(id,guid);
        }));
    }
    private AsgramNativeFeed(){}
}
