package org.telegram.ui.asgram;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

/** Optional channel discovery. This class never requests a channel subscription. */
public final class AsgramCommunityInvitation {
    private static final String CHANNEL = "ABDII_SABAA";
    private static final boolean[] resolving = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private AsgramCommunityInvitation() {}
    public static void maybeShow(BaseFragment fragment, int account) {
        if (!UserConfig.getInstance(account).isClientActivated() || fragment.getParentActivity()==null || fragment.isPaused()) return;
        SharedPreferences prefs=fragment.getParentActivity().getSharedPreferences("asgram_community_"+account,Context.MODE_PRIVATE);
        if (prefs.getBoolean("invitation_seen",false) || resolving[account]) return;
        resolving[account]=true;
        MessagesController controller=MessagesController.getInstance(account);
        controller.getUserNameResolver().resolve(CHANNEL,peerId->{
            AndroidUtilities.runOnUIThread(()->{
                resolving[account]=false;
                if (fragment.getParentActivity()==null || fragment.isPaused() || peerId==null || peerId>=0) return;
                TLRPC.Chat chat=controller.getChat(-peerId);
                if (chat==null || chat.min || !ChatObject.isNotInChat(chat)) return;
                // The account holder chooses whether to open the channel and separately whether to join.
                AlertDialog dialog=new AlertDialog.Builder(fragment.getParentActivity())
                    .setTitle("Meet Abdii Sabaa")
                    .setMessage("News, learning and community from the people behind ASGRAM.\n\nExplore @ABDII_SABAA. Joining is your choice; all your Telegram chats work either way.")
                    .setPositiveButton("View channel",(d,which)->controller.openByUserName(CHANNEL,fragment,1))
                    .setNegativeButton("Not now",null).create();
                fragment.showDialog(dialog,d->prefs.edit().putBoolean("invitation_seen",true).apply());
            });
        });
    }
    public static void open(BaseFragment fragment,int account) {
        MessagesController.getInstance(account).openByUserName(CHANNEL,fragment,1);
    }
}
