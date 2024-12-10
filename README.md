# FETCH INTERVIEW PROJECT - ANDROID
Instructions and details for how this project was developed.

## Table of Contents

* [Build tools & versions used](#build-tools-versions-used)
* [Steps to run the app](#steps-to-run-app)
* [What areas of the app did you focus on?](#areas-of-focus)
* [What was the reason for your focus? What problems were you trying to solve?](#reason-for-focus-problems-solved)
* [How long did you spend on this project?](#time-spent-on-project)
* [Is there any other information you’d like us to know?](#additional-information)
* [Resources](#resources)
* [CHANGELOG](#changelog)

<a name="build-tools-versions-used"></a>
## Build tools & versions used

Some technologies and dependencies used in this project are Material Design 3, Compose,
Coroutines, OkHTTP, and Google GSON.

CompileSDK = 34
MinSDK = 24
TargetSDK = 34

[table of contents](#table-of-contents)

<a name="steps-to-run-app"></a>
## Steps to run the app

There are no additional or special instructions to run the project. Everything should work without 
any additional setup. As mentioned in the instructions, I display a list of items based on the 
following requirements:

* Display all the items grouped by "listId"
* Sort the results first by "listId" then by "name" when displaying.
* Filter out any items where "name" is blank or null.

The final result is an easy-to-read list. For each different `listId` I color code the sections
for better visualization.

[table of contents](#table-of-contents)

<a name="areas-of-focus"></a>
## What areas of the app did you focus on?

This project is a small/simple application. The instructions said,  

`While your solution does not need to be fully production ready, you are being evaluated so 
put your best foot forward.`

so I wanted to showcase more of my capabilities. While not fully production level code, 
most of my time was spent on developing a framework that illustrates my ability to architect, 
design, and display my knowledge of various architectural patterns. The framework I developed is 
reusable, scalable, and testable. The framework is a custom HTTP stack wrapper; a layer of abstraction 
over the native HTTP (OkHttp) client, designed to handle HTTP requests and responses more efficiently
and flexibly. I did not add the below features, but the framework was made extensible for various 
functionalities such as:

1) Query Parameters:
* You can easily add support for query parameters to the request (`GetItemsRequest`) and 
executor (`OkHttpRequestExecutor`). This makes constructing complex URLs with dynamic parameters 
straightforward.

2) HTTP Setting Configurations:
* You can easily configure settings on the executor (`OkHttpRequestExecutor`) to include timeouts, 
headers, and retries logic. 

3) Logging:
* I did not include any logging. Normally I would capture detailed request and response data.
As well as logging exceptions and API errors. This usually comes in the form of a (`Logger`) wrapper
class that also lives in the framework and is configured to only work in non-production environments.

4) Metric Tracking for Exceptions and API Failures:
* The executor (`OkHttpRequestExecutor`) is setup in a way where you can easily capture data on 
API failures (and success), allowing you to identify patterns and potential issues.

5) API Timestamp Measurements:
* The executor (`OkHttpRequestExecutor`) is setup in a way where you can easy measuring timestamps 
of API calls to help monitor the performance of your requests. You can track the time taken for 
requests to complete and use this data to identify and optimize slow or problematic endpoints.

6) Extensibility for Future Features:
* The API client (`FetchApiClient`) wrapper was designed with extensibility in mind. It use interfaces 
and abstract classes so that new features or configurations can be added without altering the 
existing codebase.

Note: The framework was the best area to showcase more complex code and design. I could have easily 
used Retrofit2 to complete this assignment. That solution would not have been as extensible, scalable, 
testable, or reusable.

```
// simple Retrofit implementation example
interface ApiService {
    @GET("hiring.json")
    suspend fun getItems(): List<Item>
}

object ApiClient {
    private const val BASE_URL = "https://fetch-hiring.s3.amazonaws.com/"
    val retrofit: ApiService =
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiService::class.java)
}
```

[table of contents](#table-of-contents)

<a name="reason-for-focus-problems-solved"></a>
## What was the reason for your focus? What problems were you trying to solve?

An extensible framework is an infrastructure that provides ready-made components and solutions
to speed up development, efficiency and uniformity. I focused on creating a HTTP stack and gateway
because of the fact that I could showcase more complex code, while simplifying the actual work done 
in the application.

The framework works through a single point of access for performing requests through the 
`FetchApiClient`. This API Client handles API errors gracefully, with debugging, logging and 
scalability in mind. The response uses generics to handle various types of responses, not just
a response with an Array root. The object is not hardcoded, so a List of any type/object would
still work, and even no returned response would not crash the application.

I used configuration to showcase that the `FetchApiClient` could be configured and would work
in a real-world environment for any number of applications that also require /hiring.json request.

Here is an example of how to form the `ClientConfiguration`. Developers referencing this framework
would initialize the `ClientConfiguration` in their Application class.

Note: Whatever base URL that is set, the framework will try to sanitize it. Meaning missing 
protocols e.g. `http` or `https` is allowed. This case is handled.

Example (KOTLIN):
```
// initialize client configuration
val clientConfiguration = ClientConfiguration.Builder()
    .setBaseUrl(baseUrl)
    .create()
```  

I only added an option for setting the base URL e.g. debug environment versus production environment. 
The reason I went with a Builder pattern is because it is extensible for other options such as 
apiKeys and other necessary tokens. 

The `FetchApiClientProvider` is a Singleton class that only needs to be initialized once. After
initialization the `FetchApiClientProvider` can be accessed anytime in your application to make 
requests handled by the internal framework. The `FetchApiClientProvider` accepts the 
`ClientConfiguration` so that you can control and manage the `FetchApiClient`.

Example (KOTLIN):
```
// pass in client configuration as parameters
FetchApiClientProvider.initialize(clientConfiguration)
```

Anytime you want to make requests through the `FetchApiClient` you can retrieve an initialized
object from the `FetchApiClientProvider` in a single line of code.

Example (KOTLIN):
```
// api client
private val fetchApiClient: FetchApiClientInterfaces = FetchApiClientProvider.getInstance()
```

[table of contents](#table-of-contents)

<a name="time-spent-on-project"></a>
## How long did you spend on this project?

Below is the timesheet for the project:

Format: (yyyy-mm-dd)
1. 2024-12-09 | Framework + Unit Tests - 3.5 hrs
2. 2024-12-10 | Implementing Compose - 1 hr
3. 2024-12-10 | README + final cleanup - 1 hr

My IDE tracks my coding time. See attached screenshot.
[![ss-time-spent.png](https://i.postimg.cc/g00NK12r/ss-time-spent.png)](https://postimg.cc/fV6cz2Xh)

[table of contents](#table-of-contents)

<a name="additional-information"></a>
## Is there any other information you’d like us to know?

Thank you for this opportunity to interview with you. I am excited and look forward to hearing 
from you. 

Leonard Tatum
(e): ljtatum@hotmail.com

[table of contents](#table-of-contents)

<a name="resources"></a>
## Resources

Here are some online documentation I referred to:
1. [Compose Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3)
2. [Compose Basics](https://developer.android.com/codelabs/jetpack-compose-basics#0)
3. [Basic Layouts in Compose](https://developer.android.com/codelabs/jetpack-compose-layouts?index=..%2F..index#0)

[table of contents](#table-of-contents)

<a name="changelog"></a>
## CHANGELOG

Format: (yyyy-mm-dd)
1. 2024-12-10: README created (1.0.0)\
