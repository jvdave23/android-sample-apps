package com.ooyala.sample.players;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.ooyala.android.DefaultPlayerInfo;
import com.ooyala.android.OoyalaPlayer;
import com.ooyala.android.OoyalaNotification;
import com.ooyala.android.PlayerDomain;
import com.ooyala.android.PlayerInfo;
import com.ooyala.android.StreamSelector;
import com.ooyala.android.configuration.Options;
import com.ooyala.android.item.Stream;
import com.ooyala.sample.R;

import com.ooyala.android.skin.OoyalaSkinLayout;
import com.ooyala.android.skin.OoyalaSkinLayoutController;
import com.ooyala.android.skin.configuration.SkinOptions;
import com.ooyala.android.util.SDCardLogcatOoyalaEventsLogger;


import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * This activity illustrates how you can play basic playback video using the Skin SDK
 * you can also play Ooyala and VAST advertisements programmatically
 * through the SDK
 *
 */
public class OoyalaSkinPlayerActivity extends Activity implements Observer, DefaultHardwareBackBtnHandler {
  final String TAG = this.getClass().toString();

  String EMBED = null;
  String PCODE = null;
  String DOMAIN = null;

  // Write the sdk events text along with events count to log file in sdcard if the log file already exists
  SDCardLogcatOoyalaEventsLogger Playbacklog= new SDCardLogcatOoyalaEventsLogger();

  protected OoyalaSkinLayoutController playerLayoutController;
  protected OoyalaPlayer player;

  /**
   * Override the stream class so that we can manually set the WVServerPath
   */
  private static class WVStream extends Stream {
    WVStream(String url, String deliveryType) {
      super(url, deliveryType);
    }

    public void setWidevineServerPath(String path) {
      _widevineServerPath = path;
    }
  }

  /**
   * Create a new StreamSelector which lets us modify the stream before it goes into ExoPlayer
   * NOTE: This removes all support for HLS or MP4 assets, and will only play DASH videos
   */
  private static class NewStreamSelector implements StreamSelector {
    public NewStreamSelector() {
    }

    //Copied directly from decompiled Stream.class
    boolean betterThan(Stream original, Stream other, boolean isWifiEnabled) {
      return original.getCombinedBitrate() == other.getCombinedBitrate() && original.getHeight() > other.getHeight() ? true : (isWifiEnabled ? original.getCombinedBitrate() > other.getCombinedBitrate() : Math.abs(400 - original.getCombinedBitrate()) < Math.abs(400 - other.getCombinedBitrate()));
    }

    public Stream bestStream(Set<Stream> streams, boolean isWifiEnabled) {
      if (streams != null && streams.size() != 0) {
        Stream bestBitrateStream = null;
        Iterator var4 = streams.iterator();
        while (var4.hasNext()) {
          Stream stream = (Stream) var4.next();

          //If the asset is DASH, manually set the Widevine Server Path
          if (stream.getDeliveryType().equals("dash")) {
            if (stream.getWidevineServerPath() == null) {
              WVStream s = new WVStream(stream.decodedURL().toString(), stream.getDeliveryType());

              //TODO: Create the correct Widevine Server Path based on the asset (generally pcode/embed_code in the url)
              s.setWidevineServerPath("http://player.ooyala.com/sas/drm2/tsbmYyOjRH6dK3rmGxeTP2uleUj_/1uczVsNTE6v894WfD2TSY7qmDSQdGr73/widevine_modular/ooyala");
              return s;
            }
            return stream;
          }

          // Get best MP4 or HLS stream
          if (Stream.isDeliveryTypePlayable(stream) && Stream.isProfilePlayable(stream) && (bestBitrateStream == null || betterThan(stream, bestBitrateStream, isWifiEnabled))) {
            bestBitrateStream = stream;
          }

        }
        return bestBitrateStream;
      }
      return null;
    }
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(getIntent().getExtras().getString("selection_name"));
    setContentView(R.layout.player_skin_simple_layout);
    EMBED = getIntent().getExtras().getString("embed_code");
    PCODE = getIntent().getExtras().getString("pcode");
    DOMAIN = getIntent().getExtras().getString("domain");

