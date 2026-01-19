package com.ssg9th2team.geharbang.domain.holiday.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.holiday.dto.HolidayItemResponse;
import com.ssg9th2team.geharbang.domain.holiday.exception.HolidayConfigException;
import com.ssg9th2team.geharbang.domain.holiday.exception.HolidayUpstreamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HolidayServiceImpl implements HolidayService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_ROWS = 50;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${holiday.api-base-url:https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService}")
    private String apiBaseUrl;

    @Value("${holiday.service-key:}")
    private String serviceKey;

    @Override
    public List<HolidayItemResponse> getHolidays(int year, Integer month) {
        validateRequest(year, month);
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new HolidayConfigException("Holiday service key is not configured.");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .path("/getRestDeInfo")
                .queryParam("ServiceKey", serviceKey)
                .queryParam("solYear", String.format("%04d", year))
                .queryParam("numOfRows", MAX_ROWS)
                .queryParam("pageNo", 1);
        if (month != null) {
            uriBuilder.queryParam("solMonth", String.format("%02d", month));
        }

        String url = buildHolidayUrl(uriBuilder);

        try {
            byte[] responseBytes = restTemplate.getForObject(url, byte[].class);
            return parseHolidayResponse(responseBytes);
        } catch (Exception ex) {
            int status = resolveUpstreamStatus(ex);
            log.warn("Holiday API request failed. status={}", status);
            throw new HolidayUpstreamException(status, "Holiday API request failed.", ex);
        }
    }

    private void validateRequest(int year, Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("year must be between 1900 and 2100");
        }
    }

    private List<HolidayItemResponse> parseHolidayResponse(byte[] responseBytes) {
        if (responseBytes == null || responseBytes.length == 0) {
            return List.of();
        }

        Character marker = findFirstNonWhitespace(responseBytes);
        if (marker == null) {
            return List.of();
        }
        if (marker == '<') {
            return parseHolidayXml(responseBytes);
        }
        if (marker == '{' || marker == '[') {
            return parseHolidayJson(responseBytes);
        }
        log.warn("Holiday API response format not recognized. snippet={}", buildSnippet(responseBytes));
        throw new HolidayUpstreamException(502, "Holiday API response format not recognized.");
    }

    private List<HolidayItemResponse> parseHolidayXml(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlBytes)) {
                var document = builder.parse(inputStream);
                String resultCode = getDocumentTagText(document, "resultCode");
                String resultMsg = getDocumentTagText(document, "resultMsg");
                if (resultCode == null || resultCode.isBlank()) {
                    throw new HolidayUpstreamException(502, "Holiday API response missing resultCode.");
                }
                if (!"00".equals(resultCode.trim())) {
                    String message = resultMsg != null ? resultMsg.trim() : "Unknown error";
                    log.warn("Holiday API error resultCode={} resultMsg={}", resultCode.trim(), message);
                    if (isNoDataCode(resultCode) || isNoDataMessage(message)) {
                        return List.of();
                    }
                    if (isInvalidServiceKeyMessage(message)) {
                        throw new HolidayConfigException("Holiday service key is invalid.");
                    }
                    throw new HolidayUpstreamException(502, "Holiday API error: " + resultCode + " " + message);
                }

                NodeList items = document.getElementsByTagName("item");

                List<HolidayItemResponse> results = new ArrayList<>();
                for (int i = 0; i < items.getLength(); i++) {
                    Node itemNode = items.item(i);
                    if (!(itemNode instanceof Element item)) {
                        continue;
                    }

                    String locdate = getChildText(item, "locdate");
                    if (locdate == null || locdate.isBlank()) {
                        continue;
                    }

                    LocalDate date = LocalDate.parse(locdate.trim(), DATE_FORMAT);
                    String name = getChildText(item, "dateName");
                    String holidayFlag = getChildText(item, "isHoliday");
                    boolean isHoliday = "Y".equalsIgnoreCase(holidayFlag);

                    results.add(HolidayItemResponse.builder()
                            .date(date.toString())
                            .name(name)
                            .isHoliday(isHoliday)
                            .build());
                }

                return results;
            }
        } catch (HolidayUpstreamException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse holiday response", ex);
            throw new HolidayUpstreamException(502, "Holiday API response parsing failed.", ex);
        }
    }

    private String getChildText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node != null ? node.getTextContent() : null;
    }

    private String getDocumentTagText(org.w3c.dom.Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node != null ? node.getTextContent() : null;
    }

    private List<HolidayItemResponse> parseHolidayJson(byte[] jsonBytes) {
        try {
            JsonNode root = objectMapper.readTree(jsonBytes);
            JsonNode response = root.path("response");
            JsonNode header = response.path("header");
            String resultCode = getJsonText(header, "resultCode");
            String resultMsg = getJsonText(header, "resultMsg");
            if (resultCode == null || resultCode.isBlank()) {
                throw new HolidayUpstreamException(502, "Holiday API response missing resultCode.");
            }
            if (!"00".equals(resultCode.trim())) {
                String message = resultMsg != null ? resultMsg.trim() : "Unknown error";
                log.warn("Holiday API error resultCode={} resultMsg={}", resultCode.trim(), message);
                if (isNoDataCode(resultCode) || isNoDataMessage(message)) {
                    return List.of();
                }
                if (isInvalidServiceKeyMessage(message)) {
                    throw new HolidayConfigException("Holiday service key is invalid.");
                }
                throw new HolidayUpstreamException(502, "Holiday API error: " + resultCode + " " + message);
            }

            JsonNode itemsNode = response.path("body").path("items").path("item");
            List<HolidayItemResponse> results = new ArrayList<>();
            if (itemsNode.isMissingNode() || itemsNode.isNull()) {
                return results;
            }

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    appendHolidayFromJson(results, item);
                }
            } else if (itemsNode.isObject()) {
                appendHolidayFromJson(results, itemsNode);
            }

            return results;
        } catch (HolidayUpstreamException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse holiday response", ex);
            throw new HolidayUpstreamException(502, "Holiday API response parsing failed.", ex);
        }
    }

    private void appendHolidayFromJson(List<HolidayItemResponse> results, JsonNode item) {
        String locdate = getJsonText(item, "locdate");
        if (locdate == null || locdate.isBlank()) {
            return;
        }
        String dateName = getJsonText(item, "dateName");
        String holidayFlag = getJsonText(item, "isHoliday");
        if (holidayFlag == null || holidayFlag.isBlank()) {
            holidayFlag = getJsonText(item, "ishHoliday");
        }

        LocalDate date = LocalDate.parse(locdate.trim(), DATE_FORMAT);
        boolean isHoliday = "Y".equalsIgnoreCase(holidayFlag);
        results.add(HolidayItemResponse.builder()
                .date(date.toString())
                .name(dateName)
                .isHoliday(isHoliday)
                .build());
    }

    private String getJsonText(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private String buildHolidayUrl(UriComponentsBuilder uriBuilder) {
        if (isLikelyEncodedKey(serviceKey)) {
            return uriBuilder.build(true).toUriString();
        }
        return uriBuilder.build().encode().toUriString();
    }

    private boolean isLikelyEncodedKey(String key) {
        if (key == null) {
            return false;
        }
        String upper = key.toUpperCase();
        return upper.contains("%2B") || upper.contains("%2F") || upper.contains("%3D");
    }

    private String buildSnippet(byte[] bytes) {
        int length = Math.min(bytes.length, 200);
        String snippet = new String(bytes, 0, length, StandardCharsets.UTF_8);
        return snippet.replaceAll("\\s+", " ").trim();
    }

    private Character findFirstNonWhitespace(byte[] bytes) {
        for (byte value : bytes) {
            char candidate = (char) value;
            if (!Character.isWhitespace(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isNoDataMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.replaceAll("\\s+", "").toLowerCase();
        return normalized.contains("nodata") || normalized.contains("no_data")
                || normalized.contains("데이터없음") || normalized.contains("데이터없")
                || normalized.contains("데이터가없") || normalized.contains("없음");
    }

    private boolean isNoDataCode(String resultCode) {
        if (resultCode == null) {
            return false;
        }
        return "03".equals(resultCode.trim());
    }

    private boolean isInvalidServiceKeyMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.replaceAll("\\s+", "").toLowerCase();
        return normalized.contains("서비스키") || normalized.contains("servicekey") || normalized.contains("service_key");
    }

    private int resolveUpstreamStatus(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            return 504;
        }
        return 502;
    }
}
