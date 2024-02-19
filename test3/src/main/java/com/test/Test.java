package com.test;

import java.io.*;

public class Test {
    private static final String PDF_FILE_PATH = "D:\\test123.pdf";
    public static void main(String[] args) throws Exception {
        // 初始化
        SignatureInfo signInfo = initSignatureInfo();

        PdfboxSignManager pdfboxSign = new PdfboxSignManager(signInfo);

        ByteArrayOutputStream outputStream = pdfboxSign.signPDF(signInfo);

        File outputFile = new File("D:\\test321.pdf");
        FileOutputStream fos = new FileOutputStream(outputFile);
        outputStream.writeTo(fos);
        fos.close();
    }

    /**
     * 初始化签章信息
     * @return
     */
    private static SignatureInfo initSignatureInfo() throws Exception {
        SignatureInfo signInfo = new SignatureInfo();
        signInfo.setCertificateInputStream(new FileInputStream(new File("D:\\test.p12")));
        signInfo.setPassword("123456");
        signInfo.setImageInputStream(new FileInputStream(new File("D:\\sign.png")));
        signInfo.setSignPdfInputStream(new FileInputStream(new File(PDF_FILE_PATH)));
        // 签章附加信息
        signInfo.setSignatureAppearance(createSigAppearance());
        signInfo.setPageNo(1);
        signInfo.setRectllX(400);
        signInfo.setRectllY(50);
        signInfo.setImageWidth(100);
        signInfo.setImageHight(100);
        signInfo.setSignatureAlgorithm(SignatureAlgorithm.SHA1);
        return signInfo;
    }


    /**
     * 签章附加信息
     * @return
     */
    private static SignatureAppearance createSigAppearance() {
        SignatureAppearance sigApp = new SignatureAppearance();
        sigApp.setContact("Andy123456");
        sigApp.setLocation("Beijing");
        sigApp.setReason("I am so cool!!!!!!");
        sigApp.setVisibleSignature(true);
        return sigApp;
    }
}
