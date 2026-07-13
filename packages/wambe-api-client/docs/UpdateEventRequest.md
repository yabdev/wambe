
# UpdateEventRequest


## Properties

Name | Type
------------ | -------------
`eventType` | string
`title` | string
`startsAt` | Date
`timezone` | string
`venue` | [Venue](Venue.md)
`visibility` | [Visibility](Visibility.md)
`dressCodeNotes` | string

## Example

```typescript
import type { UpdateEventRequest } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "eventType": null,
  "title": null,
  "startsAt": null,
  "timezone": null,
  "venue": null,
  "visibility": null,
  "dressCodeNotes": null,
} satisfies UpdateEventRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateEventRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


