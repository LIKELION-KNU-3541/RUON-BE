package com.springboot.ruon.global.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 검색으로 찾은 외부 이미지를 내려받는다.
 * 주소가 우리 통제 밖이라, 내부망 요청(SSRF)과 과도한 응답을 막는 책임이 여기에 있다.
 * 내려받지 못한 경우 예외 대신 빈 값을 돌려준다.
 */
@Component
public class RemoteImageDownloader {

    private static final Logger log = LoggerFactory.getLogger(RemoteImageDownloader.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    //검증 단계의 한도(7MB)보다 조금 크게 잡아, 초과분을 감지해서 버릴 수 있게 한다.
    private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    public RemoteImageDownloader() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                    throws IOException {
                super.prepareConnection(connection, httpMethod);
                //리다이렉트를 따라가면 검사한 주소가 아닌 곳으로 끌려갈 수 있다.
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * 이미지를 내려받아 본문과 Content-Type을 돌려준다.
     * 주소가 안전하지 않거나 응답이 이미지가 아니면 빈 값을 돌려준다.
     */
    public Optional<DownloadedImage> download(String imageUrl) {
        URI uri = safeUri(imageUrl);
        if (uri == null) {
            return Optional.empty();
        }

        try {
            return restClient.get()
                    .uri(uri)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            log.debug("이미지 응답이 정상이 아닙니다: url={}, status={}",
                                    imageUrl, response.getStatusCode());
                            return Optional.<DownloadedImage>empty();
                        }
                        String contentType = Optional.ofNullable(response.getHeaders().getContentType())
                                .map(Object::toString)
                                .orElse(null);
                        return readBody(response.getBody())
                                .map(body -> new DownloadedImage(body, contentType));
                    }, false);
        } catch (Exception e) {
            //대표 이미지는 선택 기능이라 실패해도 스캔을 멈추지 않는다.
            log.debug("이미지를 내려받지 못했습니다: url={}", imageUrl, e);
            return Optional.empty();
        }
    }

    //한도를 넘는지 확인하려고 1바이트 더 읽는다. 넘으면 통째로 버린다.
    private Optional<byte[]> readBody(InputStream body) throws IOException {
        byte[] bytes = body.readNBytes(MAX_DOWNLOAD_BYTES + 1);
        if (bytes.length == 0 || bytes.length > MAX_DOWNLOAD_BYTES) {
            return Optional.empty();
        }
        return Optional.of(bytes);
    }

    /**
     * 외부 주소를 검사한다.
     * 스킴이 http(s)가 아니거나 내부망을 가리키면 요청하지 않는다.
     */
    private URI safeUri(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(imageUrl.strip());
        } catch (IllegalArgumentException e) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            log.debug("허용하지 않는 주소 형식입니다: url={}", imageUrl);
            return null;
        }
        if (uri.getHost() == null || isInternalAddress(uri.getHost())) {
            log.warn("내부망을 가리키는 이미지 주소를 차단했습니다: url={}", imageUrl);
            return null;
        }
        return uri;
    }

    private boolean isInternalAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (UnknownHostException e) {
            //이름을 못 찾으면 어차피 받을 수 없다.
            return true;
        }
    }

    public record DownloadedImage(byte[] body, String contentType) {
    }
}
