package org.schabi.newpipe.extractor.services.youtube;

import javax.annotation.Nullable;

/**
 * Supplies visitorData from the host application's active YouTube session.
 *
 * <p>This lets embedded applications reuse the visitor identifier already exposed by their
 * WebView/ytcfg instead of creating a second guest session through the visitor_id endpoint.</p>
 */
@FunctionalInterface
public interface YoutubeVisitorDataProvider {
    @Nullable
    String getVisitorData();
}
