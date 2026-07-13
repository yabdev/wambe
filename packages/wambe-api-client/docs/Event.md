
# Event


## Properties

Name | Type
------------ | -------------
`id` | string
`ownerId` | string
`status` | [EventStatus](EventStatus.md)
`version` | number
`eventType` | string
`title` | string
`startsAt` | Date
`timezone` | string
`venue` | [Venue](Venue.md)
`visibility` | [Visibility](Visibility.md)
`dressCodeNotes` | string
`slug` | string
`canonicalUrl` | string
`shareEligible` | boolean
`shareBlockedReason` | string
`publishedAt` | Date
`lastSavedAt` | Date
`createdAt` | Date
`updatedAt` | Date
`media` | [Array&lt;Media&gt;](Media.md)

## Example

```typescript
import type { Event } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "ownerId": null,
  "status": null,
  "version": null,
  "eventType": null,
  "title": null,
  "startsAt": null,
  "timezone": null,
  "venue": null,
  "visibility": null,
  "dressCodeNotes": null,
  "slug": null,
  "canonicalUrl": null,
  "shareEligible": null,
  "shareBlockedReason": null,
  "publishedAt": null,
  "lastSavedAt": null,
  "createdAt": null,
  "updatedAt": null,
  "media": null,
} satisfies Event

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Event
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


