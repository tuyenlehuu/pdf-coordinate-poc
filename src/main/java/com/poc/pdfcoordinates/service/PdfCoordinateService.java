package com.poc.pdfcoordinates.service;

import com.poc.pdfcoordinates.api.CoordinateMatch;
import com.poc.pdfcoordinates.api.CoordinateSearchResponse;
import com.poc.pdfcoordinates.api.MatchMode;
import com.poc.pdfcoordinates.api.PdfProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

@Service
public class PdfCoordinateService {

    public CoordinateSearchResponse findCoordinates(
            InputStream pdfInput,
            String query,
            MatchMode mode,
            boolean caseSensitive
    ) {
        try (PDDocument document = Loader.loadPDF(pdfInput.readAllBytes())) {
            List<CoordinateMatch> matches = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                matches.addAll(findOnPage(document, pageIndex, query, mode, caseSensitive));
            }
            return new CoordinateSearchResponse(query, mode, matches.size(), matches);
        } catch (IOException exception) {
            throw new PdfProcessingException("The file is not a readable PDF.", exception);
        }
    }

    private List<CoordinateMatch> findOnPage(
            PDDocument document,
            int pageIndex,
            String query,
            MatchMode mode,
            boolean caseSensitive
    ) throws IOException {
        PositionCollectingStripper stripper = new PositionCollectingStripper();
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);
        stripper.getText(document);

        CollectedPageText collectedPageText = collectPageText(stripper.getPositions());
        String pageText = collectedPageText.text();
        List<TextRange> ranges = findRanges(pageText, query, mode, caseSensitive);
        PDPage page = document.getPage(pageIndex);

        return ranges.stream()
                .map(range -> toCoordinateMatch(pageIndex + 1, range, collectedPageText, page))
                .toList();
    }

    private List<TextRange> findRanges(String pageText, String query, MatchMode mode, boolean caseSensitive) {
        return switch (mode) {
            case EXACT -> normalize(pageText, caseSensitive).equals(normalize(query, caseSensitive))
                    ? List.of(new TextRange(0, pageText.length()))
                    : List.of();
            case CONTAINS -> findLiteralRanges(pageText, query, caseSensitive);
            case REGEX -> findRegexRanges(pageText, query, caseSensitive);
        };
    }

    private List<TextRange> findLiteralRanges(String pageText, String query, boolean caseSensitive) {
        String searchableText = normalize(pageText, caseSensitive);
        String searchableQuery = normalize(query, caseSensitive);
        List<TextRange> ranges = new ArrayList<>();
        int index = searchableText.indexOf(searchableQuery);
        while (index >= 0) {
            ranges.add(new TextRange(index, index + searchableQuery.length()));
            index = searchableText.indexOf(searchableQuery, index + Math.max(1, searchableQuery.length()));
        }
        return ranges;
    }

    private List<TextRange> findRegexRanges(String pageText, String query, boolean caseSensitive) {
        try {
            Pattern pattern = Pattern.compile(query, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(pageText);
            List<TextRange> ranges = new ArrayList<>();
            while (matcher.find()) {
                if (!matcher.group().isEmpty()) {
                    ranges.add(new TextRange(matcher.start(), matcher.end()));
                }
            }
            return ranges;
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("Invalid regular expression: " + exception.getDescription());
        }
    }

    private CoordinateMatch toCoordinateMatch(
            int pageNumber,
            TextRange range,
            CollectedPageText collectedPageText,
            PDPage page
    ) {
        List<TextPosition> textPositions = collectedPageText.positions().subList(range.start(), range.end());
        float x = textPositions.stream().map(TextPosition::getXDirAdj).min(Float::compare).orElseThrow();
        float y = textPositions.stream().map(TextPosition::getYDirAdj).min(Float::compare).orElseThrow();
        float right = textPositions.stream()
                .map(position -> position.getXDirAdj() + position.getWidthDirAdj())
                .max(Float::compare)
                .orElseThrow();
        float bottom = textPositions.stream()
                .map(position -> position.getYDirAdj() + position.getHeightDir())
                .max(Float::compare)
                .orElseThrow();
        float pageHeight = page.getMediaBox().getHeight();

        return new CoordinateMatch(
                pageNumber,
                collectedPageText.text().substring(range.start(), range.end()),
                x,
                pageHeight - bottom,
                right - x,
                bottom - y,
                x,
                y
        );
    }

    private String normalize(String value, boolean caseSensitive) {
        return caseSensitive ? value : value.toLowerCase(Locale.ROOT);
    }

    private CollectedPageText collectPageText(List<PageTextPosition> pageTextPositions) {
        StringBuilder text = new StringBuilder();
        List<TextPosition> positions = new ArrayList<>();
        for (PageTextPosition pageTextPosition : pageTextPositions) {
            text.append(pageTextPosition.text());
            for (int index = 0; index < pageTextPosition.text().length(); index++) {
                positions.add(pageTextPosition.position());
            }
        }
        return new CollectedPageText(text.toString(), List.copyOf(positions));
    }

    private record CollectedPageText(String text, List<TextPosition> positions) {
    }

    private record TextRange(int start, int end) {
    }
}
