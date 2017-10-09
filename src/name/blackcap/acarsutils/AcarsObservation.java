package name.blackcap.acarsutils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Represents a single ACARS weather observation. Note that an ACARS
 * message can contain multiple observations. The silly mishmash of
 * units here is the same one the observations use. Don't blame me for
 * the inconsistency.
 * @author David Barts <n5jrn@me.com>
 */
public class AcarsObservation {
    /* Data fields. All are objects, so they can be null. */

    /* date and time the observation was made */
    private Date observed;
    public Date getObserved() {
        return observed;
    }
    public AcarsObservation setObserved(Date observed) {
        if (observed == null)
            throw new IllegalArgumentException("Invalid observation time: null");
        this.observed = observed;
        return this;
    }

    /* altitude of the observation, in feet */
    private Integer altitude;
    public Integer getAltitude() {
        return altitude;
    }
    public AcarsObservation setAltitude(Integer altitude) {
        if (altitude == null)
            throw new IllegalArgumentException("Invalid altitude: null");
        this.altitude = altitude;
        return this;
    }

    /* wind speed in knots */
    private Short windSpeed;
    public Short getWindSpeed() {
        return windSpeed;
    }
    public AcarsObservation setWindSpeed(Short windSpeed) {
        this.windSpeed = windSpeed;
        return this;
    }

    /* wind direction in compass degrees */
    private Short windDirection;
    public Short getWindDirection() {
        return windDirection;
    }
    public AcarsObservation setWindDirection(Short windDirection) {
        if (windDirection != null && (windDirection > 360 || windDirection < 0))
            throw new IllegalArgumentException("Invalid wind direction: " + windDirection);
        this.windDirection = windDirection;
        return this;
    }

    /* temperature in degrees Celsius */
    private Float temperature;
    public Float getTemperature() {
        return temperature;
    }
    public AcarsObservation setTemperature(Float temperature) {
        this.temperature = temperature;
        return this;
    }

    /* latitude and longitude */
    private Double latitude;
    public Double getLatitude() {
        return latitude;
    }
    public AcarsObservation setLatitude(Double latitude) {
        if (latitude == null || latitude > 90.0 || latitude < -90.0)
            throw new IllegalArgumentException("Invalid latitude: " + latitude);
        this.latitude = latitude;
        return this;
    }
    private Double longitude;
    public Double getLongitude() {
        if (longitude == null || longitude > 180.0 || longitude < -180.0)
            throw new IllegalArgumentException("Invalid longitude: " + longitude);
        return longitude;
    }
    public AcarsObservation setLongitude(Double longitude) {
        this.longitude = longitude;
        return this;
    }

    /**
     * Constructor. Must specify enough to locate the observation in space
     * and time (observations that cannot be are useless).
     * @param latitude    Latitude.
     * @param longitude   Longitude.
     * @param altitude    Altitude in feet.
     * @param observed    Time observed.
     */
    public AcarsObservation(double latitude, double longitude, int altitude, Date observed) {
        setLatitude(latitude);
        setLongitude(longitude);
        setAltitude(altitude);
        setObserved(observed);
    }

    /**
     * Two observations are equal if all their fields match.
     * @param value       Observation to compare to.
     * @return            Boolean.
     */
    public boolean equals(AcarsObservation value) {
        return this == value || (
               eq(getObserved(), value.getObserved()) &&
               eq(getAltitude(), value.getAltitude()) &&
               eq(getWindSpeed(), value.getWindSpeed()) &&
               eq(getWindDirection(), value.getWindDirection()) &&
               eq(getTemperature(), value.getTemperature()) &&
               eq(getLatitude(), value.getLatitude()) &&
               eq(getLongitude(), value.getLongitude()));
    }

    private boolean eq(Object o1, Object o2) {
        return o1 == o2 || o1.equals(o2);
    }

    private static final SimpleDateFormat UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        UTC.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Return a string representation of this object.
     * @return string
     */
    public String toString() {
        return String.format(
            "%s: obs=%s, alt=%d, wsp=%d, wdi=%d, tmp=%.1f, lat=%.3f, lon=%.3f",
            getClass().getName(), UTC.format(observed), altitude, windSpeed,
            windDirection, temperature, latitude, longitude);
    }
}