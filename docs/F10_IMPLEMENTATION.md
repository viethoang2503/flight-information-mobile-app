# F10 — My Flights Statistics

## Included
- A `View statistics` glass card at the top of My Flights, hidden when the saved-flight list is empty.
- `StatsActivity` with a 2×2 metric grid, airline bar chart, and most frequent departure airport.
- `FlightStats`, a pure-Java aggregation class, and unit tests.
- `BarChartView`, a Canvas-based chart with touch selection and accessibility summary.
- English and Vietnamese strings, plus the activity declaration in the manifest.

## Calculation rules
- Flight count includes all saved flights, including cancelled flights.
- Distance is the sum of great-circle distances for flights where both airport IATA codes exist in the bundled airport dataset. Unknown airports contribute 0 km.
- Flight duration uses the API's `duration` value (minutes) when positive; otherwise it falls back to best-known arrival minus departure timestamps.
- On-time rate excludes cancelled flights from the denominator. A flight counts as on time when its reported/derived departure delay is below 15 minutes or is not available.
- Airline chart shows the five most frequent airline codes; remaining airlines are combined into “Other”.
- The busiest departure airport is the most frequent departure IATA code; ties are resolved alphabetically.

## Test
Run from the project root:
- Windows: `gradlew.bat testDebugUnitTest`
- macOS/Linux: `./gradlew testDebugUnitTest`

The Android Gradle build could not be run in the code-generation environment because the Gradle distribution was not cached and external downloads were unavailable. The new pure-Java statistics classes were compiled with `javac`, a smoke test passed, and all XML resources parsed successfully.
