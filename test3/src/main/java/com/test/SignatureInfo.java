package com.test;

import lombok.Data;

import java.io.InputStream;

/**
 * @author Andy
 */
@Data
public class SignatureInfo {
    /**
     * 需要签章的pdf文件输入流
     */
    private InputStream signPdfInputStream;
    /**
     * 证书输入流
     */
    private InputStream certificateInputStream;
    /**
     * 证书密码
     */
    private String password;
    /**
     * 印章附加信息（理由、地址等...）
     */
    private SignatureAppearance signatureAppearance;
    /**
     * 印章图片输入流
     */
    private InputStream imageInputStream;
    /**
     * 图章x坐标
     */
    private float rectllX;
    /**
     * 图章Y坐标
     */
    private float rectllY;
    /**
     * 印章宽度
     */
    private float imageWidth;
    /**
     * 印章高度
     */
    private float imageHight;

    /**
     * 在第几页签名
     */
    private Integer pageNo = 1;
    /**
     * 摘要算法名称
     */
    private String signatureAlgorithm;


}