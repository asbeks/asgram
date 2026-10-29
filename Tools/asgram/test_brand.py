"""Checks shipped native theme assets and ASGRAM onboarding boundaries."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
def theme(name):
 return {k:int(v)&0xffffffff for k,v in (line.split('=',1) for line in (ROOT/'TMessagesProj/src/main/assets'/name).read_text().splitlines() if '=' in line)}
def luminance(color):
 rgb=[(color>>shift&255)/255 for shift in (16,8,0)]
 rgb=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in rgb]
 return sum(x*w for x,w in zip(rgb,(.2126,.7152,.0722)))
def contrast(a,b):
 x,y=sorted([luminance(a),luminance(b)])
 return (y+.05)/(x+.05)
class BrandTest(unittest.TestCase):
 def test_both_native_palettes_have_readable_text(self):
  for name in ['asgram.attheme','asgram-night.attheme']:
   t=theme(name)
   for key in ['windowBackgroundWhiteBlackText','windowBackgroundWhiteGrayText2','windowBackgroundWhiteBlueHeader','windowBackgroundWhiteBlueText','windowBackgroundWhiteValueText','windowBackgroundWhiteHintText','windowBackgroundWhiteInputFieldActivated']:
    with self.subTest(theme=name,key=key): self.assertGreaterEqual(contrast(t[key],t['windowBackgroundWhite']),4.5)
   self.assertGreaterEqual(contrast(t['featuredStickers_buttonText'],t['featuredStickers_addButton']),4.5)
 def test_onboarding_uses_own_identity_and_native_authentication(self):
  source=(ROOT/'TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java').read_text()
  self.assertIn('new LoginActivity()',source)
  self.assertIn('An unofficial Telegram client',source)
  self.assertNotIn('Intro.setTelegramTextures',source)
  self.assertIn('setLanguageOnly(true)',source)
 def test_community_invitation_never_subscribes_for_the_account(self):
  source=(ROOT/'TMessagesProj/src/main/java/org/telegram/ui/asgram/AsgramCommunityInvitation.java').read_text()
  self.assertIn('isClientActivated()',source)
  self.assertIn('ChatObject.isNotInChat',source)
  self.assertIn('"Not now"',source)
  for mutation in ['TL_channels_joinChannel','addUserToChat','TL_messages_importChatInvite']:
   self.assertNotIn(mutation,source)
 def test_standard_signin_does_not_offer_the_development_network(self):
  source=(ROOT/'TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java').read_text()
  self.assertIn('final boolean allowTestBackend = getConnectionsManager().isTestBackend();',source)
 def test_feed_does_not_read_private_chats_or_upload_account_content(self):
  source=(ROOT/'TMessagesProj/src/main/java/org/telegram/messenger/asgram/AsgramNativeFeed.java').read_text()
  self.assertIn('ChatObject.isChannelAndNotMegaGroup',source)
  self.assertIn('chat.username.equalsIgnoreCase(entry.username)',source)
  self.assertIn('bindRequestToGuid',source)
  for mutation in ['TL_channels_joinChannel','addUserToChat','TL_messages_importChatInvite','sendMessage']:
   self.assertNotIn(mutation,source)
 def test_spaces_uses_native_creation_without_automatic_account_mutation(self):
  source=(ROOT/'TMessagesProj/src/main/java/org/telegram/ui/AsgramSpacesActivity.java').read_text()
  for capability in ['"Private"','"Groups"','"Channels"','"Bots"','new ChannelCreateActivity','new GroupCreateActivity','"createSecretChat"','"BotFather"']:
   self.assertIn(capability,source)
  self.assertNotIn('sendMessage(',source)
if __name__=='__main__': unittest.main()
