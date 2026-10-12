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

## Notes

This POC works for PDFs that contain selectable text. Scanned PDFs are images, so OCR (for example, Tesseract) must be added before text and coordinates can be extracted. A match that spans multiple visual lines returns one bounding box covering all of those lines; production document templates should normally use a query unique to one field label/value.
