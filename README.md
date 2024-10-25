
# Trading Statistics Service

Spring Boot-based application REST service providing functionality to batch-process trading data and calculate 
various statistics like average, variance, min, max, and last price for a given symbol over different prices range periods.

Prices are added in batches no more than 10 0000 items at once. 
Service will store no more than 100 000 000 'newest' prices.
Statistics may be retrieved in ranges by specifying K index - number of stats pulled will be presented for lastly added 1eK.
Id the data set is not yet ready (not enough data for set ex. 88 items in cache but request is 100) it will reply with bad request.
(Responding to not full sets would require slight modifications but is easily possible).

For simplicity service stores and manipulates Floats as prices, BigDecimals are used for expected large values stored in caches.
This was assumed as for better control of floating points and performance would be better to have some strategy 
to assume decimal range, then prices would be Long or BigInteger if needed converted with agreed decimal range.

General approach is to use separated data sets for k ranges with no duplication of data.
Whole deque of 1e8 data is available when combined from all lower data (consecutively for increasing k - 10 90 900 9000 and so on).
Sum and squared sums ale calculated when batch is fed, max min are stored in TreeMap (more memory used for faster access and management simplicity).
Depending on add_batch/stats balance (especially when stats are rare and adds are frequent) this approach might need modification according.


Assuming there is a lot of ram but not infinite and contract:
-Service will not hold more than 1e8 items, older items will be removed by cache sizes limit when rolling
-Service will use TreeMap for performance and simplicity/readability.
-All prices are stored in separate dequeues, data is not overlying, size is controlled by rolling data when adding.
-Service will only allow 10 unique symbols(instruments)



Tests include:
TSSBasicTest - some basics units
TSSCornerCasesTest - batch size, symbols presence, to little data for k stats
TSSTradingDataControllerIntegrationTest - simple integration
TSS1001e4ItemsTest - longer running test with 100010000 prices

## How to Build and Run the Project

### Prerequisites

- Java 17 or higher
- Maven

### Building the Project

To build the project, navigate to the project root directory and run:

```bash
mvn clean install
```

This will compile the project, run the tests, and package the application into a JAR file.

### Running the Application

To run the application after building, use the following command:

```bash
mvn spring-boot:run
```

The application will start on `localhost:8080` by default.

### Running Specific Tests

To run a specific test class, such as TSSBasicTest:

```bash
mvn -Dtest=com.abc.tradingstatisticsservice.TSSBasicTest test
```


## REST API Endpoints

### 1. Add a Batch of Trading Data

**Endpoint:** `/add_batch/`  
**Method:** `POST`  
**Description:** Adds a batch of trading data for a specific symbol.

**Request Body Format (JSON):**
```json
{
  "symbol": "AAPL",
  "values": [1.1, 2.3, 3.5, 4.7, 5.9, 6.2, 7.1, 8.3, 9.5, 10.8]
}
```

**Sample `curl` Command (Windows):**
```bash
curl -X POST http://localhost:8080/add_batch/ -H "Content-Type: application/json" -d "{\"symbol\": \"AAPL\",  \"values\": [1.1, 2.3, 3.5, 4.7, 5.9, 6.2, 7.1, 8.3, 9.5, 10.8]}"
```

### 2. Get Statistics for a Symbol

**Endpoint:** `/stats/`  
**Method:** `GET`  
**Description:** Retrieves the calculated statistics (average, variance, min, max, and last price) for a given symbol and time period `k`.

**Parameters:**
- `symbol` (String): The symbol of the stock (e.g., "AAPL").
- `k` (int): The time period `k` (e.g., 1 = 10 items, 2 = 100 items, 3 = 1000 items, etc.).

**Sample `curl` Command (Windows):**
```bash
curl -X GET "http://localhost:8080/stats/?symbol=AAPL&k=2"
```

### Example Response:

```json
{
  "min": 1.1,
  "max": 10.8,
  "avg": 5.94,
  "variance": 8.7444,
  "last": 10.8
}
```

## Constraints

- **k Limits:** The `k` value corresponds to time periods:
  - k = 1: Last 10 items
  - k = 2: Last 100 items
  - k = 3: Last 1000 items
  - k = 4: Last 10000 items
  - k = 5: Last 100000 items
  - k = 6: Last 1000000 items
  - k = 7: Last 10000000 items
  - k = 8: Last 100000000 items
- **Max Batch Size:** The maximum batch size for a single request is 10,000 float values.
- **Data Precision:** Prices are handled as floats for batch insertion but computed as `BigDecimal` for sum and variance calculations to ensure precision.

## Error Handling

If the provided batch is too small (fewer than 10 items for `k=1`), or if there is insufficient data for the specified `k`, the service will return an error response:

**Example Error Response:**
```json
{
  "timestamp": "2024-10-24T21:33:05.132+00:00",
  "status": 400,
  "error": "Insufficient data for k=100",
  "message": "Insufficient data for k=100",
  "path": "/stats/"
}
```


