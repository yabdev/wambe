
# Venue


## Properties

Name | Type
------------ | -------------
`name` | string
`displayAddress` | string
`placeId` | string
`latitude` | number
`longitude` | number
`confirmed` | boolean

## Example

```typescript
import type { Venue } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "name": null,
  "displayAddress": null,
  "placeId": null,
  "latitude": null,
  "longitude": null,
  "confirmed": null,
} satisfies Venue

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Venue
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


