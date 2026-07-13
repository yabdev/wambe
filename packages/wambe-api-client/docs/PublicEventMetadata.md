
# PublicEventMetadata


## Properties

Name | Type
------------ | -------------
`slug` | string
`title` | string
`startsAt` | Date
`visibility` | string
`indexable` | boolean
`canonicalUrl` | string
`invitationPreviewUrl` | string
`venueName` | string
`venueAddress` | string

## Example

```typescript
import type { PublicEventMetadata } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "slug": null,
  "title": null,
  "startsAt": null,
  "visibility": null,
  "indexable": null,
  "canonicalUrl": null,
  "invitationPreviewUrl": null,
  "venueName": null,
  "venueAddress": null,
} satisfies PublicEventMetadata

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PublicEventMetadata
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


