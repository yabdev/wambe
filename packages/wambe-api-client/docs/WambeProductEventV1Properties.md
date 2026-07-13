
# WambeProductEventV1Properties


## Properties

Name | Type
------------ | -------------
`step` | string
`outcomeCode` | string
`mediaRole` | string
`visibility` | string
`hadMediaUpload` | boolean
`lifecycleAction` | string

## Example

```typescript
import type { WambeProductEventV1Properties } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "step": null,
  "outcomeCode": null,
  "mediaRole": null,
  "visibility": null,
  "hadMediaUpload": null,
  "lifecycleAction": null,
} satisfies WambeProductEventV1Properties

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WambeProductEventV1Properties
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


