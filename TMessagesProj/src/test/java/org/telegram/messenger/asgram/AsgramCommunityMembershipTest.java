package org.telegram.messenger.asgram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Community;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Status;
import org.telegram.tgnet.TLRPC;

public class AsgramCommunityMembershipTest {
    @Test public void setupUsesAmharicChannelAndExcludesDiscussionGroup() {
        assertEquals("abdiisabaa", Community.AMHARIC.username);
        assertEquals(4, Community.values().length);
        for (Community channel : Community.values()) {
            assertFalse("Garee_Abdii_Sabaa".equalsIgnoreCase(channel.username));
        }
    }

    @Test public void classifiesMembershipFromTelegramRatherThanNetworkErrors() {
        TLRPC.TL_channels_channelParticipant response = new TLRPC.TL_channels_channelParticipant();
        assertEquals(Status.UNAVAILABLE, AsgramCommunityMembership.participantStatus(response, null));
        response.participant = new TLRPC.TL_channelParticipantSelf();
        assertEquals(Status.JOINED, AsgramCommunityMembership.participantStatus(response, null));
        response.participant = new TLRPC.TL_channelParticipantLeft();
        assertEquals(Status.MISSING, AsgramCommunityMembership.participantStatus(response, null));
        assertEquals(Status.MISSING, AsgramCommunityMembership.participantStatus(null, "USER_NOT_PARTICIPANT"));
        assertEquals(Status.UNAVAILABLE, AsgramCommunityMembership.participantStatus(null, "FLOOD_WAIT_10"));
    }
}
