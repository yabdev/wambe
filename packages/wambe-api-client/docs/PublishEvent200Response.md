
# PublishEvent200Response


## Properties

Name | Type
------------ | -------------
`event` | [Event](Event.md)
`canonicalUrl` | string
`shareEligible` | boolean
`shareBlockedReason` | string

## Example

```typescript
import type { PublishEvent200Response } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "event": null,
  "canonicalUrl": null,
  "shareEligible": null,
  "shareBlockedReason": null,
} satisfies PublishEvent200Response

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PublishEvent200Response
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


