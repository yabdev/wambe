
# CreateMediaIntent201Response


## Properties

Name | Type
------------ | -------------
`media` | [Media](Media.md)
`uploadUrl` | string
`expiresAt` | Date

## Example

```typescript
import type { CreateMediaIntent201Response } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "media": null,
  "uploadUrl": null,
  "expiresAt": null,
} satisfies CreateMediaIntent201Response

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateMediaIntent201Response
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


