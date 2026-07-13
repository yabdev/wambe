
# Media


## Properties

Name | Type
------------ | -------------
`id` | string
`eventId` | string
`role` | [MediaRole](MediaRole.md)
`filename` | string
`claimedMimeType` | string
`detectedMimeType` | string
`sizeBytes` | number
`status` | [MediaStatus](MediaStatus.md)
`rejectionCode` | string
`previewUrl` | string
`createdAt` | Date
`updatedAt` | Date

## Example

```typescript
import type { Media } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "eventId": null,
  "role": null,
  "filename": null,
  "claimedMimeType": null,
  "detectedMimeType": null,
  "sizeBytes": null,
  "status": null,
  "rejectionCode": null,
  "previewUrl": null,
  "createdAt": null,
  "updatedAt": null,
} satisfies Media

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Media
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


