
# ErrorEnvelopeError


## Properties

Name | Type
------------ | -------------
`code` | string
`message` | string
`requestId` | string
`fieldErrors` | { [key: string]: Array&lt;string&gt;; }

## Example

```typescript
import type { ErrorEnvelopeError } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "code": null,
  "message": null,
  "requestId": null,
  "fieldErrors": null,
} satisfies ErrorEnvelopeError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ErrorEnvelopeError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


