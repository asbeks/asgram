"""Release boundaries that complement the executable consent/service policy tests."""
from pathlib import Path
import tempfile
import unittest
from check_manifest import check
ROOT=Path(__file__).resolve().parents[2]
MANIFEST='''<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/><uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/><uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION"/>{extra}<application android:allowBackup="false"><meta-data android:name="com.google.android.geo.API_KEY" android:value="AIza-test-key-not-real"/><service android:name="org.telegram.messenger.LocationSharingService" android:foregroundServiceType="location"/></application></manifest>'''
class LocationReleaseTest(unittest.TestCase):
 def test_rejects_reintroduced_background_permission(self):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/'manifest.xml';p.write_text(MANIFEST.format(extra=''));check(p,True)
   p.write_text(MANIFEST.format(extra='').replace('foregroundServiceType="location"','foregroundServiceType="0x00000009"'));check(p,True)
   p.write_text(MANIFEST.format(extra='').replace('foregroundServiceType="location"','foregroundServiceType="0x00000001"'))
   with self.assertRaisesRegex(ValueError,'foreground location service'):check(p,True)
   for tag in ['uses-permission','uses-permission-sdk-23']:
    p.write_text(MANIFEST.format(extra=f'<{tag} android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>'))
    with self.assertRaisesRegex(ValueError,'all-the-time'):check(p,True)
 def test_signin_information_is_not_location_consent(self):
  source=(ROOT/'TMessagesProj/src/main/java/org/telegram/ui/asgram/AsgramLocationDisclosure.java').read_text()
  introduction=source.split('public static void showIntroduction')[1].split('public static void requestPermissions')[0]
  for forbidden in ['putInt(','.requestPermissions(','.getLastLocation(','.requestLocationUpdates(']:self.assertNotIn(forbidden,introduction)
  self.assertIn('Continue without location',introduction)
  self.assertIn('showIntroduction(', (ROOT/'TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java').read_text())
if __name__=='__main__':unittest.main()
