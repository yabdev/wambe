# @wambe/api-client@0.1.0

A TypeScript SDK client for the localhost API.

## Usage

First, install the SDK from npm.

```bash
npm install @wambe/api-client --save
```

Next, try it out.


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


## Documentation

### API Endpoints

All URIs are relative to */api/v1*

| Class | Method | HTTP request | Description
| ----- | ------ | ------------ | -------------
*DefaultApi* | [**acceptScannerCallback**](docs/DefaultApi.md#acceptscannercallback) | **POST** /internal/scanner/callback | Accept one signed, single-use malware scan result
*DefaultApi* | [**completeMediaUpload**](docs/DefaultApi.md#completemediaupload) | **POST** /events/{eventId}/media/{mediaId}/complete | Verify quarantine upload and enqueue scanning
*DefaultApi* | [**createEvent**](docs/DefaultApi.md#createeventoperation) | **POST** /events | Create an owned draft and creation session
*DefaultApi* | [**createMediaIntent**](docs/DefaultApi.md#createmediaintentoperation) | **POST** /events/{eventId}/media/intents | Create a private quarantine upload intent
*DefaultApi* | [**deleteEvent**](docs/DefaultApi.md#deleteevent) | **DELETE** /events/{eventId} | Soft-delete an owned event and schedule media purge
*DefaultApi* | [**deleteMedia**](docs/DefaultApi.md#deletemedia) | **DELETE** /events/{eventId}/media/{mediaId} | Detach media and schedule active-storage purge
*DefaultApi* | [**dispatchDueScanJobs**](docs/DefaultApi.md#dispatchduescanjobs) | **POST** /internal/jobs/scan-dispatch | Lease and dispatch due media scan jobs
*DefaultApi* | [**getEvent**](docs/DefaultApi.md#getevent) | **GET** /events/{eventId} | Get one owned event management representation
*DefaultApi* | [**getPublicEventMetadata**](docs/DefaultApi.md#getpubliceventmetadata) | **GET** /public/events/{slug}/metadata | Return visibility-safe metadata for Next.js canonical and OG rendering
*DefaultApi* | [**linkIdentity**](docs/DefaultApi.md#linkidentityoperation) | **POST** /auth/link-identity | Link a verified provider identity after recent reauthentication
*DefaultApi* | [**listEvents**](docs/DefaultApi.md#listevents) | **GET** /events | List the authenticated host\&#39;s events
*DefaultApi* | [**listMedia**](docs/DefaultApi.md#listmedia) | **GET** /events/{eventId}/media | List upload, scan, and preview status
*DefaultApi* | [**publishEvent**](docs/DefaultApi.md#publishevent) | **POST** /events/{eventId}/publish | Validate and transactionally publish or republish an event
*DefaultApi* | [**recordCreationMilestone**](docs/DefaultApi.md#recordcreationmilestone) | **POST** /creation-sessions/{sessionId}/events | Record one allowlisted client milestone with server receipt time
*DefaultApi* | [**runRetentionJobs**](docs/DefaultApi.md#runretentionjobs) | **POST** /internal/jobs/retention | Run due draft, media, idempotency, and nonce retention
*DefaultApi* | [**unpublishEvent**](docs/DefaultApi.md#unpublishevent) | **POST** /events/{eventId}/unpublish | Unpublish while preserving the stable slug
*DefaultApi* | [**updateEvent**](docs/DefaultApi.md#updateeventoperation) | **PATCH** /events/{eventId} | Autosave partial event fields


### Models

- [CreateEventRequest](docs/CreateEventRequest.md)
- [CreateMediaIntent201Response](docs/CreateMediaIntent201Response.md)
- [CreateMediaIntentRequest](docs/CreateMediaIntentRequest.md)
- [ErrorEnvelope](docs/ErrorEnvelope.md)
- [ErrorEnvelopeError](docs/ErrorEnvelopeError.md)
- [Event](docs/Event.md)
- [EventStatus](docs/EventStatus.md)
- [EventWithSession](docs/EventWithSession.md)
- [InternalJobResult](docs/InternalJobResult.md)
- [LinkIdentityRequest](docs/LinkIdentityRequest.md)
- [ListEvents200Response](docs/ListEvents200Response.md)
- [ListMedia200Response](docs/ListMedia200Response.md)
- [Media](docs/Media.md)
- [MediaRole](docs/MediaRole.md)
- [MediaStatus](docs/MediaStatus.md)
- [PublicEventMetadata](docs/PublicEventMetadata.md)
- [PublishEvent200Response](docs/PublishEvent200Response.md)
- [ScannerCallbackRequest](docs/ScannerCallbackRequest.md)
- [UpdateEventRequest](docs/UpdateEventRequest.md)
- [Venue](docs/Venue.md)
- [Visibility](docs/Visibility.md)
- [WambeProductEventV1](docs/WambeProductEventV1.md)
- [WambeProductEventV1Properties](docs/WambeProductEventV1Properties.md)

### Authorization


Authentication schemes defined for the API:
<a id="bearerAuth"></a>
#### bearerAuth


- **Type**: HTTP Bearer Token authentication (SupabaseJWT)
<a id="scannerHmac"></a>
#### scannerHmac


- **Type**: API key
- **API key parameter name**: `X-Wambe-Signature`
- **Location**: HTTP header
<a id="internalOidc"></a>
#### internalOidc


- **Type**: HTTP Bearer Token authentication (GoogleOIDC)

## About

This TypeScript SDK client supports the [Fetch API](https://fetch.spec.whatwg.org/)
and is automatically generated by the
[OpenAPI Generator](https://openapi-generator.tech) project:

- API version: `1.0.0`
- Package version: `0.1.0`
- Generator version: `7.23.0`
- Build package: `org.openapitools.codegen.languages.TypeScriptFetchClientCodegen`

The generated npm module supports the following:

- Environments
  * Node.js
  * Webpack
  * Browserify
- Language levels
  * ES5 - you must have a Promises/A+ library installed
  * ES6
- Module systems
  * CommonJS
  * ES6 module system


## Development

### Building

To build the TypeScript source code, you need to have Node.js and npm installed.
After cloning the repository, navigate to the project directory and run:

```bash
npm install
npm run build
```

### Publishing

Once you've built the package, you can publish it to npm:

```bash
npm publish
```

## License

[Proprietary]()
