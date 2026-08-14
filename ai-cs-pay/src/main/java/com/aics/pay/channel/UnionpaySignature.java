package com.aics.pay.channel;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银联全渠道网关签名工具（与银联官方 SDK 规则对齐，纯 JCE 实现，无需第三方依赖）
 *
 * <p>签名规则：
 * <ol>
 *   <li>排除 sign / signMethod 字段</li>
 *   <li>剩余字段按键名升序排列，仅保留非空值</li>
 *   <li>拼接 {@code key=value&key=value...}</li>
 *   <li>使用签名证书私钥做 SHA256withRSA 签名，Base64 编码</li>
 * </ol>
 * 验签使用银联公钥证书，规则相同。
 */
public final class UnionpaySignature {

    private UnionpaySignature() {
    }

    /** 组装待签名串 */
    public static String buildContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !"sign".equals(e.getKey()) && !"signMethod".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /** 签名：返回 Base64 签名串 */
    public static String sign(Map<String, String> params, PrivateKey privateKey, String charset) {
        String content = buildContent(params);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(charset));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("银联签名失败", e);
        }
    }

    /** 验签：成功返回 true */
    public static boolean verify(Map<String, String> params, PublicKey publicKey, String charset) {
        String sign = params.get("sign");
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        String content = buildContent(params);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(content.getBytes(charset));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    /** 加载 PKCS12 签名证书（返回私钥 + 证书 + certId） */
    public static SignCert loadSignCert(String p12Path, String pwd) {
        try (InputStream in = Files.newInputStream(Paths.get(p12Path))) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, pwd.toCharArray());
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, pwd.toCharArray());
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            return new SignCert(privateKey, cert);
        } catch (Exception e) {
            throw new IllegalStateException("加载银联签名证书失败: " + p12Path, e);
        }
    }

    /** 加载银联验签公钥证书 */
    public static PublicKey loadVerifyPublicKey(String cerPath) {
        try (InputStream in = Files.newInputStream(Paths.get(cerPath))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
            return cert.getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("加载银联验签证书失败: " + cerPath, e);
        }
    }

    /** 解析银联 urlencoded 响应（key=value&key=value） */
    public static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = urlDecode(pair.substring(0, idx));
                String value = idx + 1 < pair.length() ? urlDecode(pair.substring(idx + 1)) : "";
                map.put(key, value);
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    /** 银联签名证书信息 */
    public record SignCert(PrivateKey privateKey, X509Certificate certificate) {

        /** certId = 证书序列号（十进制字符串） */
        public String certId() {
            return certificate.getSerialNumber().toString(10);
        }
    }
}