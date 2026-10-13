package com.poc.pdfcoordinates.api;

import com.poc.pdfcoordinates.service.PdfCoordinateService;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf")
public class PdfCoordinateController {

    private final PdfCoordinateService pdfCoordinateService;

    public PdfCoordinateController(PdfCoordinateService pdfCoordinateService) {
        this.pdfCoordinateService = pdfCoordinateService;
    }

    @PostMapping(value = "/coordinates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CoordinateSearchResponse findCoordinates(
            @RequestParam("file") MultipartFile file,
            @RequestParam("query") String query,
            @RequestParam(defaultValue = "CONTAINS") MatchMode mode,
            @RequestParam(defaultValue = "false") boolean caseSensitive,
            @RequestParam(defaultValue = "150") float width,
            @RequestParam(defaultValue = "50") float height
    ) {
        validatePdf(file, query);
        try {
            return pdfCoordinateService.findCoordinates(file.getInputStream(), query, mode, caseSensitive, width, height);
        } catch (IOException exception) {
            throw new PdfProcessingException("Could not read the uploaded PDF.", exception);
        }
    }

    private void validatePdf(MultipartFile file, String query) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty PDF file is required.");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank.");
        }
    }
}
