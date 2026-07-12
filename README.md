# Local Lines

Local Lines is a free, privacy-minimal Wear OS watch face inspired by large
stacked digital time over a map of nearby streets. It supports Wear OS 4 and
newer.

The project contains two independently installable applications:

- `watchface`: a resource-only Watch Face Format v1 application.
- `mapprovider`: a Wear OS complication provider that renders free
  OpenFreeMap vector tiles with MapLibre Native.

The watch face remains useful without location or network access: it keeps the
last successful live map and includes a decorative fallback.

## Features

- Live, north-up map of nearby roads with no labels.
- Eight single-colour themes plus dark and light multicolour map themes.
- Multicolour maps show blue water, green spaces, hierarchical roads, and a red location marker.
- Residential streets remain visible while driveways, parking aisles, paths, and tracks are omitted.
- Large stacked digital hour and minute, respecting 12/24-hour settings.
- Editable weather and step-count complications.
- Optional curved top location label assembled from any combination of number,
  road, town, city, and country.
- Curved bottom text complication for calendars and other long-text sources.
- Battery Saver, Balanced, and Frequent map refresh modes.
- Fast location labels use free passive fused fixes, a best-effort 100 m exit
  geofence, and active-complication refresh requests rather than a continuous
  location listener. Map images remain protected
  by the selected movement gate, 30-second coalescing, low-battery/network
  constraints, and a periodic fallback.
- Wear OS does not expose an unprivileged callback for every individual wrist
  raise. The platform requests the active complication at most every five
  minutes and reduces requests while ambient or not worn.
- Black, time-only ambient display.
- No accounts, analytics, advertisements, API keys, or paid map service.

## Build

Use JDK 21 and an Android SDK containing API 36:

```sh
./gradlew test lintDebug assembleDebug
```

Debug APKs are written to:

```text
watchface/build/outputs/apk/debug/watchface-debug.apk
mapprovider/build/outputs/apk/debug/mapprovider-debug.apk
```

Install the provider first, open **Local Lines Maps**, grant location access,
and refresh once. Then install/select the Local Lines watch face:

```sh
adb install -r mapprovider/build/outputs/apk/debug/mapprovider-debug.apk
adb install -r watchface/build/outputs/apk/debug/watchface-debug.apk
```

If the background does not select automatically, customize Local Lines and set
the full-screen background complication to **Local Lines live map**. The left
circle defaults to empty because Wear OS does not define a portable system
weather provider; select the weather provider installed on the watch.

## Map data and privacy

The provider uses balanced-power location for scheduled work and requests high
accuracy only for a manual refresh. It stores only the last coordinate,
resolved labels, and rendered bitmap.
The coordinate is used locally to request standard vector tiles from
OpenFreeMap; no location history or user identity is collected.

Location names first use Android's system geocoder when a map refresh is
already required. If it returns no result, the provider makes one reverse
lookup through the public OpenStreetMap Nominatim service. Results are cached
on the watch, and Live mode's movement gate and batching prevent repeated
lookups. This use must remain within the
[Nominatim usage policy](https://operations.osmfoundation.org/policies/nominatim/).
Map rendering uses
[MapLibre Native](https://maplibre.org/) and
[OpenFreeMap](https://openfreemap.org/). Map data is © OpenStreetMap
contributors and © OpenMapTiles. Attribution is embedded in every live map.

OpenFreeMap is a free public service without an SLA. If it is unavailable,
Local Lines continues to display its cached or bundled map.
