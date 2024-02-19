package com.test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CRLHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author Andy
 */
public class PdfboxSignManager implements SignatureInterface {

    /**
     * 私钥
     */
    private PrivateKey privateKey;
    /**
     * 证书链
     */
    private Certificate[] certChain;
    /**
     * 摘要算法名称
     */
    private String signatureAlgorithm;


    public PdfboxSignManager(SignatureInfo signatureInfo) {
        try {
            // 密码
            char[] password = signatureInfo.getPassword().toCharArray();
            // 得到证书链
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(signatureInfo.getCertificateInputStream(), password);
            String certAlias = keyStore.aliases().nextElement();
            certChain = keyStore.getCertificateChain(certAlias);
            // 私钥
            privateKey = (PrivateKey) keyStore.getKey(certAlias, password);

            signatureAlgorithm = signatureInfo.getSignatureAlgorithm();
        } catch (KeyStoreException e) {
            throw new RuntimeException("pdf签章密码错误！", e);
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (CertificateException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给pdf签章
     * @param signatureInfo
     * @return
     * @throws IOException
     */
    public ByteArrayOutputStream signPDF(SignatureInfo signatureInfo) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        InputStream pdfInputStream = signatureInfo.getSignPdfInputStream();
        SignatureAppearance signatureAppearance = signatureInfo.getSignatureAppearance();
        if (pdfInputStream == null) {
            throw new RuntimeException("找不到pdf文件");
        }
        // 加载pdf
        PDDocument doc = PDDocument.load(pdfInputStream);
        // 创建 签章dictionary
        PDSignature signature = new PDSignature();
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        // 签章附加信息
        signature.setName(signatureAppearance.getContact());
        signature.setLocation(signatureAppearance.getLocation());
        signature.setReason(signatureAppearance.getReason());
        // 设置签名时间
        signature.setSignDate(Calendar.getInstance());

        // 注册签名字典和签名接口
        SignatureOptions sigOpts;
        if (signatureAppearance.isVisibleSignature()) {
            sigOpts = createVisibleSignature(doc, signatureInfo);
        } else {
            sigOpts = new SignatureOptions();
        }
        sigOpts.setPreferredSignatureSize(100000);

        doc.addSignature(signature, this, sigOpts);
        // 输出
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        doc.saveIncremental(byteArrayOutputStream);
        sigOpts.close();
        return byteArrayOutputStream;
    }

    private SignatureOptions createVisibleSignature(PDDocument doc, SignatureInfo signatureInfo) throws IOException {
        SignatureAppearance signatureAppearance = signatureInfo.getSignatureAppearance();
        SignatureOptions sigOpts = new SignatureOptions();
        PDVisibleSignDesigner visibleSig = new PDVisibleSignDesigner(doc, signatureInfo.getImageInputStream(), 1);

        visibleSig.xAxis(signatureInfo.getRectllX())
                .yAxis(signatureInfo.getRectllY())
                .width(signatureInfo.getImageWidth())
                .height(signatureInfo.getImageHight()).signatureFieldName("signature");

        // 设置印章附加信息
        PDVisibleSigProperties signatureProperties = new PDVisibleSigProperties();
        signatureProperties
                .preferredSize(0).page(1).visualSignEnabled(true)
                .setPdVisibleSignature(visibleSig).buildSignature();

        sigOpts.setVisualSignature(signatureProperties);
        sigOpts.setPage(signatureInfo.getPageNo() - 1);
        return sigOpts;
    }

    @Override
    public byte[] sign(InputStream content) {
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        List<Certificate> certList = Arrays.asList(certChain);
        try {
            // SHA1 SHA256 SH384 SHA512
            X509Certificate signingCert = (X509Certificate) certList.get(0);
            gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC")
                    .setSignedAttributeGenerator(new AttributeTable(new Hashtable<>()))
                    .build(signatureAlgorithm, privateKey, signingCert));

            gen.addCertificates(new JcaCertStore(certList));

            X509CRL[] crls = fetchCRLs(signingCert);
            for (X509CRL crl : crls) {
                gen.addCRL(new JcaX509CRLHolder(crl));
            }

            CMSProcessableByteArray processable = new CMSProcessableByteArray(IOUtils.toByteArray(content));

            CMSSignedData signedData = gen.generate(processable, false);

            return signedData.getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Problem while preparing signature");
        }

    }

    private X509CRL[] fetchCRLs(X509Certificate signingCert)
            throws CertificateException, MalformedURLException, CRLException, IOException {
        List<String> crlList = CRLDistributionPointsExtractor.getCrlDistributionPoints(signingCert);
        List<X509CRL> crls = new ArrayList<X509CRL>();
        for (String crlUrl : crlList) {
            if (!crlUrl.startsWith("http")) {
                continue;
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            URL url = new URL(crlUrl);
            X509CRL crl = (X509CRL) cf.generateCRL(url.openStream());
            crls.add(crl);
        }
        return crls.toArray(new X509CRL[] {});
    }

}