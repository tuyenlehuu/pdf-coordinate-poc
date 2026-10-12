package com.poc.pdfcoordinates.api;

import java.util.List;

public record CoordinateSearchResponse(
        String query,
        MatchMode mode,
        int totalMatches,
        List<CoordinateMatch> matches
) {
}
