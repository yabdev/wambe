
# InternalJobResult


## Properties

Name | Type
------------ | -------------
`job` | string
`acceptedAt` | Date
`examined` | number
`affected` | number

## Example

```typescript
import type { InternalJobResult } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "job": null,
  "acceptedAt": null,
  "examined": null,
  "affected": null,
} satisfies InternalJobResult

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as InternalJobResult
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


