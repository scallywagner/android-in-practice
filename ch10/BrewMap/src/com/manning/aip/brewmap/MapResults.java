package com.manning.aip.brewmap;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.manning.aip.brewmap.model.Pub;

public class MapResults extends MapActivity {

   private MapView map;
   private MapController controller;
   private List<Overlay> overlays;

   private BrewMapApp app;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.map_results);

      app = (BrewMapApp) getApplication();

      map = (MapView) findViewById(R.id.map);
      map.setBuiltInZoomControls(true);

      List<Pub> pubs = app.getPubs();
      PubOverlay pubOverlay = new PubOverlay(this, pubs, this.getResources().getDrawable(R.drawable.beer_icon));
      overlays = map.getOverlays();
      overlays.add(pubOverlay);
      
      // zoom to the span (without having to calculate bounding box rectangle ourselves, nice for the people Android)
      controller.zoomToSpan(pubOverlay.getLatSpanE6(), pubOverlay.getLonSpanE6());
   }

   @Override
   protected boolean isRouteDisplayed() {
      // TODO Auto-generated method stub
      return false;
   }
}