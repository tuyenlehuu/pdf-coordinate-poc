package com.poc.pdfcoordinates.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.poc.pdfcoordinates.api.CoordinateSearchResponse;
import com.poc.pdfcoordinates.api.MatchMode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfCoordinateServiceTest {

    private final PdfCoordinateService service = new PdfCoordinateService();

    @Test
    void returnsCoordinatesForEachCaseInsensitiveTextMatch() throws Exception {
        byte[] pdf = createPdf("Invoice Number: INV-001");

        CoordinateSearchResponse response = service.findCoordinates(
                new ByteArrayInputStream(pdf),
                "invoice number",
                MatchMode.CONTAINS,
                false
        );

        assertThat(response.totalMatches()).isEqualTo(1);
        assertThat(response.matches().getFirst().page()).isEqualTo(1);
        assertThat(response.matches().getFirst().text()).isEqualTo("Invoice Number");
        assertThat(response.matches().getFirst().x()).isGreaterThan(0);
        assertThat(response.matches().getFirst().y()).isGreaterThan(0);
        assertThat(response.matches().getFirst().width()).isGreaterThan(0);
        assertThat(response.matches().getFirst().height()).isGreaterThan(0);
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            try (PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0))) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
