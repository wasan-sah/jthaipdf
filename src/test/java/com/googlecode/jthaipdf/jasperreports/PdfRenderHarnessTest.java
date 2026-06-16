package com.googlecode.jthaipdf.jasperreports;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import com.googlecode.jthaipdf.jasperreports.engine.ThaiExporterManager;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.base.JRBasePrintPage;
import net.sf.jasperreports.engine.base.JRBasePrintText;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;

/**
 * Manual visual harness (not a strict assertion test): it builds a small
 * {@link JasperPrint} programmatically with hard Thai cases and exports it to
 * PDF two ways so you can open the files and eyeball the glyph stacking.
 * Outputs land in {@code target/}.
 *
 * <ul>
 *   <li>{@code target/thai-native.pdf} -- stock {@link JRPdfExporter} with the
 *       built-in glyph renderer ({@code glyph.renderer.blocks.x=thai}). Renders
 *       correctly with the bundled modern TH Sarabun New font.</li>
 *   <li>{@code target/thai-library.pdf} -- this library's
 *       {@link ThaiExporterManager}, which rearranges marks to Private Use Area
 *       codepoints. Renders correctly with any font that ships the legacy Thai
 *       PUA glyph set -- which TH Sarabun New does, so this output matches the
 *       native one. A purely modern TTF without PUA glyphs would show blank marks.</li>
 * </ul>
 *
 * The font is registered for embedding via
 * {@code src/test/resources/jasperreports_extension.properties}.
 */
public class PdfRenderHarnessTest {

    private static final String FONT = "TH Sarabun New";
    private static final String GLYPH_PROP = "net.sf.jasperreports.export.pdf.glyph.renderer.blocks.x";

    /**
     * Hard/edge/rare cases, each line targeting a specific branch of the glyph
     * rearrangement in {@code ThaiDisplayUtils}. Latin labels are kept so the
     * rendered PDF is self-describing.
     */
    private static final String[] LINES = {
        // normal everyday / document text (no unusual stacking)
        "normal: สวัสดีครับ ยินดีต้อนรับสู่ประเทศไทย",
        "normal: รายงานการประชุมประจำเดือน วันที่ ๑๖ มิถุนายน",
        "invoice: ยอดรวม 1,250.50 บาท ขอบคุณที่ใช้บริการ",
        // upper vowel + tone, and lower vowel + tone (pull-down)
        "vowel+tone: ที่ ปี่ ผู้ สู้ ขึ้น เป็น",
        // up-tail consonants (ป ฝ ฟ ฬ): upper marks must shift left / pull-down-and-shift
        "up-tail: ปั๊ม ฝั่ง ฟื้นฟู ปื้น จุฬา",
        // down-tail consonants + lower vowel: cut tail (ฐ ญ) vs pull-down vowel (ฎ ฏ)
        "tail-cut: ฐุ ญุ กตัญญู  | down-tail: ฎุ ฏุ",
        // SARA_AM explosion (ำ -> NIKHAHIT + SARA_AA), including over a tone mark
        "sara-am: น้ำ ค่ำ ทำ จำนำ ส้มตำ",
        // Pali/Sanskrit PHINTHU (below-base virama) — rare clusters
        "PHINTHU: นโม พุทฺธ ธมฺม สงฺฆ พฺราหฺมณ",
        // THANTHAKHAT (karan / silent) and rare punctuation
        "karan: จันทร์ ศุกร์ สิทธิ์ เล่ห์ ๚๛",
        // RU / LU vowels (ฤ ฤๅ ฦ ฦๅ), incl. archaic ฦ
        "RU/LU: ฤๅ ฤกษ์ หฤทัย ฦๅ ปฤษฎางค์",
        // mixed scripts, Thai + Arabic digits + punctuation
        "mixed: PDF ที่ ๒๕๖๙ 100% — (ทดสอบ)",
    };

    @Test
    public void renderNativeGlyphRenderer() throws Exception {
        JasperPrint print = buildPrint();
        print.setProperty(GLYPH_PROP, "thai"); // shape Thai via AWT; no PUA font needed

        File out = outFile("thai-native.pdf");
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        exporter.exportReport();

        System.out.println("[native]  wrote " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
        assertTrue(out.length() > 5000, "native PDF should embed the font and text");
    }

    @Test
    public void renderJthaipdfLibrary() throws Exception {
        JasperPrint print = buildPrint(); // no glyph property -> library rearranges to PUA

        File out = outFile("thai-library.pdf");
        try (OutputStream os = new FileOutputStream(out)) {
            ThaiExporterManager.exportReportToPdfStream(print, os);
        }

        System.out.println("[library] wrote " + out.getAbsolutePath() + " (" + out.length() + " bytes)"
            + " -- needs a font with the legacy Thai PUA glyphs (TH Sarabun New has them)");
        assertTrue(out.length() > 5000, "library PDF should embed the font and text");
    }

    private JasperPrint buildPrint() {
        JasperPrint print = new JasperPrint();
        print.setName("thai-sample");
        print.setPageWidth(595);  // A4 portrait (points)
        print.setPageHeight(842);

        JRBasePrintPage page = new JRBasePrintPage();
        int y = 50;
        for (String line : LINES) {
            JRBasePrintText text = new JRBasePrintText(print.getDefaultStyleProvider());
            text.setX(40);
            text.setY(y);
            text.setWidth(515);
            text.setHeight(40);
            text.setFontName(FONT);
            text.setFontSize(26f);
            text.setPdfEmbedded(true);
            text.setPdfEncoding("Identity-H");
            text.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
            text.setTextHeight(32f); // required: the PDF exporter draws nothing if textHeight is 0
            text.setText(line);
            page.addElement(text);
            y += 50;
        }
        print.addPage(page);
        return print;
    }

    private File outFile(String name) {
        File dir = new File("target");
        dir.mkdirs();
        return new File(dir, name);
    }
}
