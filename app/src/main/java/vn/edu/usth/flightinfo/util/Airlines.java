package vn.edu.usth.flightinfo.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Airline code lookups: ICAO callsign prefix -> IATA code, IATA code -> display name and logo. */
public final class Airlines {

    private static final Map<String, String> ICAO_TO_IATA = new HashMap<>();
    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        add("HVN", "VN", "Vietnam Airlines");
        add("VJC", "VJ", "VietJet Air");
        add("BAV", "QH", "Bamboo Airways");
        add("PIC", "BL", "Pacific Airlines");
        add("VAG", "VU", "Vietravel Airlines");
        add("THA", "TG", "Thai Airways");
        add("AIQ", "FD", "Thai AirAsia");
        add("TVJ", "VZ", "Thai Vietjet Air");
        add("SIA", "SQ", "Singapore Airlines");
        add("TGW", "TR", "Scoot");
        add("AXM", "AK", "AirAsia");
        add("MAS", "MH", "Malaysia Airlines");
        add("KAL", "KE", "Korean Air");
        add("AAR", "OZ", "Asiana Airlines");
        add("JJA", "7C", "Jeju Air");
        add("JNA", "LJ", "Jin Air");
        add("TWB", "TW", "T'way Air");
        add("JAL", "JL", "Japan Airlines");
        add("ANA", "NH", "All Nippon Airways");
        add("CPA", "CX", "Cathay Pacific");
        add("EVA", "BR", "EVA Air");
        add("CAL", "CI", "China Airlines");
        add("CES", "MU", "China Eastern");
        add("CSN", "CZ", "China Southern");
        add("CCA", "CA", "Air China");
        add("QTR", "QR", "Qatar Airways");
        add("UAE", "EK", "Emirates");
        add("THY", "TK", "Turkish Airlines");
        add("AFR", "AF", "Air France");
        add("DLH", "LH", "Lufthansa");
        add("BAW", "BA", "British Airways");
        add("QFA", "QF", "Qantas");
        add("PAL", "PR", "Philippine Airlines");
        add("GIA", "GA", "Garuda Indonesia");
        add("LAO", "QV", "Lao Airlines");
        add("KME", "K6", "Cambodia Angkor Air");
    }

    private Airlines() {
    }

    private static void add(String icao, String iata, String name) {
        ICAO_TO_IATA.put(icao, iata);
        NAMES.put(iata, name);
    }

    /** Display name for an IATA airline code, or the code itself when unknown. */
    public static String name(String iata) {
        if (iata == null) {
            return null;
        }
        String name = NAMES.get(iata.toUpperCase(Locale.US));
        return name != null ? name : iata;
    }

    /** Logo image URL for an IATA airline code. */
    public static String logoUrl(String iata) {
        return iata == null ? null : "https://pics.avs.io/200/200/" + iata.toUpperCase(Locale.US) + ".png";
    }

    /**
     * Converts an ATC callsign to an IATA flight number: "HVN215 " -> "VN215".
     * Returns null when the airline prefix is unknown or there is no flight number.
     */
    public static String callsignToFlightIata(String callsign) {
        if (callsign == null) {
            return null;
        }
        String cs = callsign.trim().toUpperCase(Locale.US);
        if (cs.length() < 4) {
            return null;
        }
        String iata = ICAO_TO_IATA.get(cs.substring(0, 3));
        String number = cs.substring(3).replaceFirst("^0+(?=\\d)", "");
        if (iata == null || number.isEmpty()) {
            return null;
        }
        return iata + number;
    }
}
