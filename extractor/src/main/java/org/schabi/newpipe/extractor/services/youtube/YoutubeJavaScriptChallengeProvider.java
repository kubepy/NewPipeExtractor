package org.schabi.newpipe.extractor.services.youtube;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Host-provided solver for YouTube player JavaScript challenges.
 *
 * <p>The extractor keeps its legacy parser as a fallback, while applications which have a real
 * JavaScript runtime can provide a modern solver for {@code sig} and {@code n} challenges.</p>
 */
public interface YoutubeJavaScriptChallengeProvider {

    /**
     * Solve a batch of challenges from one YouTube player.
     *
     * @param videoId video id used for the player request
     * @param playerJavaScript complete YouTube player JavaScript source
     * @param challengeType {@code sig} or {@code n}
     * @param challenges encrypted challenge strings
     * @return map from each input challenge to its solved value
     * @throws Exception if the host runtime cannot solve the challenge batch
     */
    @Nonnull
    Map<String, String> solve(@Nonnull String videoId,
                              @Nonnull String playerJavaScript,
                              @Nonnull String challengeType,
                              @Nonnull List<String> challenges) throws Exception;
}
