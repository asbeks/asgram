package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.asgram.AsgramUi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local views over the account's real native dialogs; never creates server-side folders. */
public final class AsgramSpacesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private String type="All",query="";
    private AsgramUi.Palette palette;
    private RecyclerView recycler;
    private SpaceAdapter adapter;
    private final List<TLRPC.Dialog> visible=new ArrayList<>();
    private boolean nested;
    public AsgramSpacesActivity(){this(false);}
    public AsgramSpacesActivity(boolean nested){this.nested=nested;}
    @Override public boolean onFragmentCreate(){getNotificationCenter().addObserver(this,NotificationCenter.dialogsNeedReload);return super.onFragmentCreate();}
    @Override public void onFragmentDestroy(){getNotificationCenter().removeObserver(this,NotificationCenter.dialogsNeedReload);super.onFragmentDestroy();}
    @Override public View createView(Context c){
        palette=new AsgramUi.Palette(c);actionBar.setTitle("Spaces");actionBar.setSubtitle("Every part of your Telegram");
        actionBar.setBackgroundColor(palette.background);actionBar.setTitleColor(palette.text);actionBar.setSubtitleColor(palette.secondary);actionBar.setItemsColor(palette.text,false);
        actionBar.setAllowOverlayTitle(false);actionBar.setCastShadows(false);
        if(nested)actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();else if(id==2)createMenu();}});
        actionBar.createMenu().addItem(1,R.drawable.outline_header_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener(){@Override public void onTextChanged(EditText f){query=f.getText().toString();reload();}}).setSearchFieldHint("Search your spaces");
        actionBar.createMenu().addItem(2,R.drawable.msg_add).setContentDescription("Create a chat, group or channel");
        FrameLayout root=new FrameLayout(c);root.setBackgroundColor(palette.background);root.setPadding(0,AndroidUtilities.statusBarHeight+ActionBar.getCurrentActionBarHeight(),0,0);fragmentView=root;
        recycler=new RecyclerView(c);recycler.setLayoutManager(new LinearLayoutManager(c));recycler.setItemAnimator(null);recycler.setClipToPadding(false);recycler.setPadding(0,0,0,AndroidUtilities.navigationBarHeight+AsgramUi.dp(104));adapter=new SpaceAdapter();recycler.setAdapter(adapter);root.addView(recycler,LayoutHelper.createFrame(-1,-1));reload();if(getMessagesController().getDialogs(0).isEmpty()&&!getMessagesController().isLoadingDialogs(0))getMessagesController().loadDialogs(0,0,100,true);return root;
    }
    private String title(TLRPC.Dialog d){
        if(DialogObject.isEncryptedDialog(d.id)){TLRPC.EncryptedChat chat=getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(d.id));return chat==null?"Secret chat":UserObject.getUserName(getMessagesController().getUser(chat.user_id));}
        if(d.id>0)return UserObject.getUserName(getMessagesController().getUser(d.id));TLRPC.Chat chat=getMessagesController().getChat(-d.id);return chat==null?"Channel or group":chat.title;
    }
    private String kind(TLRPC.Dialog d){
        if(DialogObject.isEncryptedDialog(d.id))return "Private";
        if(d.id>0){TLRPC.User user=getMessagesController().getUser(d.id);return user!=null&&user.bot?"Bots":"Private";}
        return ChatObject.isChannelAndNotMegaGroup(getMessagesController().getChat(-d.id))?"Channels":"Groups";
    }
    private void reload(){if(adapter==null)return;visible.clear();
        List<TLRPC.Dialog> all=new ArrayList<>(getMessagesController().getDialogs(0));all.addAll(getMessagesController().getDialogs(1));
        java.util.HashSet<Long> seen=new java.util.HashSet<>();String term=query.toLowerCase(Locale.ROOT).trim();
        for(TLRPC.Dialog d:all){if(DialogObject.isFolderDialogId(d.id)||!seen.add(d.id))continue;if(!type.equals("All")&&!kind(d).equals(type))continue;if(!term.isEmpty()&&!title(d).toLowerCase(Locale.ROOT).contains(term))continue;visible.add(d);}adapter.notifyDataSetChanged();
    }
    private void show(BaseFragment target){target.setCurrentAccount(currentAccount);presentFragment(target);}
    private void createMenu(){new AlertDialog.Builder(getParentActivity()).setTitle("Create something new").setItems(new CharSequence[]{"Private chat","Group","Channel","Secret chat","Bot with BotFather","Open a channel or bot link"},(dialog,index)->{
        Bundle args=new Bundle();
        if(index==0){args.putBoolean("onlyUsers",true);args.putBoolean("destroyAfterSelect",true);show(new ContactsActivity(args));}
        else if(index==1)show(new GroupCreateActivity(args));
        else if(index==2){args.putInt("step",0);show(new ChannelCreateActivity(args));}
        else if(index==3){args.putBoolean("createSecretChat",true);args.putBoolean("onlyUsers",true);args.putBoolean("destroyAfterSelect",true);show(new ContactsActivity(args));}
        else if(index==4)getMessagesController().openByUserName("BotFather",this,1);
        else openLink();
    }).show();}
    private void openLink(){Context c=getContext();EditText field=new EditText(c);field.setSingleLine(true);field.setHint("@channel or https://t.me/…");field.setTextColor(palette.text);field.setHintTextColor(palette.secondary);field.setPadding(AsgramUi.dp(22),AsgramUi.dp(16),AsgramUi.dp(22),AsgramUi.dp(16));
        new AlertDialog.Builder(getParentActivity()).setTitle("Open on Telegram").setView(field).setPositiveButton("Open",(d,w)->{String value=field.getText().toString().trim();if(value.matches("@?[A-Za-z][A-Za-z0-9_]{4,31}"))getMessagesController().openByUserName(value.replace("@",""),this,1);else if(value.matches("https://t\\.me/[A-Za-z0-9_+/?=-]+"))org.telegram.messenger.browser.Browser.openUrl(getParentActivity(),value);else new AlertDialog.Builder(getParentActivity()).setTitle("Check the link").setMessage("Enter a Telegram username or a https://t.me/ link.").setPositiveButton("OK",null).show();}).setNegativeButton("Cancel",null).show();
    }
    private void open(TLRPC.Dialog dialog){Bundle args=new Bundle();if(DialogObject.isEncryptedDialog(dialog.id))args.putInt("enc_id",DialogObject.getEncryptedChatId(dialog.id));else if(dialog.id>0)args.putLong("user_id",dialog.id);else args.putLong("chat_id",-dialog.id);show(new ChatActivity(args));}
    private View header(Context c){LinearLayout box=AsgramUi.column(c);box.setPadding(AsgramUi.dp(20),AsgramUi.dp(18),AsgramUi.dp(20),AsgramUi.dp(12));
        box.addView(AsgramUi.text(c,"Find your space.",29,palette.text,true));AsgramUi.gap(box,10);box.addView(AsgramUi.text(c,"Private chats, groups, channels and bots. Your existing spaces stay right here.",14,palette.secondary,false));AsgramUi.gap(box,16);
        LinearLayout actions=AsgramUi.row(c);actions.addView(AsgramUi.button(c,"＋ Create",palette,true,this::createMenu),new LinearLayout.LayoutParams(0,-2,1));TextView contacts=AsgramUi.button(c,"Contacts",palette,false,()->{Bundle args=new Bundle();args.putBoolean("needPhonebook",true);show(new ContactsActivity(args));});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=AsgramUi.dp(10);actions.addView(contacts,lp);box.addView(actions);AsgramUi.gap(box,16);
        HorizontalScrollView scroll=new HorizontalScrollView(c);scroll.setHorizontalScrollBarEnabled(false);LinearLayout filters=AsgramUi.row(c);
        for(String value:new String[]{"All","Private","Groups","Channels","Bots"}){boolean selected=type.equals(value);TextView tab=AsgramUi.text(c,value,14,selected?palette.inverse:palette.secondary,selected);tab.setGravity(Gravity.CENTER);tab.setPadding(AsgramUi.dp(16),0,AsgramUi.dp(16),0);tab.setMinHeight(AsgramUi.dp(48));if(selected)tab.setBackground(AsgramUi.shape(palette.primary,24));tab.setSelected(selected);tab.setOnClickListener(v->{type=value;reload();recycler.scrollToPosition(0);});filters.addView(tab,AsgramUi.lp(-2,48));}scroll.addView(filters);box.addView(scroll);return box;
    }
    private View row(Context c,TLRPC.Dialog d){LinearLayout row=AsgramUi.row(c);row.setPadding(AsgramUi.dp(20),AsgramUi.dp(12),AsgramUi.dp(20),AsgramUi.dp(12));
        BackupImageView avatar=new BackupImageView(c);avatar.setRoundRadius(AsgramUi.dp(25));avatar.getImageReceiver().setCurrentAccount(currentAccount);
        Object peer=d.id>0&&!DialogObject.isEncryptedDialog(d.id)?getMessagesController().getUser(d.id):d.id<0?getMessagesController().getChat(-d.id):null;
        if(peer instanceof TLRPC.User){TLRPC.User user=(TLRPC.User)peer;avatar.setForUserOrChat(user,new AvatarDrawable(user));}else if(peer instanceof TLRPC.Chat){TLRPC.Chat chat=(TLRPC.Chat)peer;avatar.setForUserOrChat(chat,new AvatarDrawable(chat));}else avatar.setImageDrawable(new AvatarDrawable());
        row.addView(avatar,AsgramUi.lp(50,50));LinearLayout text=AsgramUi.column(c);TextView name=AsgramUi.text(c,title(d),16,palette.text,true);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);text.addView(name);AsgramUi.gap(text,5);
        String detail=DialogObject.isEncryptedDialog(d.id)?"Secret chat":kind(d);TLRPC.Chat chat=d.id<0?getMessagesController().getChat(-d.id):null;if(chat!=null)detail+=(chat.username==null||chat.username.isEmpty())?" · Private":" · @"+chat.username;
        text.addView(AsgramUi.text(c,detail,12,palette.secondary,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=AsgramUi.dp(14);row.addView(text,lp);
        if(d.unread_count>0){TextView count=AsgramUi.text(c,Integer.toString(d.unread_count),12,palette.primary,true);count.setPadding(AsgramUi.dp(10),AsgramUi.dp(6),AsgramUi.dp(10),AsgramUi.dp(6));count.setBackground(AsgramUi.shape(palette.tint,16));row.addView(count);}
        row.setContentDescription(title(d)+", "+detail);row.setBackground(org.telegram.ui.ActionBar.Theme.createSelectorDrawable(palette.tint));row.setOnClickListener(v->open(d));return row;
    }
    @Override public void onResume(){super.onResume();if(fragmentView!=null){palette=new AsgramUi.Palette(fragmentView.getContext());fragmentView.setBackgroundColor(palette.background);actionBar.setBackgroundColor(palette.background);actionBar.setTitleColor(palette.text);actionBar.setSubtitleColor(palette.secondary);actionBar.setItemsColor(palette.text,false);reload();}}
    @Override public void didReceivedNotification(int id,int account,Object...args){reload();}
    @Override public boolean isLightStatusBar(){return palette==null||!palette.dark;}
    private final class SpaceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        @Override public int getItemCount(){return 2+Math.max(1,visible.size());}
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int t){FrameLayout f=new FrameLayout(parent.getContext());f.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new RecyclerView.ViewHolder(f){};}
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,int position){FrameLayout f=(FrameLayout)holder.itemView;f.removeAllViews();View child;
            if(position==0)child=header(f.getContext());else if(position==getItemCount()-1){
                boolean more=!getMessagesController().isServerDialogsEndReached(0)||!getMessagesController().isServerDialogsEndReached(1);
                LinearLayout footer=AsgramUi.column(f.getContext());footer.setPadding(AsgramUi.dp(20),AsgramUi.dp(16),AsgramUi.dp(20),AsgramUi.dp(16));
                if(more)footer.addView(AsgramUi.button(f.getContext(),"Load more spaces",palette,false,()->{for(int folder:new int[]{0,1})if(!getMessagesController().isServerDialogsEndReached(folder)&&!getMessagesController().isLoadingDialogs(folder))getMessagesController().loadDialogs(folder,-1,100,false);}),AsgramUi.lp(-1,-2));
                else footer.addView(AsgramUi.text(f.getContext(),"All available spaces are loaded.",12,palette.secondary,false));child=footer;
            }else if(visible.isEmpty()){TextView empty=AsgramUi.text(f.getContext(),"No "+type.toLowerCase(Locale.ROOT)+" spaces to show. Try another filter or open a Telegram link.",16,palette.secondary,false);empty.setPadding(AsgramUi.dp(24),AsgramUi.dp(28),AsgramUi.dp(24),AsgramUi.dp(28));child=empty;}else child=row(f.getContext(),visible.get(position-1));f.addView(child,LayoutHelper.createFrame(-1,-2));}
    }
}
