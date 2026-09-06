package com.aichuangzuo.user.modules.wechat.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信消息 XML 解析与生成工具。
 */
public final class WechatXmlUtil {

    private WechatXmlUtil() {
    }

    /**
     * 将 XML 字符串解析为 Map。
     *
     * @param xml XML 字符串
     * @return 字段映射
     */
    public static Map<String, String> parseXml(String xml) {
        Map<String, String> map = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用 DTD，防止 XXE 攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    map.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析微信 XML 消息失败", e);
        }
        return map;
    }

    /**
     * 构造文本消息回复 XML。
     *
     * @param toUser   接收方 openid
     * @param fromUser 公众号 ID
     * @param content  文本内容
     * @return XML 字符串
     */
    public static String buildTextMessage(String toUser, String fromUser, String content) {
        long createTime = System.currentTimeMillis() / 1000;
        return """
                <xml>
                    <ToUserName><![CDATA[%s]]></ToUserName>
                    <FromUserName><![CDATA[%s]]></FromUserName>
                    <CreateTime>%d</CreateTime>
                    <MsgType><![CDATA[text]]></MsgType>
                    <Content><![CDATA[%s]]></Content>
                </xml>
                """.formatted(toUser, fromUser, createTime, escapeCdata(content));
    }

    private static String escapeCdata(String content) {
        return content.replace("]]>", "]]]]><![CDATA[>");
    }
}
