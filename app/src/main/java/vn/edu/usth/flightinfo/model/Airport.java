package vn.edu.usth.flightinfo.model;

/** A static airport entry from assets/airports.json. */
public class Airport {

    private final String iata;
    private final String icao;
    private final String name;
    private final String city;
    private final String cityVi;
    private final String country;
    private final double lat;
    private final double lon;

    public Airport(String iata, String icao, String name, String city, String cityVi,
                   String country, double lat, double lon) {
        this.iata = iata;
        this.icao = icao;
        this.name = name;
        this.city = city;
        this.cityVi = cityVi;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
    }

    public String getIata() { return iata; }
    public String getIcao() { return icao; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }

    /** City name in Vietnamese when requested and available, otherwise in English. */
    public String getCity(boolean vietnamese) {
        return vietnamese && cityVi != null ? cityVi : city;
    }

    /** Case-insensitive match against code, name and city (for the airport filter box). */
    public boolean matches(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return iata.toLowerCase().contains(q)
                || (icao != null && icao.toLowerCase().contains(q))
                || name.toLowerCase().contains(q)
                || city.toLowerCase().contains(q)
                || (cityVi != null && cityVi.toLowerCase().contains(q));
    }
}
