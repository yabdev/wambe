
# WambeProductEventV1


## Properties

Name | Type
------------ | -------------
`schemaVersion` | any
`name` | string
`eventId` | string
`creationSessionId` | string
`occurredAt` | Date
`eligibility` | string
`deviceClass` | string
`networkQuality` | string
`properties` | [WambeProductEventV1Properties](WambeProductEventV1Properties.md)

## Example

```typescript
import type { WambeProductEventV1 } from '@wambe/api-client'

// TODO: Update the object below with actual values
const example = {
  "schemaVersion": null,
  "name": null,
  "eventId": null,
  "creationSessionId": null,
  "occurredAt": null,
  "eligibility": null,
  "deviceClass": null,
  "networkQuality": null,
  "properties": null,
} satisfies WambeProductEventV1

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WambeProductEventV1
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


