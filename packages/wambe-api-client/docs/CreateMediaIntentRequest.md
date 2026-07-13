
# CreateMediaIntentRequest


## Properties

Name | Type
------------ | -------------
`role` | [MediaRole](MediaRole.md)
`filename` | string
`claimedMimeType` | string
`sizeBytes` | number

## Example

```typescript
import type { CreateMediaIntentRequest } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "role": null,
  "filename": null,
  "claimedMimeType": null,
  "sizeBytes": null,
} satisfies CreateMediaIntentRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateMediaIntentRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


