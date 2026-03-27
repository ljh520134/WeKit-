package dev.ujhhgtg.wekit.ui.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Check
import com.composables.icons.materialsymbols.outlined.Close
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * A full-screen dialog wrapping an osmdroid [MapView] (OpenStreetMap, FOSS).
 * The user taps anywhere on the map to drop a pin; tapping Confirm fires
 * [onLocationSelected] with the chosen [GeoPoint].
 *
 * @param initialLocation    Camera target on open (defaults to Shanghai).
 * @param initialZoom        Initial zoom level (0–19, defaults to 12).
 * @param tileSource         Tile source to use; swap for a China-accessible CDN.
 * @param onLocationSelected Called with the confirmed [GeoPoint]; dismiss here.
 * @param onDismiss          Called when the user cancels.
 */
@Composable
fun OsmLocationPickerDialogContent(
    initialLocation: GeoPoint = GeoPoint(31.224361, 121.469170), // Shanghai
    initialZoom: Double = 12.0,
    tileSource: ITileSource = TileSourceFactory.MAPNIK,
    onLocationSelected: (GeoPoint) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // osmdroid requires a user-agent to be set before first MapView creation
    SideEffect {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var pickedPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // Hold a stable reference so overlays can be mutated without recomposing
    val markerRef = remember { mutableStateOf<Marker?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.9f),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────
            OsmPickerHeader(
                pickedPoint = pickedPoint,
                onDismiss = onDismiss,
                onConfirm = { pickedPoint?.let(onLocationSelected) },
            )

            HorizontalDivider()

            // ── Map ───────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).clipToBounds()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(tileSource)
                            setMultiTouchControls(true)
                            controller.setZoom(initialZoom)
                            controller.setCenter(initialLocation)
                            minZoomLevel = 3.0
                            maxZoomLevel = 19.0

                            val eventsOverlay = MapEventsOverlay(
                                object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        pickedPoint = p
                                        val existing = markerRef.value
                                        if (existing != null) {
                                            existing.position = p
                                        } else {
                                            val m = Marker(this@apply).apply {
                                                position = p
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                title = "选择位置"
                                            }
                                            overlays.add(m)
                                            markerRef.value = m
                                        }
                                        invalidate()
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint) = false
                                }
                            )
                            overlays.add(0, eventsOverlay)
                        }
                    },
                    update = { mapView ->
                        if (mapView.tileProvider.tileSource != tileSource) {
                            mapView.setTileSource(tileSource)
                        }
                    },
                )

                // Hint chip – hidden once a point is chosen
                if (pickedPoint == null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    ) {
                        Text(
                            text = "点击地图以选择虚拟位置",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                // Coordinate chips – float over map once a point is picked
                pickedPoint?.let { pt ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OsmCoordinateChip(label = "纬度", value = "%.6f".format(pt.latitude))
                        OsmCoordinateChip(label = "经度", value = "%.6f".format(pt.longitude))
                    }
                }
            }
        }
    }
}

// ── Internal composables ──────────────────────────────────────────────────────

@Composable
private fun OsmPickerHeader(
    pickedPoint: GeoPoint?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(MaterialSymbols.Outlined.Close, contentDescription = "Cancel")
        }
        Text(
            text = "地图选点",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onConfirm,
            enabled = pickedPoint != null,
        ) {
            Icon(
                imageVector = MaterialSymbols.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("确定")
        }
    }
}

@Composable
private fun OsmCoordinateChip(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
