package org.telegram.messenger.asgram;

import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Checks the signed-in account and joins only the destinations named on the consent screen. */
public final class AsgramCommunityMembership {
    public enum Community {
        NEWS("Abdii Sabaa", "ABDII_SABAA", null),
        UNIVERSITY("Abdii Sabaa University", "abdii_sabaa_university", null),
        SPORTS("Abdii Sabaa Ispoortii", null, "7P24H-U3sWdlNzJk"),
        DISCUSSION("Garee Miidiyaa Abdii Sabaa", "Garee_Abdii_Sabaa", null);

        public final String title;
        final String username, inviteHash;

        Community(String title, String username, String inviteHash) {
            this.title = title;
            this.username = username;
            this.inviteHash = inviteHash;
        }
    }

    public enum Status {
        CHECKING("Checking…"), JOINED("Joined"), MISSING("Membership required"),
        APPROVAL_PENDING("Waiting for approval"), UNAVAILABLE("Couldn’t verify");

        public final String label;

        Status(String label) { this.label = label; }
    }

    private final int account;
    private final long userId;
    private final ConnectionsManager connection;
    private final MessagesController messages;
    private final SharedPreferences preferences;
    private final Runnable onChange;
    private final EnumMap<Community, Status> states = new EnumMap<>(Community.class);
    private final EnumMap<Community, TLRPC.InputChannel> channels = new EnumMap<>(Community.class);
    private int requestGuid;
    private boolean busy;
    private final Runnable timeout = this::finish;

    public AsgramCommunityMembership(int account, long userId, SharedPreferences preferences, Runnable onChange) {
        this.account = account;
        this.userId = userId;
        this.preferences = preferences;
        this.onChange = onChange;
        connection = ConnectionsManager.getInstance(account);
        messages = MessagesController.getInstance(account);
    }

    public Status status(Community community) { return states.getOrDefault(community, Status.CHECKING); }

    public boolean isBusy() { return busy; }

    public boolean hasAccess() { return hasAccess(states); }

    static boolean hasAccess(Map<Community, Status> states) {
        for (Community community : Community.values()) {
            if (states.get(community) != Status.JOINED) return false;
        }
        return true;
    }

    public boolean canJoin() { return states.containsValue(Status.MISSING); }

    public void check(boolean joinMissing) {
        if (busy || !sameAccount()) return;
        busy = true;
        requestGuid = ConnectionsManager.generateClassGuid();
        states.clear();
        channels.clear();
        onChange.run();
        AndroidUtilities.runOnUIThread(timeout, 30_000);
        for (Community community : Community.values()) {
            check(community, status -> {
                states.put(community, status);
                if (states.size() != Community.values().length) {
                    onChange.run();
                } else if (joinMissing) {
                    joinNext();
                } else {
                    finish();
                }
            });
        }
    }

    private void check(Community community, Consumer<Status> result) {
        if (community.inviteHash != null) {
            TLRPC.TL_messages_checkChatInvite request = new TLRPC.TL_messages_checkChatInvite();
            request.hash = community.inviteHash;
            send(request, (response, error) -> {
                if (response instanceof TLRPC.TL_chatInviteAlready && !ChatObject.isNotInChat(((TLRPC.ChatInvite) response).chat)) {
                    clearApproval(community);
                    result.accept(Status.JOINED);
                } else if (response instanceof TLRPC.ChatInvite && ((TLRPC.ChatInvite) response).subscription_pricing == null) {
                    result.accept(missing(community));
                } else {
                    result.accept(Status.UNAVAILABLE);
                }
            });
            return;
        }

        TLRPC.TL_contacts_resolveUsername resolve = new TLRPC.TL_contacts_resolveUsername();
        resolve.username = community.username;
        send(resolve, (response, error) -> {
            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer resolved = (TLRPC.TL_contacts_resolvedPeer) response;
                for (TLRPC.Chat chat : resolved.chats) {
                    if (resolved.peer instanceof TLRPC.TL_peerChannel && chat.id == resolved.peer.channel_id && ChatObject.isChannel(chat)) {
                        TLRPC.InputChannel channel = MessagesController.getInputChannel(chat);
                        channels.put(community, channel);
                        if (!chat.min && chat.left && !ChatObject.isKickedFromChat(chat)) {
                            result.accept(missing(community));
                            return;
                        }
                        TLRPC.TL_channels_getParticipant request = new TLRPC.TL_channels_getParticipant();
                        request.channel = channel;
                        request.participant = new TLRPC.TL_inputPeerSelf();
                        send(request, (membership, failure) -> {
                            Status state = participantStatus(membership, failure);
                            if (state == Status.JOINED) clearApproval(community);
                            result.accept(state == Status.MISSING ? missing(community) : state);
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

    private Status missing(Community community) {
        return preferences.getLong(approvalKey(community), 0) > System.currentTimeMillis()
                ? Status.APPROVAL_PENDING : Status.MISSING;
    }

    private String approvalKey(Community community) {
        return "community_approval_" + userId + "_" + community.name();
    }

    private void clearApproval(Community community) {
        String key = approvalKey(community);
        if (preferences.contains(key)) preferences.edit().remove(key).apply();
    }

    private void joinNext() {
        for (Community community : Community.values()) {
            if (status(community) != Status.MISSING) continue;
            states.put(community, Status.CHECKING);
            onChange.run();
            TLObject request;
            if (community.inviteHash != null) {
                TLRPC.TL_messages_importChatInvite invite = new TLRPC.TL_messages_importChatInvite();
                invite.hash = community.inviteHash;
                request = invite;
            } else {
                TLRPC.TL_channels_joinChannel join = new TLRPC.TL_channels_joinChannel();
                join.channel = channels.get(community);
                request = join;
            }
            send(request, (response, error) -> {
                if ("INVITE_REQUEST_SENT".equals(error)) {
                    long retryAfter = System.currentTimeMillis() + 24 * 60 * 60_000L;
                    preferences.edit().putLong(approvalKey(community), retryAfter).apply();
                    states.put(community, Status.APPROVAL_PENDING);
                    joinNext();
                    return;
                }
                TLRPC.Updates updates = response instanceof TLRPC.TL_chatInviteJoinResultOk
                        ? ((TLRPC.TL_chatInviteJoinResultOk) response).updates
                        : response instanceof TLRPC.Updates ? (TLRPC.Updates) response : null;
                if (updates != null) messages.processUpdates(updates, false);
                if (updates != null || "USER_ALREADY_PARTICIPANT".equals(error)) {
                    check(community, status -> {
                        // A follow-up check may lag behind a join. Leave it for the next button tap.
                        states.put(community, status == Status.MISSING ? Status.UNAVAILABLE : status);
                        joinNext();
                    });
                } else {
                    states.put(community, Status.UNAVAILABLE);
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
        for (Community community : Community.values()) {
            if (status(community) == Status.CHECKING) states.put(community, Status.UNAVAILABLE);
        }
        onChange.run();
    }

    public void close() {
        AndroidUtilities.cancelRunOnUIThread(timeout);
        if (requestGuid != 0) connection.cancelRequestsForGuid(requestGuid);
        requestGuid = 0;
        busy = false;
    }
}
