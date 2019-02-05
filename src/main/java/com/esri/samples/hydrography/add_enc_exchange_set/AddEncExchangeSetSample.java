/*
 * Copyright 2019 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.esri.samples.hydrography.add_enc_exchange_set;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.hydrography.EncCell;
import com.esri.arcgisruntime.hydrography.EncDataset;
import com.esri.arcgisruntime.hydrography.EncExchangeSet;
import com.esri.arcgisruntime.layers.EncLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DrawStatus;
import com.esri.arcgisruntime.mapping.view.MapView;

public class AddEncExchangeSetSample extends Application {

  private MapView mapView;
  private Envelope completeExtent;

  @Override
  public void start(Stage stage) {

    try {
      // set the title and width of the stage
      stage.setTitle("Add ENC Exchange Set Sample");
      stage.setWidth(800);
      stage.setHeight(700);

      // create a stack pane and set it as the JavaFX scene's root
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);
      stage.setScene(scene);
      stage.show();

      // create a map view and add it to the stack pane
      mapView = new MapView();
      stackPane.getChildren().add(mapView);

      // show progress indicator when map is drawing
      ProgressIndicator progressIndicator = new ProgressIndicator();
      progressIndicator.setStyle("-fx-progress-color: white;");
      progressIndicator.setMaxSize(25, 25);
      progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
      stackPane.getChildren().add(progressIndicator);

      // hide progress indicator when map is done drawing
      mapView.addDrawStatusChangedListener(e -> {
        if (e.getDrawStatus() == DrawStatus.COMPLETED) {
          progressIndicator.setVisible(false);
        }
      });

      // create a map with the Oceans basemap and set it on the map view
      ArcGISMap map = new ArcGISMap(Basemap.createOceans());
      mapView.setMap(map);

      // load the ENC exchange set from local data
      File encPath = new File("./samples-data/enc/ExchangeSetwithoutUpdates/ENC_ROOT/CATALOG.031");
      EncExchangeSet encExchangeSet = new EncExchangeSet(Collections.singletonList(encPath.getAbsolutePath()));
      encExchangeSet.loadAsync();
      encExchangeSet.addDoneLoadingListener(() -> {
        if (encExchangeSet.getLoadStatus() == LoadStatus.LOADED) {
          // loop through the individual datasets of the exchange set
          for (EncDataset encDataset : encExchangeSet.getDatasets()) {
            // create an ENC layer with an ENC cell using the dataset
            EncLayer encLayer = new EncLayer(new EncCell(encDataset));
            // add the ENC layer to the map's operational layers to display it
            map.getOperationalLayers().add(encLayer);
            // combine the extents of each layer after loading to set the viewpoint to their complete extent
            encLayer.addDoneLoadingListener(() -> {
              if (encLayer.getLoadStatus() == LoadStatus.LOADED) {
                Envelope extent = encLayer.getFullExtent();
                if (completeExtent == null) {
                  completeExtent = extent;
                } else {
                  completeExtent = GeometryEngine.combineExtents(Arrays.asList(completeExtent, extent));
                }
                mapView.setViewpoint(new Viewpoint(completeExtent.getCenter(), 60000));
              } else {
                new Alert(Alert.AlertType.ERROR, "Failed to load ENC layer.").show();
              }
            });
          }
        } else {
          new Alert(Alert.AlertType.ERROR, "Failed to load ENC exchange set.").show();
        }
      });
    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {

    if (mapView != null) {
      mapView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }

}

