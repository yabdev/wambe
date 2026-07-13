
# CreateEventRequest


## Properties

Name | Type
------------ | -------------
`clientCreationKey` | string
`eligibility` | string
`deviceClass` | string
`networkQuality` | string

## Example

```typescript
import type { CreateEventRequest } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "clientCreationKey": null,
  "eligibility": null,
  "deviceClass": null,
  "networkQuality": null,
} satisfies CreateEventRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateEventRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


