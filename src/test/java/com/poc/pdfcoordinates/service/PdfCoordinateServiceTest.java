package com.poc.pdfcoordinates.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poc.pdfcoordinates.api.CoordinateSearchResponse;
import com.poc.pdfcoordinates.api.MatchMode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PdfCoordinateServiceTest {

    private final PdfCoordinateService service = new PdfCoordinateService();

    @ParameterizedTest
    @EnumSource(MatchMode.class)
    void returnsBottomRightOfLastPageWhenMarkerIsMissing(MatchMode mode) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.addPage(new PDPage(new PDRectangle(400, 600)));
            document.save(output);

            CoordinateSearchResponse response = service.findCoordinates(
                    new ByteArrayInputStream(output.toByteArray()), "SIGN_HERE", mode, false, 120, 40
            );

            assertThat(response.totalMatches()).isEqualTo(1);
            assertThat(response.matches()).hasSize(1);
            var match = response.matches().getFirst();
            assertThat(match.page()).isEqualTo(2);
            assertThat(match.text()).isEmpty();
            assertThat(match.x()).isEqualTo(280);
            assertThat(match.y()).isZero();
            assertThat(match.topLeftX()).isEqualTo(280);
            assertThat(match.topLeftY()).isEqualTo(560);
            assertThat(match.width()).isEqualTo(120);
            assertThat(match.height()).isEqualTo(40);
        }
    }

    @Test
    void usesDefaultDimensionsWhenMarkerIsMissing() throws Exception {
        var response = service.findCoordinates(
                new ByteArrayInputStream(createPdf("Invoice")), "SIGN_HERE", MatchMode.CONTAINS, false
        );
        var match = response.matches().getFirst();
        assertThat(match.page()).isEqualTo(1);
        assertThat(match.x()).isEqualTo(PDRectangle.LETTER.getWidth() - 150);
        assertThat(match.y()).isZero();
        assertThat(match.topLeftY()).isEqualTo(PDRectangle.LETTER.getHeight() - 50);
        assertThat(match.width()).isEqualTo(150);
        assertThat(match.height()).isEqualTo(50);
    }

    @Test
    void rejectsFallbackBoxLargerThanLastPage() throws Exception {
        byte[] pdf = createPdf("Invoice");
        assertThatThrownBy(() -> service.findCoordinates(
                new ByteArrayInputStream(pdf), "SIGN_HERE", MatchMode.CONTAINS, false, 1000, 40
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fit within the last page");
    }

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
