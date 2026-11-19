package org.telegram.messenger.asgram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Community;
import org.telegram.messenger.asgram.AsgramCommunityMembership.Status;
import org.telegram.tgnet.TLRPC;

import java.util.EnumMap;

public class AsgramCommunityMembershipTest {
    @Test public void accessRequiresConfirmedMembershipInAllFourCommunities() {
        EnumMap<Community, Status> states = new EnumMap<>(Community.class);
        states.put(Community.NEWS, Status.JOINED);
        states.put(Community.UNIVERSITY, Status.JOINED);
        states.put(Community.DISCUSSION, Status.JOINED);
        assertFalse(AsgramCommunityMembership.hasAccess(states));
        for (Status state : Status.values()) {
            states.put(Community.SPORTS, state);
            assertEquals(state == Status.JOINED, AsgramCommunityMembership.hasAccess(states));
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
