
# F9 - Add to Calendar Testing

## Feature
Add a flight to the Android Calendar.

## Test Cases

1. Open Flight Detail screen.
2. Select Add to Calendar.
3. Verify Calendar application opens.
4. Verify flight number and route.
5. Verify departure and arrival times.
6. Verify airport information.
7. Test a flight without arrival time.
8. Test when no Calendar app is available.

## Expected Result
The Calendar event form opens with flight
information pre-filled. The user can review
and save the event.

## Build Verification
Run: ./gradlew assembleDebug
