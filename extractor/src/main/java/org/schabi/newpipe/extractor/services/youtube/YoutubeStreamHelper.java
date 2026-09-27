package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonBuilder;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.schabi.newpipe.extractor.NewPipe.getDownloader;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.ANDROID_VR_USER_AGENT;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.MWEB_CLIENT_ID;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.MWEB_CLIENT_VERSION;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.MWEB_USER_AGENT;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.TVHTML5_USER_AGENT;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_ID;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_VERSION;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CPN;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.VIDEO_ID;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.generateTParameter;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getAndroidUserAgent;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getClientHeaders;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getClientVersion;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getIosUserAgent;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getOriginReferrerHeaders;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYouTubeHeaders;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareJsonBuilder;

public final class YoutubeStreamHelper {

    private static final String PLAYER = "player";
    private static final String SERVICE_INTEGRITY_DIMENSIONS = "serviceIntegrityDimensions";
    private static final String PO_TOKEN = "poToken";
    private static final String BASE_YT_DESKTOP_WATCH_URL = "https://www.youtube.com/watch?v=";

    private YoutubeStreamHelper() {
    }

    @Nonnull
    public static JsonObject getWebPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult webPoTokenResult,
            final int signatureTimestamp) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofWebClient();
        innertubeClientRequestInfo.clientInfo.clientVersion = getClientVersion();

        final Map<String, List<String>> headers = getYouTubeHeaders();
        final String watchUrl = BASE_YT_DESKTOP_WATCH_URL + videoId;

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = webPoTokenResult == null
                ? YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL, null, false)
                : webPoTokenResult.visitorData;
        headers.put("X-Goog-Visitor-Id", List.of(innertubeClientRequestInfo.clientInfo.visitorData));

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);
        addPlaybackContext(builder, watchUrl, signatureTimestamp);

        if (webPoTokenResult != null) {
            addPoToken(builder, webPoTokenResult.playerRequestPoToken);
        }

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    @Nonnull
    public static JsonObject getWebMetadataPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofWebClient();
        innertubeClientRequestInfo.clientInfo.clientVersion = getClientVersion();

        final Map<String, List<String>> headers = getYouTubeHeaders();

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData =
                YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL, null, false);

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, null);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&$fields=microformat,videoDetails.thumbnail.thumbnails,videoDetails.videoId";

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(
                        url, headers, body, localization)));
    }

    /**
     * Get the mobile-web player response. MWEB currently exposes direct formats but requires a
     * Web/BotGuard GVS PoToken on those googlevideo URLs. The player request itself does not need
     * the token, so the result is only used here for its visitor/session binding; the streaming
     * token is appended later by {@code YoutubeStreamExtractor}.
     */
    @Nonnull
    public static JsonObject getMWebPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult webPoTokenResult,
            final int signatureTimestamp) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofMWebClient();

        final Map<String, List<String>> headers = new HashMap<>(
                getClientHeaders(MWEB_CLIENT_ID, MWEB_CLIENT_VERSION));
        // MWEB uses www.youtube.com as its Innertube API origin. This is especially
        // important for signed-in sessions because SAPISIDHASH is origin-bound.
        headers.putAll(getOriginReferrerHeaders("https://www.youtube.com"));
        headers.put("User-Agent", List.of(MWEB_USER_AGENT));

        innertubeClientRequestInfo.clientInfo.visitorData = webPoTokenResult == null
                ? YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL,
                        "https://m.youtube.com", false)
                : webPoTokenResult.visitorData;
        headers.put("X-Goog-Visitor-Id", List.of(innertubeClientRequestInfo.clientInfo.visitorData));

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);
        addVideoIdCpnAndOkChecks(builder, videoId, cpn);
        addPlaybackContext(builder, "https://www.youtube.com/watch?v=" + videoId,
                signatureTimestamp);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);
        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER;
        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    @Nonnull
    public static JsonObject getWebEmbeddedPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult webEmbeddedPoTokenResult,
            final int signatureTimestamp) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofWebEmbeddedPlayerClient();

        final Map<String, List<String>> headers = new HashMap<>(
                getClientHeaders(WEB_EMBEDDED_CLIENT_ID, WEB_EMBEDDED_CLIENT_VERSION));
        headers.putAll(getOriginReferrerHeaders("https://www.youtube.com"));

        final String embedUrl = "https://www.youtube.com/embed/" + videoId;

        // 2026-08-19 (aria33): extract encryptedHostFlags from the embed webpage to satisfy
        // YouTube's recent mandatory encryptedHostFlags verification on WEB_EMBEDDED player requests.
        String encryptedHostFlags = null;
        try {
            final String embedPageContent = getDownloader().get(embedUrl, headers, localization).responseBody();
            encryptedHostFlags = org.schabi.newpipe.extractor.utils.Parser.matchGroup1(
                    java.util.regex.Pattern.compile("\"encryptedHostFlags\":\"([^\"]+)\""), embedPageContent);
        } catch (Exception ignored) {
            // fallback
        }

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = webEmbeddedPoTokenResult == null
                ? YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL, embedUrl, false)
                : webEmbeddedPoTokenResult.visitorData;
        headers.put("X-Goog-Visitor-Id", List.of(innertubeClientRequestInfo.clientInfo.visitorData));

        // WEB_EMBEDDED expects thirdParty.embedUrl to identify the external embedding site, not
        // YouTube's own /embed page. A YouTube URL here can yield ERROR: video unavailable.
        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, "https://www.reddit.com/");

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);

        addPlaybackContext(builder, embedUrl, signatureTimestamp, encryptedHostFlags);

        // Current WEB_EMBEDDED does not require a player PoToken. The supplied Web result is
        // used only as a stable visitor/session source; do not cross-apply its player token.

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);
        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    public static JsonObject getAndroidPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nonnull final PoTokenResult androidPoTokenResult)
            throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofAndroidClient();
        innertubeClientRequestInfo.clientInfo.visitorData = androidPoTokenResult.visitorData;

        final Map<String, List<String>> headers =
                getMobileClientHeaders(getAndroidUserAgent(localization));

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);

        addPoToken(builder, androidPoTokenResult.playerRequestPoToken);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    public static JsonObject getAndroidReelPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofAndroidClient();

        final Map<String, List<String>> headers =
                getMobileClientHeaders(getAndroidUserAgent(localization));

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData =
                YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_GAPIS_URL, null, false);

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        builder.object("playerRequest");
        addVideoIdCpnAndOkChecks(builder, videoId, cpn);
        builder.end()
                .value("disablePlayerResponse", false);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_GAPIS_URL + "reel/reel_item_watch" + "?"
                + DISABLE_PRETTY_PRINT_PARAMETER + "&t=" + generateTParameter() + "&id=" + videoId
                + "&$fields=playerResponse";

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)))
                .getObject("playerResponse");
    }

    public static JsonObject getAndroidVrPlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult androidPoTokenResult) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofAndroidVrClient();

        final Map<String, List<String>> headers =
                getMobileClientHeaders(ANDROID_VR_USER_AGENT);

        // 2026-08+: VR streams are also protected by PoToken. When the host app provides one
        // (e.g. a WEB-client mint), attach it so the issued stream URLs carry a server-signed pot=
        // instead of a client-appended one, which googlevideo rejects with HTTP 403.
        innertubeClientRequestInfo.clientInfo.visitorData = androidPoTokenResult == null
                ? YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_GAPIS_URL, null, false)
                : androidPoTokenResult.visitorData;

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);

        if (androidPoTokenResult != null) {
            addPoToken(builder, androidPoTokenResult.playerRequestPoToken);
        }

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&t=" + generateTParameter() + "&id=" + videoId;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    public static JsonObject getIosPlayerResponse(@Nonnull final ContentCountry contentCountry,
                                                  @Nonnull final Localization localization,
                                                  @Nonnull final String videoId,
                                                  @Nonnull final String cpn,
                                                  @Nullable final PoTokenResult iosPoTokenResult)
            throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofIosClient();

        final Map<String, List<String>> headers =
                getMobileClientHeaders(getIosUserAgent(localization));

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = iosPoTokenResult == null
                ? YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL, null, false)
                : iosPoTokenResult.visitorData;

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);

        if (iosPoTokenResult != null) {
            addPoToken(builder, iosPoTokenResult.playerRequestPoToken);
        }

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&t=" + generateTParameter() + "&id=" + videoId;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    /**
     * Gets the player response of a video using the {@code TVHTML5} client.
     *
     * <p>
     * This client is used as a PoToken-free fallback for videos that are unplayable on the
     * {@code ANDROID_VR} or {@code WEB} clients (e.g. age-gated or "made for kids" videos). Like
     * the {@code ANDROID_VR} client, it does not require a JavaScript player or a PoToken.
     * </p>
     *
     * @param contentCountry the content country
     * @param localization   the localization
     * @param videoId        the video id
     * @param cpn            the content playback nonce
     * @return the player response as a {@link JsonObject}
     */
    @Nonnull
    public static JsonObject getTvHtml5PlayerResponse(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String cpn,
            @Nullable final PoTokenResult tvPoTokenResult,
            final int signatureTimestamp) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofTvHtml5Client();

        final Map<String, List<String>> headers = new HashMap<>(
                getClientHeaders(ClientsConstants.TVHTML5_CLIENT_ID,
                        ClientsConstants.TVHTML5_CLIENT_VERSION));
        headers.putAll(getOriginReferrerHeaders("https://www.youtube.com"));
        headers.put("User-Agent", List.of(TVHTML5_USER_AGENT));

        // TVHTML5 itself is PoToken-free. Reuse the already-established Web visitorData when
        // available, rather than asking the TV client /visitor_id endpoint (which returns no ID).
        innertubeClientRequestInfo.clientInfo.visitorData = tvPoTokenResult == null
                ? YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL, null, false)
                : tvPoTokenResult.visitorData;
        headers.put("X-Goog-Visitor-Id", List.of(innertubeClientRequestInfo.clientInfo.visitorData));

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);

        // 2026-08-19: TVHTML5 player requests now enforce signatureTimestamp checks.
        // Failing to provide the correct signatureTimestamp causes the player response to
        // fail with UNPLAYABLE: "The page needs to be reloaded."
        addPlaybackContext(builder, "https://www.youtube.com/", signatureTimestamp);

        // Do not send the Web player PoToken to TVHTML5.

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    private static void addVideoIdCpnAndOkChecks(@Nonnull final JsonBuilder<JsonObject> builder,
                                                 @Nonnull final String videoId,
                                                 @Nullable final String cpn) {
        builder.value(VIDEO_ID, videoId);

        if (cpn != null) {
            builder.value(CPN, cpn);
        }

        builder.value(CONTENT_CHECK_OK, true)
                .value(RACY_CHECK_OK, true);
    }

    private static void addPlaybackContext(@Nonnull final JsonBuilder<JsonObject> builder,
                                           @Nonnull final String referer,
                                           final int signatureTimestamp) {
        builder.object("playbackContext")
                .object("contentPlaybackContext")
                .value("html5Preference", "HTML5_PREF_WANTS")
                .value("signatureTimestamp", signatureTimestamp)
                .value("referer", referer)
                .end()
                .end();
    }

    private static void addPlaybackContext(@Nonnull final JsonBuilder<JsonObject> builder,
                                           @Nonnull final String referer,
                                           final int signatureTimestamp,
                                           @Nullable final String encryptedHostFlags) {
        builder.object("playbackContext")
                .object("contentPlaybackContext")
                .value("html5Preference", "HTML5_PREF_WANTS")
                .value("signatureTimestamp", signatureTimestamp)
                .value("referer", referer);
        if (encryptedHostFlags != null) {
            builder.value("encryptedHostFlags", encryptedHostFlags);
        }
        builder.end()
                .end();
    }

    private static void addPoToken(@Nonnull final JsonBuilder<JsonObject> builder,
                                   @Nonnull final String poToken) {
        builder.object(SERVICE_INTEGRITY_DIMENSIONS)
                .value(PO_TOKEN, poToken)
                .end();
    }

    @Nonnull
    private static Map<String, List<String>> getMobileClientHeaders(
            @Nonnull final String userAgent) {
        return Map.of("User-Agent", List.of(userAgent),
                "X-Goog-Api-Format-Version", List.of("2"));
    }
}
