package com.test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Test2 {
    public static void main(String[] args) throws IOException {
        Document document = Jsoup.parse("<div>abc</div>", "UTF-8");
        document.outputSettings().syntax(Document.OutputSettings.Syntax.html);
        OutputStream os = new FileOutputStream("D:/test123.pdf");
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.toStream(os);
        builder.withW3cDocument(new W3CDom().fromJsoup(document), "D:/");

        //引入指定字体，注意字体名需要和css样式中指定的字体名相同
        builder.run();
        os.close();
    }
}
