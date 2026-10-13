# PDF Coordinate POC

Spring Boot + Maven POC to find text in a text-based PDF and return each matching bounding box.

## Run

```powershell
cd .\pdf-coordinate-poc
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## API

`POST /api/pdf/coordinates` using `multipart/form-data`.

| Field | Required | Description |
| --- | --- | --- |
| `file` | Yes | PDF file (up to 20 MB) |
| `query` | Yes | Text or regular expression to find |
| `mode` | No | `CONTAINS` (default), `EXACT`, or `REGEX` |
| `caseSensitive` | No | `false` by default |
| `width` | No | Fallback box width in PDF points, default `150` |
| `height` | No | Fallback box height in PDF points, default `50` |

```powershell
curl.exe -X POST "http://localhost:8080/api/pdf/coordinates" `
  -F "file=@C:\temp\sample.pdf;type=application/pdf" `
  -F "query=Invoice Number" `
  -F "mode=CONTAINS"
```

Example response:

```json
{
  "query": "Invoice Number",
  "mode": "CONTAINS",
  "totalMatches": 1,
  "matches": [
    {
      "page": 1,
      "text": "Invoice Number",
      "x": 72.0,
      "y": 690.2,
      "width": 86.4,
      "height": 10.2,
      "topLeftX": 72.0,
      "topLeftY": 91.6
    }
  ]
}
```

`x` and `y` use the usual PDF coordinate system: origin at the **bottom-left** of the page. `topLeftX` and `topLeftY` are included for UI/browser overlays, where the origin is **top-left**.

If the query is not found anywhere in the PDF (including a scanned PDF), the API returns one fallback box on the **last page** with `text: ""` and `totalMatches: 1`. Its top-left coordinates are `(pageWidth - width, pageHeight - height)`; in PDF bottom-left coordinates, `x = pageWidth - width` and `y = 0`. Page dimensions use the media box, consistent with text matches. `width` and `height` must be finite positive numbers and must fit within the last page when fallback is used. They do not change the bounding boxes of actual text matches.

## Notes

This POC works for PDFs that contain selectable text. Scanned PDFs are images, so OCR (for example, Tesseract) must be added before text and coordinates can be extracted. A match that spans multiple visual lines returns one bounding box covering all of those lines; production document templates should normally use a query unique to one field label/value.
