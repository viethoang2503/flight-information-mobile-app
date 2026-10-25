package vn.edu.usth.flightinfo.model;

/**
 * One flight, mirroring the AirLabs flight/schedules JSON.
 * Times are "yyyy-MM-dd HH:mm" in the airport's local time; *_ts fields are UNIX seconds (0 = unknown).
 * The original JSON is kept in {@link #getRawJson()} so a flight can be saved or passed in an Intent.
 */
public class Flight {

    private String flightIata;
    private String flightIcao;
    private String airlineIata;
    private String airlineIcao;
    private String rawStatus;

    private String depIata;
    private String depIcao;
    private String depTerminal;
    private String depGate;
    private String depTime;
    private long depTimeTs;
    private String depEstimated;
    private long depEstimatedTs;
    private String depActual;
    private long depActualTs;
    private Integer depDelayed;

    private String arrIata;
    private String arrIcao;
    private String arrTerminal;
    private String arrGate;
    private String arrBaggage;
    private String arrTime;
    private long arrTimeTs;
    private String arrEstimated;
    private long arrEstimatedTs;
    private String arrActual;
    private long arrActualTs;
    private Integer arrDelayed;

    private Integer duration;
    private String aircraftIcao;
    private String regNumber;

    private String rawJson;

    /** Departure delay in minutes: the reported value, else derived from estimated vs scheduled time. */
    public Integer getDelayMinutes() {
        if (depDelayed != null) {
            return depDelayed;
        }
        if (depTimeTs > 0 && depEstimatedTs > 0) {
            return (int) ((depEstimatedTs - depTimeTs) / 60);
        }
        return null;
    }

    public boolean isDelayed() {
        Integer delay = getDelayMinutes();
        return delay != null && delay >= FlightStatus.DELAY_THRESHOLD_MINUTES;
    }

    public FlightStatus getStatus() {
        return FlightStatus.from(rawStatus, getDelayMinutes());
    }

    /** Best known departure time: actual, then estimated, then scheduled. */
    public long getBestDepTs() {
        if (depActualTs > 0) return depActualTs;
        if (depEstimatedTs > 0) return depEstimatedTs;
        return depTimeTs;
    }

    /** Best known arrival time: actual, then estimated, then scheduled. */
    public long getBestArrTs() {
        if (arrActualTs > 0) return arrActualTs;
        if (arrEstimatedTs > 0) return arrEstimatedTs;
        return arrTimeTs;
    }

    public String getBestDepTime() {
        if (depActual != null) return depActual;
        if (depEstimated != null) return depEstimated;
        return depTime;
    }

    public String getBestArrTime() {
        if (arrActual != null) return arrActual;
        if (arrEstimated != null) return arrEstimated;
        return arrTime;
    }

    // --- Getters / setters ---

    public String getFlightIata() { return flightIata; }
    public void setFlightIata(String flightIata) { this.flightIata = flightIata; }

    public String getFlightIcao() { return flightIcao; }
    public void setFlightIcao(String flightIcao) { this.flightIcao = flightIcao; }

    public String getAirlineIata() { return airlineIata; }
    public void setAirlineIata(String airlineIata) { this.airlineIata = airlineIata; }

    public String getAirlineIcao() { return airlineIcao; }
    public void setAirlineIcao(String airlineIcao) { this.airlineIcao = airlineIcao; }

    public String getRawStatus() { return rawStatus; }
    public void setRawStatus(String rawStatus) { this.rawStatus = rawStatus; }

    public String getDepIata() { return depIata; }
    public void setDepIata(String depIata) { this.depIata = depIata; }

    public String getDepIcao() { return depIcao; }
    public void setDepIcao(String depIcao) { this.depIcao = depIcao; }

    public String getDepTerminal() { return depTerminal; }
    public void setDepTerminal(String depTerminal) { this.depTerminal = depTerminal; }

    public String getDepGate() { return depGate; }
    public void setDepGate(String depGate) { this.depGate = depGate; }

    public String getDepTime() { return depTime; }
    public void setDepTime(String depTime) { this.depTime = depTime; }

    public long getDepTimeTs() { return depTimeTs; }
    public void setDepTimeTs(long depTimeTs) { this.depTimeTs = depTimeTs; }

    public String getDepEstimated() { return depEstimated; }
    public void setDepEstimated(String depEstimated) { this.depEstimated = depEstimated; }

    public long getDepEstimatedTs() { return depEstimatedTs; }
    public void setDepEstimatedTs(long depEstimatedTs) { this.depEstimatedTs = depEstimatedTs; }

    public String getDepActual() { return depActual; }
    public void setDepActual(String depActual) { this.depActual = depActual; }

    public long getDepActualTs() { return depActualTs; }
    public void setDepActualTs(long depActualTs) { this.depActualTs = depActualTs; }

    public Integer getDepDelayed() { return depDelayed; }
    public void setDepDelayed(Integer depDelayed) { this.depDelayed = depDelayed; }

    public String getArrIata() { return arrIata; }
    public void setArrIata(String arrIata) { this.arrIata = arrIata; }

    public String getArrIcao() { return arrIcao; }
    public void setArrIcao(String arrIcao) { this.arrIcao = arrIcao; }

    public String getArrTerminal() { return arrTerminal; }
    public void setArrTerminal(String arrTerminal) { this.arrTerminal = arrTerminal; }

    public String getArrGate() { return arrGate; }
    public void setArrGate(String arrGate) { this.arrGate = arrGate; }

    public String getArrBaggage() { return arrBaggage; }
    public void setArrBaggage(String arrBaggage) { this.arrBaggage = arrBaggage; }

    public String getArrTime() { return arrTime; }
    public void setArrTime(String arrTime) { this.arrTime = arrTime; }

    public long getArrTimeTs() { return arrTimeTs; }
    public void setArrTimeTs(long arrTimeTs) { this.arrTimeTs = arrTimeTs; }

    public String getArrEstimated() { return arrEstimated; }
    public void setArrEstimated(String arrEstimated) { this.arrEstimated = arrEstimated; }

    public long getArrEstimatedTs() { return arrEstimatedTs; }
    public void setArrEstimatedTs(long arrEstimatedTs) { this.arrEstimatedTs = arrEstimatedTs; }

    public String getArrActual() { return arrActual; }
    public void setArrActual(String arrActual) { this.arrActual = arrActual; }

    public long getArrActualTs() { return arrActualTs; }
    public void setArrActualTs(long arrActualTs) { this.arrActualTs = arrActualTs; }

    public Integer getArrDelayed() { return arrDelayed; }
    public void setArrDelayed(Integer arrDelayed) { this.arrDelayed = arrDelayed; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getAircraftIcao() { return aircraftIcao; }
    public void setAircraftIcao(String aircraftIcao) { this.aircraftIcao = aircraftIcao; }

    public String getRegNumber() { return regNumber; }
    public void setRegNumber(String regNumber) { this.regNumber = regNumber; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