    // Get the SkinLayout from our layout xml
    OoyalaSkinLayout skinLayout = (OoyalaSkinLayout)findViewById(R.id.ooyalaSkin);
    Stream.setStreamSelector(new NewStreamSelector());
    // Create the OoyalaPlayer, with some built-in UI disabled
    PlayerDomain domain = new PlayerDomain(DOMAIN);
    Options options = new Options.Builder().setShowPromoImage(false).setShowNativeLearnMoreButton(false).setUseExoPlayer(true).build();
    player = new OoyalaPlayer(PCODE, domain, options);

    //Create the SkinOptions, and setup React
    JSONObject overrides = createSkinOverrides();
    SkinOptions skinOptions = new SkinOptions.Builder().setSkinOverrides(overrides).build();
    playerLayoutController = new OoyalaSkinLayoutController(getApplication(), skinLayout, player, skinOptions);

    player.addObserver(this);

    if (player.setEmbedCode(EMBED)) {
      //Uncomment for autoplay
      //player.play();
    }
    else {
      Log.e(TAG, "Asset Failure");
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    playerLayoutController.onKeyDown(keyCode, event);
    return super.onKeyDown(keyCode, event);
  }

  /** Start DefaultHardwareBackBtnHandler **/
  @Override
  public void invokeDefaultOnBackPressed() {
    super.onBackPressed();
  }
  /** End DefaultHardwareBackBtnHandler **/

  /** Start Activity methods for Skin **/
  @Override
  protected void onPause() {
    super.onPause();
    if (playerLayoutController != null) {
      playerLayoutController.onPause();
    }
    Log.d(TAG, "Player Activity Stopped");
    if (player != null) {
      player.suspend();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (playerLayoutController != null) {
      playerLayoutController.onResume( this, this );
    }
    Log.d(TAG, "Player Activity Restarted");
    if (player != null) {
      player.resume();
    }
  }

  @Override
  public void onBackPressed() {
    if (playerLayoutController != null) {
      playerLayoutController.onBackPressed();
    } else {
      super.onBackPressed();
    }
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (playerLayoutController != null) {
      playerLayoutController.onDestroy();
    }
  }
  /** End Activity methods for Skin **/

  /**
   * Create skin overrides to show up in the skin.
   * Default commented. Uncomment to show changes to the start screen.
   * @return the overrides to apply to the skin.json in the assets folder
   */
  private JSONObject createSkinOverrides() {
    JSONObject overrides = new JSONObject();
//    JSONObject startScreenOverrides = new JSONObject();
//    JSONObject playIconStyleOverrides = new JSONObject();
//    try {
//      playIconStyleOverrides.put("color", "red");
//      startScreenOverrides.put("playButtonPosition", "bottomLeft");
//      startScreenOverrides.put("playIconStyle", playIconStyleOverrides);
//      overrides.put("startScreen", startScreenOverrides);
//    } catch (Exception e) {
//      Log.e(TAG, "Exception Thrown", e);
//    }
    return overrides;
  }

  @Override
  protected void onStop() {
    super.onStop();

  }

  @Override
  protected void onRestart() {
    super.onRestart();

  }

  /**
   * Listen to all notifications from the OoyalaPlayer
   */
  @Override
  public void update(Observable arg0, Object argN) {
    if (arg0 != player) {
      return;
    }

    final String arg1 = OoyalaNotification.getNameOrUnknown(argN);
    if (arg1 == OoyalaPlayer.TIME_CHANGED_NOTIFICATION_NAME) {
      return;
    }

    if (arg1 == OoyalaPlayer.ERROR_NOTIFICATION_NAME) {
      final String msg = "Error Event Received";
      if (player != null && player.getError() != null) {
        Log.e(TAG, msg, player.getError());
      }
      else {
        Log.e(TAG, msg);
      }
      return;
    }

    // Automation Hook: to write Notifications to a temporary file on the device/emulator
    String text="Notification Received: " + arg1 + " - state: " + player.getState();
    // Automation Hook: Write the event text along with event count to log file in sdcard if the log file exists
    Playbacklog.writeToSdcardLog(text);

    Log.d(TAG, "Notification Received: " + arg1 + " - state: " + player.getState());
  }

}
