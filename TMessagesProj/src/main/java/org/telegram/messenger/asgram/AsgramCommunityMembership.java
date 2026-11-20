package org.telegram.messenger.asgram;

import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Verifies the signed-in Telegram account. Only the explicit Join action can subscribe it. */
public final class AsgramCommunityMembership {
    public enum Status { JOINED, MISSING, APPROVAL_PENDING, UNAVAILABLE }

    private final int account;
    private final long userId;
    private final ConnectionsManager connection;
    private final MessagesController messages;
    private final SharedPreferences preferences;
    private final List<AsgramDirectory.Entry> entries;
    private final Runnable onChange;
    private Map<String, Status> states = new HashMap<>();
    private final Map<String, Status> checked = new HashMap<>();
    private final Map<String, TLRPC.InputChannel> channels = new HashMap<>();
    private int requestGuid;
    private final Runnable timeout = this::finish;

    public AsgramCommunityMembership(int account, long userId, SharedPreferences preferences,
                                     List<AsgramDirectory.Entry> entries, Runnable onChange) {
        this.account = account;
        this.userId = userId;
        this.preferences = preferences;
        this.entries = entries;
        this.onChange = onChange;
        connection = ConnectionsManager.getInstance(account);
        messages = MessagesController.getInstance(account);
    }

    public boolean isBusy() { return requestGuid != 0; }
    public boolean needsJoin() { return states.containsValue(Status.MISSING); }
    public boolean awaitingApproval() { return states.containsValue(Status.APPROVAL_PENDING); }
    public boolean needsSetup() { return !allJoined(entries, states); }

    static boolean allJoined(List<AsgramDirectory.Entry> entries, Map<String, Status> states) {
        for (AsgramDirectory.Entry entry : entries) {
            if (states.get(entry.username) != Status.JOINED) return false;
        }
        return true;
    }

    public void verify() { start(false); }
    public void joinMissing() { start(true); }

    private void start(boolean join) {
        if (isBusy() || !sameAccount() || entries.isEmpty()) return;
        requestGuid = ConnectionsManager.generateClassGuid();
        checked.clear();
        channels.clear();
        onChange.run();
        AndroidUtilities.runOnUIThread(timeout, 25_000);
        for (AsgramDirectory.Entry entry : entries) {
            check(entry.username, status -> {
                checked.put(entry.username, status);
                if (checked.size() == entries.size()) {
                    if (join) joinNext();
                    else finish();
                }
            });
        }
    }

    private void check(String username, Consumer<Status> result) {
        TLRPC.TL_contacts_resolveUsername resolve = new TLRPC.TL_contacts_resolveUsername();
        resolve.username = username;
        send(resolve, (response, error) -> {
            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer resolved = (TLRPC.TL_contacts_resolvedPeer) response;
                for (TLRPC.Chat chat : resolved.chats) {
                    if (resolved.peer instanceof TLRPC.TL_peerChannel && chat.id == resolved.peer.channel_id
                            && ChatObject.isChannelAndNotMegaGroup(chat)
                            && username.equalsIgnoreCase(ChatObject.getPublicUsername(chat))) {
                        TLRPC.InputChannel channel = MessagesController.getInputChannel(chat);
                        channels.put(username, channel);
                        TLRPC.TL_channels_getParticipant request = new TLRPC.TL_channels_getParticipant();
                        request.channel = channel;
                        request.participant = new TLRPC.TL_inputPeerSelf();
                        send(request, (membership, failure) -> {
                            Status state = participantStatus(membership, failure);
                            if (state == Status.JOINED) preferences.edit().remove(approvalKey(username)).apply();
                            if (state == Status.MISSING && preferences.getLong(approvalKey(username), 0) > System.currentTimeMillis()) {
                                state = Status.APPROVAL_PENDING;
                            }
                            result.accept(state);
                        });
                        return;
                    }
                }
            }
            result.accept(Status.UNAVAILABLE);
        });
    }

    static Status participantStatus(TLObject response, String error) {
        if (response instanceof TLRPC.TL_channels_channelParticipant) {
            TLRPC.ChannelParticipant participant = ((TLRPC.TL_channels_channelParticipant) response).participant;
            if (participant != null) {
                return participant instanceof TLRPC.TL_channelParticipantLeft
                        || participant instanceof TLRPC.TL_channelParticipantBanned && participant.left
                        ? Status.MISSING : Status.JOINED;
            }
        }
        return "USER_NOT_PARTICIPANT".equals(error) ? Status.MISSING : Status.UNAVAILABLE;
    }

    private String approvalKey(String username) {
        return "feed_join_approval_" + userId + "_" + username;
    }

    private void joinNext() {
        for (AsgramDirectory.Entry entry : entries) {
            String username = entry.username;
            if (checked.get(username) != Status.MISSING) continue;
            TLRPC.TL_channels_joinChannel join = new TLRPC.TL_channels_joinChannel();
            join.channel = channels.get(username);
            send(join, (response, error) -> {
                if ("INVITE_REQUEST_SENT".equals(error)) {
                    preferences.edit().putLong(approvalKey(username), System.currentTimeMillis() + 86_400_000L).apply();
                    checked.put(username, Status.APPROVAL_PENDING);
                    joinNext();
                } else if (response instanceof TLRPC.Updates || "USER_ALREADY_PARTICIPANT".equals(error)) {
                    if (response instanceof TLRPC.Updates) messages.processUpdates((TLRPC.Updates) response, false);
                    check(username, status -> {
                        checked.put(username, status == Status.MISSING ? Status.UNAVAILABLE : status);
                        joinNext();
                    });
                } else {
                    checked.put(username, Status.UNAVAILABLE);
                    joinNext();
                }
            });
            return;
        }
        finish();
    }

    private interface Reply { void complete(TLObject response, String error); }

    private void send(TLObject request, Reply callback) {
        int operation = requestGuid;
        int id = connection.sendRequest(request, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (requestGuid == operation && sameAccount()) callback.complete(response, error == null ? null : error.text);
        }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagDoNotWaitFloodWait);
        connection.bindRequestToGuid(id, operation);
    }

    private boolean sameAccount() { return UserConfig.getInstance(account).getClientUserId() == userId; }

    private void finish() {
        close();
        if (!sameAccount()) return;
        Map<String, Status> result = new HashMap<>();
        for (AsgramDirectory.Entry entry : entries) {
            Status status = checked.getOrDefault(entry.username, Status.UNAVAILABLE);
            if (status == Status.UNAVAILABLE && states.get(entry.username) == Status.JOINED) status = Status.JOINED;
            result.put(entry.username, status);
        }
        states = result;
        onChange.run();
    }

    public void close() {
        AndroidUtilities.cancelRunOnUIThread(timeout);
        if (requestGuid != 0) connection.cancelRequestsForGuid(requestGuid);
        requestGuid = 0;
    }
}
