package com.poc.pdfcoordinates.api;

public record CoordinateMatch(
        int page,
        String text,
        float x,
        float y,
        float width,
        float height,
        float topLeftX,
        float topLeftY
) {
}
