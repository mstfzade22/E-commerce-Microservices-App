package com.ecommerce.paymentservice.client;

import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankOrderResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankRefundResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankStatusResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
@Slf4j
public class KapitalBankClient {

    @Value("${kapitalbank.api-url}")
    private String apiUrl;

    @Value("${kapitalbank.merchant-id}")
    private String merchantId;

    @Value("${kapitalbank.currency}")
    private String currency;

    @Value("${kapitalbank.language}")
    private String language;

    @Value("${kapitalbank.cert-path}")
    private String certPath;

    @Value("${kapitalbank.key-path}")
    private String keyPath;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        try {
            SSLContext sslContext = buildSslContext();
            this.httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
            log.info("KapitalBankClient initialized with SSL context");
        } catch (Exception e) {
            log.warn("Failed to initialize SSL context, using default HttpClient: {}", e.getMessage());
            this.httpClient = HttpClient.newHttpClient();
        }
    }

    public KapitalBankOrderResponse createOrder(BigDecimal amount, String description,
                                                 String approveUrl, String cancelUrl, String declineUrl) {
        long amountInMinorUnits = amount.multiply(BigDecimal.valueOf(100)).longValue();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TKKPG>
                  <Request>
                    <Operation>CreateOrder</Operation>
                    <Language>%s</Language>
                    <Order>
                      <OrderType>Purchase</OrderType>
                      <Merchant>%s</Merchant>
                      <Amount>%d</Amount>
                      <Currency>%s</Currency>
                      <Description>%s</Description>
                      <ApproveURL>%s</ApproveURL>
                      <CancelURL>%s</CancelURL>
                      <DeclineURL>%s</DeclineURL>
                    </Order>
                  </Request>
                </TKKPG>
                """.formatted(language, merchantId, amountInMinorUnits, currency,
                escapeXml(description), approveUrl, cancelUrl, declineUrl);

        Document doc = sendRequest(xml);
        Element response = getElement(doc.getDocumentElement(), "Response");
        String status = getTextContent(response, "Status");
        Element order = getElement(response, "Order");

        return new KapitalBankOrderResponse(
                status,
                getTextContent(order, "OrderID"),
                getTextContent(order, "SessionID"),
                getTextContent(order, "URL")
        );
    }

    public KapitalBankStatusResponse getOrderStatus(String orderId, String sessionId) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TKKPG>
                  <Request>
                    <Operation>GetOrderStatus</Operation>
                    <Language>%s</Language>
                    <Order>
                      <Merchant>%s</Merchant>
                      <OrderID>%s</OrderID>
                    </Order>
                    <SessionID>%s</SessionID>
                  </Request>
                </TKKPG>
                """.formatted(language, merchantId, orderId, sessionId);

        Document doc = sendRequest(xml);
        Element response = getElement(doc.getDocumentElement(), "Response");
        String status = getTextContent(response, "Status");
        Element order = getElement(response, "Order");

        return new KapitalBankStatusResponse(
                status,
                getTextContent(order, "OrderStatus"),
                getTextContent(order, "OrderID"),
                sessionId
        );
    }

    public KapitalBankRefundResponse reverseOrder(String orderId, String sessionId, String description) {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <TKKPG>
                  <Request>
                    <Operation>Reverse</Operation>
                    <Language>%s</Language>
                    <Order>
                      <Merchant>%s</Merchant>
                      <OrderID>%s</OrderID>
                    </Order>
                    <SessionID>%s</SessionID>
                    <Description>%s</Description>
                  </Request>
                </TKKPG>
                """.formatted(language, merchantId, orderId, sessionId, escapeXml(description));

        Document doc = sendRequest(xml);
        Element response = getElement(doc.getDocumentElement(), "Response");
        String status = getTextContent(response, "Status");

        return new KapitalBankRefundResponse(status, orderId);
    }

    private Document sendRequest(String xml) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Kapital Bank response status: {}", response.statusCode());

            return parseXml(response.body());
        } catch (Exception e) {
            log.error("Failed to communicate with Kapital Bank: {} - {}", e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Kapital Bank communication error: " + e.getClass().getName() + " - " + e.getMessage(), e);
        }
    }

    private SSLContext buildSslContext() throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(Files.readAllBytes(Path.of(certPath))));

        String keyContent = Files.readString(Path.of(keyPath))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("merchant", privateKey, new char[0], new java.security.cert.Certificate[]{certificate});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Element getElement(Element parent, String tagName) {
        return (Element) parent.getElementsByTagName(tagName).item(0);
    }

    private String getTextContent(Element parent, String tagName) {
        var nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) return null;
        return nodeList.item(0).getTextContent();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
