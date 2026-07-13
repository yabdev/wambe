
# ScannerCallbackRequest


## Properties

Name | Type
------------ | -------------
`mediaId` | string
`result` | string
`detectedMimeType` | string
`objectSha256` | string
`rejectionCode` | string
`previewQuarantinePath` | string
`previewSha256` | string

## Example

```typescript
import type { ScannerCallbackRequest } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "mediaId": null,
  "result": null,
  "detectedMimeType": null,
  "objectSha256": null,
  "rejectionCode": null,
  "previewQuarantinePath": null,
  "previewSha256": null,
} satisfies ScannerCallbackRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ScannerCallbackRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


