/*
 * Copyright 2019 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.samples.subnetwork_trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.ProximityResult;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.LayerContent;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.ColorUtil;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.utilitynetworks.UtilityAssetGroup;
import com.esri.arcgisruntime.utilitynetworks.UtilityAssetType;
import com.esri.arcgisruntime.utilitynetworks.UtilityDomainNetwork;
import com.esri.arcgisruntime.utilitynetworks.UtilityElement;
import com.esri.arcgisruntime.utilitynetworks.UtilityNetwork;
import com.esri.arcgisruntime.utilitynetworks.UtilityNetworkSource;
import com.esri.arcgisruntime.utilitynetworks.UtilityTerminal;
import com.esri.arcgisruntime.utilitynetworks.UtilityTier;
import com.esri.arcgisruntime.utilitynetworks.UtilityTraceParameters;
import com.esri.arcgisruntime.utilitynetworks.UtilityTraceType;

public class SubnetworkTraceController {

  @FXML private RadioButton startingLocationsRadioButton;
  @FXML private ComboBox<UtilityTraceType> traceTypeComboBox;
  @FXML private ComboBox<UtilityTier> sourceTierComboBox;
  @FXML private ComboBox<UtilityTier> targetTierComboBox;
  @FXML
  private Button resetButton;
  @FXML
  private Button traceButton;
  @FXML
  private Label statusLabel;
  @FXML
  private MapView mapView;
  @FXML
  private ProgressIndicator progressIndicator;

  private ArrayList<UtilityElement> barriers;
  private ArrayList<UtilityElement> startingLocations;
  private GraphicsOverlay graphicsOverlay;
  private SimpleMarkerSymbol barrierPointSymbol;
  private SimpleMarkerSymbol startingPointSymbol;
  private UtilityNetwork utilityNetwork;
  private UtilityTraceParameters utilityTraceParameters;

  public void initialize() {
    try {
      progressIndicator = new ProgressIndicator();
      progressIndicator.setVisible(true);
      statusLabel.setText("Loading Utility Network...");

      // create a basemap and set it to the map view
      ArcGISMap map = new ArcGISMap(Basemap.createStreetsNightVector());
      mapView.setMap(map);
      mapView.setViewpointAsync(new Viewpoint(
        new Envelope(-9813547.35557238, 5129980.36635111, -9813185.0602376, 5130215.41254146,
          SpatialReferences.getWebMercator())));

      // create symbols for the starting point and barriers
      startingPointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, ColorUtil.colorToArgb(Color.GREEN), 20);
      barrierPointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.X, ColorUtil.colorToArgb(Color.RED), 20);

      // load the utility network data from the feature service and create feature layers
      String featureServiceURL =
        "https://sampleserver7.arcgisonline.com/arcgis/rest/services/UtilityNetwork/NapervilleElectric/FeatureServer";

      ServiceFeatureTable distributionLineFeatureTable = new ServiceFeatureTable(featureServiceURL + "/115");
      FeatureLayer distributionLineLayer = new FeatureLayer(distributionLineFeatureTable);

      ServiceFeatureTable electricDeviceFeatureTable = new ServiceFeatureTable(featureServiceURL + "/100");
      FeatureLayer electricDeviceLayer = new FeatureLayer(electricDeviceFeatureTable);

      // create and apply a renderer for the electric distribution lines feature layer
      distributionLineLayer.setRenderer(new SimpleRenderer(
        new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, ColorUtil.colorToArgb(Color.DARKCYAN), 3)));

      // add the feature layers to the map
      map.getOperationalLayers().addAll(Arrays.asList(distributionLineLayer, electricDeviceLayer));

      // create a graphics overlay and add it to the map view
      graphicsOverlay = new GraphicsOverlay();
      mapView.getGraphicsOverlays().add(graphicsOverlay);

      // set the map view's selection color
      mapView.getSelectionProperties().setColor(0xFFFFFF00);

      // create a list of starting locations for the trace
      startingLocations = new ArrayList<>();
      barriers = new ArrayList<>();

      // create and load the utility network
      utilityNetwork = new UtilityNetwork(featureServiceURL, map);
      utilityNetwork.loadAsync();
      utilityNetwork.addDoneLoadingListener(() -> {

        if (utilityNetwork.getLoadStatus() == LoadStatus.LOADED) {

          // add the trace type options to the combobox
          traceTypeComboBox.getItems()
            .addAll(UtilityTraceType.SUBNETWORK, UtilityTraceType.UPSTREAM, UtilityTraceType.DOWNSTREAM);
          traceTypeComboBox.getSelectionModel().select(0);

          //add the tiers to the combobox
          List<UtilityDomainNetwork> domainNetworks = utilityNetwork.getDefinition().getDomainNetworks();
          domainNetworks.forEach(domain -> sourceTierComboBox.getItems().addAll(domain.getTiers()));
          if (!sourceTierComboBox.getItems().isEmpty()) {
            sourceTierComboBox.getSelectionModel().select(0);
          }


          // enable the UI
          enableUI();

          // update the status text
          statusLabel.setText("Click on the network lines or points to add a utility element.");

        } else {
          new Alert(Alert.AlertType.ERROR, "Error loading Utility Network.").show();
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Uses the clicked map point to identify any utility elements in the utility network at the clicked location. Based
   * on the selection mode, the clicked utility element is added either to the starting locations or barriers for the
   * trace parameters. The appropriate graphic is created at the clicked location to mark the element as either a
   * starting location or barrier.
   *
   * @param e mouse event registered when the map view is clicked on
   */
  @FXML
  private void handleMapViewClicked(MouseEvent e) {
    if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {

      // show the progress indicator
      progressIndicator.setVisible(true);

      // set whether the user is adding a starting point or a barrier
      boolean isAddingStartingPoint = startingLocationsRadioButton.isSelected();

      // get the clicked map point
      Point2D screenPoint = new Point2D(e.getX(), e.getY());
      Point mapPoint = mapView.screenToLocation(screenPoint);

      // identify the feature to be used
      ListenableFuture<List<IdentifyLayerResult>> identifyLayerResultsFuture =
        mapView.identifyLayersAsync(screenPoint, 10, false);
      identifyLayerResultsFuture.addDoneListener(() -> {
        try {
          // get the result of the query
          List<IdentifyLayerResult> identifyLayerResults = identifyLayerResultsFuture.get();

          // return if no features are identified
          if (!identifyLayerResults.isEmpty()) {
            // retrieve the first result and get its contents
            IdentifyLayerResult firstResult = identifyLayerResults.get(0);
            LayerContent layerContent = firstResult.getLayerContent();
            // check that the result is a feature layer and has elements
            if (layerContent instanceof FeatureLayer && !firstResult.getElements().isEmpty()) {
              // retrieve the geoelements in the feature layer
              GeoElement identifiedElement = firstResult.getElements().get(0);
              if (identifiedElement instanceof ArcGISFeature) {
                // get the feature
                ArcGISFeature identifiedFeature = (ArcGISFeature) identifiedElement;

                // get the network source of the identified feature
                String featureTableName = identifiedFeature.getFeatureTable().getTableName();
                UtilityNetworkSource networkSource = utilityNetwork.getDefinition().getNetworkSource(featureTableName);

                UtilityElement utilityElement = null;

                // check if the network source is a junction or an edge
                if (networkSource.getSourceType() == UtilityNetworkSource.Type.JUNCTION) {
                  //  create a utility element with the identified feature
                  utilityElement = createUtilityElement(identifiedFeature, networkSource);
                }
                // check if the network source is an edge
                else if (networkSource.getSourceType() == UtilityNetworkSource.Type.EDGE &&
                  identifiedFeature.getGeometry().getGeometryType() == GeometryType.POLYLINE) {

                  //  create a utility element with the identified feature
                  utilityElement = utilityNetwork.createElement(identifiedFeature, null);

                  // get the geometry of the identified feature as a polyline, and remove the z component
                  Polyline polyline = (Polyline) GeometryEngine.removeZ(identifiedFeature.getGeometry());

                  // compute how far the clicked location is along the edge feature
                  utilityElement.setFractionAlongEdge(GeometryEngine.fractionAlong(polyline, mapPoint, -1));

                  // update the status label text
                  statusLabel.setText("Fraction along edge: " + utilityElement.getFractionAlongEdge());
                }

                if (utilityElement != null) {
                  // create a graphic for the new utility element
                  Graphic traceLocationGraphic = new Graphic();

                  // find the closest coordinate on the selected element to the clicked point
                  ProximityResult proximityResult =
                    GeometryEngine.nearestCoordinate(identifiedFeature.getGeometry(), mapPoint);

                  // set the graphic's geometry to the coordinate on the element
                  traceLocationGraphic.setGeometry(proximityResult.getCoordinate());
                  graphicsOverlay.getGraphics().add(traceLocationGraphic);

                  // add the element to the appropriate list, and add the appropriate symbol to the graphic
                  if (isAddingStartingPoint) {
                    startingLocations.add(utilityElement);
                    traceLocationGraphic.setSymbol(startingPointSymbol);
                  } else {
                    barriers.add(utilityElement);
                    traceLocationGraphic.setSymbol(barrierPointSymbol);
                  }
                }
              }
            }
          }
        } catch (InterruptedException | ExecutionException ex) {
          statusLabel.setText("Error identifying clicked features.");
          new Alert(Alert.AlertType.ERROR, "Error identifying clicked features.").show();
        } finally {
          progressIndicator.setVisible(false);
        }
      });
    }
  }

  /**
   * Uses a UtilityNetworkSource to create a UtilityElement object out of an ArcGISFeature.
   *
   * @param identifiedFeature an ArcGISFeature object that will be used to create a UtilityElement
   * @param networkSource the UtilityNetworkSource to which the created UtilityElement is associated
   * @return the created UtilityElement
   */
  private UtilityElement createUtilityElement(ArcGISFeature identifiedFeature, UtilityNetworkSource networkSource) {
    UtilityElement utilityElement = null;

    // get the attributes of the identified feature
    Map<String, Object> attributes = identifiedFeature.getAttributes();

    // get the name of the utility asset group's attribute field from the feature
    String assetGroupFieldName = identifiedFeature.getFeatureTable().getSubtypeField();

    // find the code matching the asset group name in the feature's attributes
    int assetGroupCode = (int) attributes.get(assetGroupFieldName.toLowerCase());

    // iterate through the network source's asset groups to find the group with the matching code
    List<UtilityAssetGroup> assetGroups = networkSource.getAssetGroups();
    for (UtilityAssetGroup assetGroup : assetGroups) {
      if (assetGroup.getCode() == assetGroupCode) {

        // get the code for the feature's asset type from its attributes
        String assetTypeCode = attributes.get("assettype").toString();

        // iterate through the asset group's asset types to find the type matching the feature's asset type code
        List<UtilityAssetType> utilityAssetTypes = assetGroup.getAssetTypes();
        for (UtilityAssetType assetType : utilityAssetTypes) {
          if (assetType.getCode() == Integer.parseInt(assetTypeCode)) {

            // get the list of terminals for the feature
            List<UtilityTerminal> terminals = assetType.getTerminalConfiguration().getTerminals();

            // if there is only one terminal, use it to create a utility element
            if (terminals.size() == 1) {
              utilityElement = utilityNetwork.createElement(identifiedFeature, terminals.get(0));
              // show the name of the terminal in the status label
              showTerminalNameInStatusLabel(terminals.get(0));

              // if there is more than one terminal, prompt the user to select one
            } else if (terminals.size() > 1) {
              // create a dialog for terminal selection
              UtilityTerminalSelectionDialog utilityTerminalSelectionDialog =
                new UtilityTerminalSelectionDialog(terminals);

              // show the terminal selection dialog and capture the user selection
              Optional<UtilityTerminal> selectedTerminalOptional = utilityTerminalSelectionDialog.showAndWait();

              // use the selected terminal
              if (selectedTerminalOptional.isPresent()) {
                UtilityTerminal selectedTerminal = selectedTerminalOptional.get();
                utilityElement = utilityNetwork.createElement(identifiedFeature, selectedTerminal);
                showTerminalNameInStatusLabel(selectedTerminal);
              }
            }
          }
        }
      }
    }
    return utilityElement;
  }

  /**
   * Shows the name of a UtilityTerminal in the status label in the UI.
   *
   * @param terminal the UtilityTerminal object of which to show the name in the UI
   */
  private void showTerminalNameInStatusLabel(UtilityTerminal terminal) {
    String terminalName = terminal.getName() != null ? terminal.getName() : "default";
    statusLabel.setText("Terminal: " + terminalName);
  }

  private void enableUI() {
  }

  @FXML
  private void handleTraceClick() {

    progressIndicator.setVisible(false);
  }

  @FXML
  private void handleResetClick() {

  }

  /**
   * Stops and releases all resources used in application.
   */
  public void terminate() {

    if (mapView != null) {
      mapView.dispose();
    }
  }
}
