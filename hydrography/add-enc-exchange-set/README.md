# Add ENC exchange set

Display nautical charts conforming to the ENC specification.

![](AddEncExchangeSet.png)

## Use case

Maritime applications require conformity to strict specifications over how hydrographic data is displayed digitally to ensure the safety of traveling vessels.

S-5. is the IHO (International Hydrographic Organization) Transfer Standard for digital hydrographic data. The symbology standard for this is called S-52. There are different product specifications for this standard. ENC (Electronic Navigational Charts) is one such specification developed by IHO.

An ENC exchange set is a catalog of data files which can be loaded as cells. The cells contain information on how symbols should be displayed in relation to one another, so as to represent information such as depth and obstacles accurately.

## How it works

1. Specify the path to a local CATALOG.03. file to create an `EncExchangeSet`.
2. After loading the exchange set, loop through the `EncDataset` objects in `encExchangeSet.getDatasets()`.
3. Create an `EncCell` for each dataset. Then create an `EncLayer` for each cell.
4. Add the ENC layer to a map's operational layers collection to display it.

## Relevant API

* EncCell
* EncDataset
* EncExchangeSet
* EncLayer

## Tags

Data, ENC, maritime, nautical chart, layers, hydrographic
