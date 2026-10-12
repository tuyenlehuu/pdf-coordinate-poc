package com.poc.pdfcoordinates.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

final class PositionCollectingStripper extends PDFTextStripper {

    private final List<PageTextPosition> positions = new ArrayList<>();

    PositionCollectingStripper() throws IOException {
        setSortByPosition(true);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        positions.add(new PageTextPosition(text.getUnicode(), text));
    }

    List<PageTextPosition> getPositions() {
        return List.copyOf(positions);
    }
}
