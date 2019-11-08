# Edit KML ground overlay

Edit the values of a KML ground overlay.

![Edit KML ground overlay](EditKMLGroundOverlay.png)

## Use case

KML ground overlays are used for showing aerial imagery, symbology, or other images draped over a scene. Changing the geometry, rotation, and other attributes of a ground overlay after it has been loaded allows for live editing.  For example, editing the geometry and opacity of a historical image draped over present day satellite imagery makes it possible to view change over time.

## How to use the sample

Use the slider to adjust the opacity of the ground overlay.

## How it works

1. Create an `Envelope` defining the geometry of the overlay.
2. Create a `KmlIcon` using a  URI linking to an image.
3. Create a `KmlGroundOverlay` using the envelope and icon.
4. Create a `KmlDataset` using the ground overlay.
5. Create a `KmlLayer` using the dataset.
6. Add the KML layer to the scene.
7. Use `kmlGroundOverlay.setColor(value)` with a different alpha value to change the opacity.

## Relevant API

* KmlDataset
* KmlGroundOverlay
* KmlIcon
* KmlLayer

## About the data

The [image](https://libapps.s3.amazonaws.com/accounts/55937/images/1944.jpg) is an aerial view of the campus of the University of Oregon. This imagery was taken in 1944 by the U.S. Army Corps of Engineers. It is publicly available on the University of Oregon libraries [website](https://researchguides.uoregon.edu/online-aerial-photography). It is also available on [ArcGIS Online](https://arcgisruntime.maps.arcgis.com/home/item.html?id=1f3677c24b2c446e96eaf1099292e83e).

## Tags

imagery, Keyhole, KML, KMZ, OGC