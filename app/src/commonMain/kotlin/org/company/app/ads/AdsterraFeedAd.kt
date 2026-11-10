package org.company.app.ads

import androidx.compose.runtime.Composable

/**
 * Renders one of the configured Adsterra feed placements on supported platforms.
 * unitNumber 1 and 2 alternate in the home feed.
 */
@Composable
expect fun AdsterraFeedAd(unitNumber: Int)
