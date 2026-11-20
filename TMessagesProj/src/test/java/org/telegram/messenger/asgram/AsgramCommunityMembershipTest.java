package org.telegram.messenger.asgram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Status;
import org.telegram.tgnet.TLRPC;

public class AsgramCommunityMembershipTest {
    @Test public void everyConfiguredChannelMustBeJoined() {
        Map<String, Status> membership = new HashMap<>();
        for (AsgramDirectory.Entry channel : AsgramDirectory.PUBLISHERS) {
            membership.put(channel.username, Status.JOINED);
        }
        assertTrue(AsgramCommunityMembership.allJoined(AsgramDirectory.PUBLISHERS, membership));
        AsgramDirectory.Entry addition = new AsgramDirectory.Entry("testsource", "Test", "News");
        assertFalse(AsgramCommunityMembership.allJoined(Collections.singletonList(addition), membership));
        membership.put(addition.username, Status.APPROVAL_PENDING);
        assertFalse(AsgramCommunityMembership.allJoined(Collections.singletonList(addition), membership));
        assertTrue(AsgramCommunityMembership.allJoined(Collections.emptyList(), membership));
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
