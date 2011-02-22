package com.manning.aip.brewmap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.manning.aip.brewmap.model.Pub;
import com.manning.aip.brewmap.xml.BeerMappingParser;
import com.manning.aip.brewmap.xml.BeerMappingXmlPullParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// NOTE -- this is far from complete, just an interim checkin

// 2.2 emulator fails geocoding -- http://code.google.com/p/android/issues/detail?id=8816

// TODO for main page have entry form, and button to get pubs near me
// TODO try to skip geocoding, or do it in a batch?

public class Main extends Activity {

   private static final String CITY = "CITY";
   private static final String STATE = "STATE";
   private static final String PIECE = "PIECE";

   private BrewMapApp app;

   private ProgressDialog progressDialog;

   private Geocoder geocoder;

   private BeerMappingParser parser;
   
   private Handler handler;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      app = (BrewMapApp) getApplication();      
      
      // TODO not sure we need the GPS at all? (if not move this little checker to another example to show people how to do this)
      // determine if GPS is enabled or not, if not prompt user to enable it
      LocationManager lMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
      if (!lMgr.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle("GPS Settings are not enabled")
                  .setMessage("Would you like to go the location settings and enable GPS?").setCancelable(true)
                  .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                     }
                  }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                     }
                  });
         AlertDialog alert = builder.create();
         alert.show();
      }

      progressDialog = new ProgressDialog(this);
      progressDialog.setCancelable(false);
      progressDialog.setMessage("Retrieving data...");      
      
      geocoder = new Geocoder(this);
      // note that API level 9 added the "isPresent" method which could be checked here

      parser = new BeerMappingXmlPullParser();

      handler = new Handler() {
         public void handleMessage(Message m) {
            if (progressDialog.isShowing()) {
               progressDialog.hide();
               Toast.makeText(Main.this, "HANDLER RETURNED -- lat:" + m.arg1 + " lon:" + m.arg2, Toast.LENGTH_SHORT).show();
               // TODO parse and center map using lat/lon returned
            }
         }
      };
      
      // TODO rename LocationUtil (or make static util-ish method or such?)
      final LocationUtil locUtil = new LocationUtil(lMgr, handler);       
      
      final EditText input = (EditText) findViewById(R.id.input);

      Button near = (Button) findViewById(R.id.button_nearby);
      near.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            progressDialog.show();
            locUtil.getLocation(); // fire off async call to get current location, which will use handler    
         }
      });
      
      Button search = (Button) findViewById(R.id.button_search);
      search.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            new ParseFeedTask().execute(new String[] { PIECE, input.getText().toString() });
         }
      });
   }

   private void handleResults(List<Pub> pubs) {
      if (pubs != null && !pubs.isEmpty()) {
         app.setPubs(pubs);
         startActivity(new Intent(this, MapResults.class));
      } else {
         Toast.makeText(this, "Pubs empty!", Toast.LENGTH_SHORT).show();
      }
   }

   private class ParseFeedTask extends AsyncTask<String, Void, List<Pub>> {

      @Override
      protected void onPreExecute() {
         progressDialog.show();
      }

      @Override
      protected List<Pub> doInBackground(String... args) {
         List<Pub> result = new ArrayList<Pub>();
         if (args == null || args.length != 2) {
            return result;
         }
         String type = args[0];
         String input = args[1];
         if (type.equals(CITY)) {
            result = parser.parseCity(input);
         } else if (type.equals(STATE)) {
            result = parser.parseState(input);
         } else if (type.equals(PIECE)) {
            result = parser.parsePiece(input);
         }

         // geocode the city/state/zip form addresses in the task too
         if (result != null) {
            for (Pub p : result) {
               try {
                  List<android.location.Address> addresses =
                           geocoder.getFromLocationName(p.getAddress().getLocationName(), 1);
                  if (addresses != null && !addresses.isEmpty()) {
                     android.location.Address a = addresses.get(0);
                     p.setLatitude(a.getLatitude());
                     p.setLongitude(a.getLongitude());
                     System.out.println("*** GEOCODED ADDRESS: " + a);
                  }
               } catch (IOException e) {
                  Log.e(Constants.LOG_TAG, "Error geocoding location name", e);
               }
            }
         }

         return result;
      }

      @Override
      protected void onPostExecute(List<Pub> pubs) {
         if (progressDialog.isShowing()) {
            progressDialog.hide();
         }
         handleResults(pubs);
      }
   }
}