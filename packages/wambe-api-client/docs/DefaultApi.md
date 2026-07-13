# DefaultApi

All URIs are relative to */api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**acceptScannerCallback**](DefaultApi.md#acceptscannercallback) | **POST** /internal/scanner/callback | Accept one signed, single-use malware scan result |
| [**completeMediaUpload**](DefaultApi.md#completemediaupload) | **POST** /events/{eventId}/media/{mediaId}/complete | Verify quarantine upload and enqueue scanning |
| [**createEvent**](DefaultApi.md#createeventoperation) | **POST** /events | Create an owned draft and creation session |
| [**createMediaIntent**](DefaultApi.md#createmediaintentoperation) | **POST** /events/{eventId}/media/intents | Create a private quarantine upload intent |
| [**deleteEvent**](DefaultApi.md#deleteevent) | **DELETE** /events/{eventId} | Soft-delete an owned event and schedule media purge |
| [**deleteMedia**](DefaultApi.md#deletemedia) | **DELETE** /events/{eventId}/media/{mediaId} | Detach media and schedule active-storage purge |
| [**dispatchDueScanJobs**](DefaultApi.md#dispatchduescanjobs) | **POST** /internal/jobs/scan-dispatch | Lease and dispatch due media scan jobs |
| [**getEvent**](DefaultApi.md#getevent) | **GET** /events/{eventId} | Get one owned event management representation |
| [**getPublicEventMetadata**](DefaultApi.md#getpubliceventmetadata) | **GET** /public/events/{slug}/metadata | Return visibility-safe metadata for Next.js canonical and OG rendering |
| [**linkIdentity**](DefaultApi.md#linkidentityoperation) | **POST** /auth/link-identity | Link a verified provider identity after recent reauthentication |
| [**listEvents**](DefaultApi.md#listevents) | **GET** /events | List the authenticated host\&#39;s events |
| [**listMedia**](DefaultApi.md#listmedia) | **GET** /events/{eventId}/media | List upload, scan, and preview status |
| [**publishEvent**](DefaultApi.md#publishevent) | **POST** /events/{eventId}/publish | Validate and transactionally publish or republish an event |
| [**recordCreationMilestone**](DefaultApi.md#recordcreationmilestone) | **POST** /creation-sessions/{sessionId}/events | Record one allowlisted client milestone with server receipt time |
| [**runRetentionJobs**](DefaultApi.md#runretentionjobs) | **POST** /internal/jobs/retention | Run due draft, media, idempotency, and nonce retention |
| [**unpublishEvent**](DefaultApi.md#unpublishevent) | **POST** /events/{eventId}/unpublish | Unpublish while preserving the stable slug |
| [**updateEvent**](DefaultApi.md#updateeventoperation) | **PATCH** /events/{eventId} | Autosave partial event fields |



## acceptScannerCallback

> acceptScannerCallback(xWambeTimestamp, xWambeNonce, scannerCallbackRequest)

Accept one signed, single-use malware scan result

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { AcceptScannerCallbackRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: scannerHmac
    apiKey: "YOUR API KEY",
  });
  const api = new DefaultApi(config);

  const body = {
    // Date | UTC request time; callbacks outside the five-minute acceptance window are rejected
    xWambeTimestamp: 2013-10-20T19:20:30+01:00,
    // string | Single-use UUID consumed atomically before media state changes
    xWambeNonce: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // ScannerCallbackRequest
    scannerCallbackRequest: ...,
  } satisfies AcceptScannerCallbackRequest;

  try {
    const data = await api.acceptScannerCallback(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xWambeTimestamp** | `Date` | UTC request time; callbacks outside the five-minute acceptance window are rejected | [Defaults to `undefined`] |
| **xWambeNonce** | `string` | Single-use UUID consumed atomically before media state changes | [Defaults to `undefined`] |
| **scannerCallbackRequest** | [ScannerCallbackRequest](ScannerCallbackRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

[scannerHmac](../README.md#scannerHmac)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Result accepted or identical terminal result already applied |  -  |
| **401** | Authentication is missing, expired, or invalid |  -  |
| **409** | State, version, or idempotency conflict |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## completeMediaUpload

> Media completeMediaUpload(eventId, mediaId, idempotencyKey)

Verify quarantine upload and enqueue scanning

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { CompleteMediaUploadRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    mediaId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies CompleteMediaUploadRequest;

  try {
    const data = await api.completeMediaUpload(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **mediaId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |

### Return type

[**Media**](Media.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Scan queued |  -  |
| **409** | State, version, or idempotency conflict |  -  |
| **404** | Resource not found or not owned by this host |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createEvent

> EventWithSession createEvent(idempotencyKey, createEventRequest)

Create an owned draft and creation session

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { CreateEventOperationRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // CreateEventRequest
    createEventRequest: ...,
  } satisfies CreateEventOperationRequest;

  try {
    const data = await api.createEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **createEventRequest** | [CreateEventRequest](CreateEventRequest.md) |  | |

### Return type

[**EventWithSession**](EventWithSession.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Draft created |  -  |
| **409** | State, version, or idempotency conflict |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createMediaIntent

> CreateMediaIntent201Response createMediaIntent(eventId, idempotencyKey, createMediaIntentRequest)

Create a private quarantine upload intent

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { CreateMediaIntentOperationRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // CreateMediaIntentRequest
    createMediaIntentRequest: ...,
  } satisfies CreateMediaIntentOperationRequest;

  try {
    const data = await api.createMediaIntent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **createMediaIntentRequest** | [CreateMediaIntentRequest](CreateMediaIntentRequest.md) |  | |

### Return type

[**CreateMediaIntent201Response**](CreateMediaIntent201Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Signed quarantine upload intent |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteEvent

> deleteEvent(idempotencyKey, eventId)

Soft-delete an owned event and schedule media purge

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { DeleteEventRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteEventRequest;

  try {
    const data = await api.deleteEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **eventId** | `string` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Event removed from active surfaces |  -  |
| **404** | Resource not found or not owned by this host |  -  |
| **409** | State, version, or idempotency conflict |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteMedia

> deleteMedia(eventId, mediaId, idempotencyKey)

Detach media and schedule active-storage purge

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { DeleteMediaRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    mediaId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteMediaRequest;

  try {
    const data = await api.deleteMedia(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **mediaId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Media unavailable to active product surfaces |  -  |
| **404** | Resource not found or not owned by this host |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## dispatchDueScanJobs

> InternalJobResult dispatchDueScanJobs()

Lease and dispatch due media scan jobs

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { DispatchDueScanJobsRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: internalOidc
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  try {
    const data = await api.dispatchDueScanJobs();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**InternalJobResult**](InternalJobResult.md)

### Authorization

[internalOidc](../README.md#internalOidc)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Dispatch batch accepted |  -  |
| **401** | Authentication is missing, expired, or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getEvent

> Event getEvent(eventId)

Get one owned event management representation

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { GetEventRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetEventRequest;

  try {
    const data = await api.getEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**Event**](Event.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Event |  -  |
| **404** | Resource not found or not owned by this host |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getPublicEventMetadata

> PublicEventMetadata getPublicEventMetadata(slug)

Return visibility-safe metadata for Next.js canonical and OG rendering

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { GetPublicEventMetadataRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const api = new DefaultApi();

  const body = {
    // string
    slug: slug_example,
  } satisfies GetPublicEventMetadataRequest;

  try {
    const data = await api.getPublicEventMetadata(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **slug** | `string` |  | [Defaults to `undefined`] |

### Return type

[**PublicEventMetadata**](PublicEventMetadata.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Safe public or private-link metadata |  -  |
| **404** | Resource not found or not owned by this host |  -  |
| **410** | Event was deleted and is unavailable |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## linkIdentity

> linkIdentity(idempotencyKey, linkIdentityRequest)

Link a verified provider identity after recent reauthentication

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { LinkIdentityOperationRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // LinkIdentityRequest
    linkIdentityRequest: ...,
  } satisfies LinkIdentityOperationRequest;

  try {
    const data = await api.linkIdentity(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **linkIdentityRequest** | [LinkIdentityRequest](LinkIdentityRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Identity linked or already linked to this host |  -  |
| **401** | Authentication is missing, expired, or invalid |  -  |
| **409** | State, version, or idempotency conflict |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listEvents

> ListEvents200Response listEvents(status, cursor, limit)

List the authenticated host\&#39;s events

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { ListEventsRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // EventStatus (optional)
    status: ...,
    // string (optional)
    cursor: cursor_example,
    // number (optional)
    limit: 56,
  } satisfies ListEventsRequest;

  try {
    const data = await api.listEvents(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **status** | `EventStatus` |  | [Optional] [Defaults to `undefined`] [Enum: draft, published, unpublished, deleted] |
| **cursor** | `string` |  | [Optional] [Defaults to `undefined`] |
| **limit** | `number` |  | [Optional] [Defaults to `20`] |

### Return type

[**ListEvents200Response**](ListEvents200Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Owner-scoped event page |  -  |
| **401** | Authentication is missing, expired, or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listMedia

> ListMedia200Response listMedia(eventId)

List upload, scan, and preview status

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { ListMediaRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListMediaRequest;

  try {
    const data = await api.listMedia(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**ListMedia200Response**](ListMedia200Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Media states |  -  |
| **401** | Authentication is missing, expired, or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## publishEvent

> PublishEvent200Response publishEvent(eventId, idempotencyKey, ifMatch)

Validate and transactionally publish or republish an event

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { PublishEventRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string | Current integer event version
    ifMatch: ifMatch_example,
  } satisfies PublishEventRequest;

  try {
    const data = await api.publishEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **ifMatch** | `string` | Current integer event version | [Defaults to `undefined`] |

### Return type

[**PublishEvent200Response**](PublishEvent200Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Published event and stable canonical URL |  -  |
| **409** | State, version, or idempotency conflict |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## recordCreationMilestone

> recordCreationMilestone(sessionId, idempotencyKey, wambeProductEventV1)

Record one allowlisted client milestone with server receipt time

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { RecordCreationMilestoneRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    sessionId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // WambeProductEventV1
    wambeProductEventV1: ...,
  } satisfies RecordCreationMilestoneRequest;

  try {
    const data = await api.recordCreationMilestone(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **sessionId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **wambeProductEventV1** | [WambeProductEventV1](WambeProductEventV1.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Milestone accepted |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## runRetentionJobs

> InternalJobResult runRetentionJobs()

Run due draft, media, idempotency, and nonce retention

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { RunRetentionJobsRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: internalOidc
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  try {
    const data = await api.runRetentionJobs();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**InternalJobResult**](InternalJobResult.md)

### Authorization

[internalOidc](../README.md#internalOidc)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Retention batch accepted |  -  |
| **401** | Authentication is missing, expired, or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## unpublishEvent

> Event unpublishEvent(eventId, idempotencyKey, ifMatch)

Unpublish while preserving the stable slug

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { UnpublishEventRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string | Current integer event version
    ifMatch: ifMatch_example,
  } satisfies UnpublishEventRequest;

  try {
    const data = await api.unpublishEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **ifMatch** | `string` | Current integer event version | [Defaults to `undefined`] |

### Return type

[**Event**](Event.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Unpublished event |  -  |
| **404** | Resource not found or not owned by this host |  -  |
| **409** | State, version, or idempotency conflict |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateEvent

> Event updateEvent(idempotencyKey, ifMatch, eventId, updateEventRequest)

Autosave partial event fields

### Example

```ts
import {
  Configuration,
  DefaultApi,
} from '@wambe/api-client';
import type { UpdateEventOperationRequest } from '@wambe/api-client';

async function example() {
  console.log("🚀 Testing @wambe/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new DefaultApi(config);

  const body = {
    // string
    idempotencyKey: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string | Current integer event version
    ifMatch: ifMatch_example,
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // UpdateEventRequest
    updateEventRequest: ...,
  } satisfies UpdateEventOperationRequest;

  try {
    const data = await api.updateEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **idempotencyKey** | `string` |  | [Defaults to `undefined`] |
| **ifMatch** | `string` | Current integer event version | [Defaults to `undefined`] |
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **updateEventRequest** | [UpdateEventRequest](UpdateEventRequest.md) |  | |

### Return type

[**Event**](Event.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Saved event with incremented version |  -  |
| **404** | Resource not found or not owned by this host |  -  |
| **409** | State, version, or idempotency conflict |  -  |
| **422** | Request or publication validation failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

