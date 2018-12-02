/**
    Copyright (C) 2014-2018 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.calculator;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Location
 */
public class Location
{
    public static String pattern_latLon = "#.#####";

    private String label;
    private String latitude;   // decimal degrees (DD)
    private String longitude;  // decimal degrees (DD)
    private String altitude;   // meters above the WGS 84 reference ellipsoid
    private boolean useAltitude = true;

    /**
     * @param latitude decimal degrees (DD) string
     * @param longitude decimal degrees (DD) string
     */
    public Location(String latitude, String longitude )
    {
        this(null, latitude, longitude, null, WidgetSettings.LengthUnit.METRIC);
    }

    /**
     * @param label display name
     * @param latitude decimal degrees (DD) string
     * @param longitude decimal degrees (DD) string
     */
    public Location(String label, String latitude, String longitude )
    {
        this(label, latitude, longitude, null, WidgetSettings.LengthUnit.METRIC);
    }

    public Location(String label, String latitude, String longitude, String altitude )
    {
        this(label, latitude, longitude, altitude, WidgetSettings.LengthUnit.METRIC);
    }

    /**
     * @param label display name
     * @param latitude decimal degrees (DD) string
     * @param longitude decimal degrees (DD) string
     * @param altitude meters string
     */
    public Location(String label, String latitude, String longitude, String altitude, WidgetSettings.LengthUnit altitudeUnits )
    {
        this.label = (label == null) ? "" : label;
        this.latitude = latitude;
        this.longitude = longitude;

        if (altitudeUnits != null)
        {
            switch (altitudeUnits)
            {
                case IMPERIAL:
                case USC:
                    this.altitude = Double.toString(WidgetSettings.LengthUnit.feetToMeters(Double.parseDouble(altitude)));
                    break;

                case METRIC:
                    this.altitude = altitude;
                    break;
            }
        } else {
            this.altitude = "";
        }
    }

    /**
     * @param label display name
     * @param location an android.location.Location object (that might be obtained via GPS or otherwise)
     */
    public Location(String label, @NonNull android.location.Location location )
    {
        double rawLatitude = location.getLatitude();
        double rawLongitude = location.getLongitude();
        double rawAltitude = location.getAltitude();

        DecimalFormat formatter = decimalDegreesFormatter();

        this.label = label;
        this.latitude = formatter.format(rawLatitude);
        this.longitude = formatter.format(rawLongitude);
        this.altitude = rawAltitude + "";
    }

    /**
     * @return a user-defined display label / location name
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @return latitude in decimal degrees (DD)
     */
    public String getLatitude()
    {
        return latitude;
    }

    public Double getLatitudeAsDouble()
    {
        double latitudeDouble = Double.parseDouble(latitude);
        if (latitudeDouble > 90 || latitudeDouble < -90)
        {
            double s = Math.signum(latitudeDouble);
            double adjusted = (s * 90) - (latitudeDouble % (s * 90));
            Log.w("Location", "latitude is out of range! adjusting.. " + latitudeDouble + " -> " + adjusted);
            latitudeDouble = adjusted;
        }
        return latitudeDouble;
    }

    /**
     * @return longitude in decimal degrees (DD)
     */
    public String getLongitude()
    {
        return longitude;
    }

    public Double getLongitudeAsDouble()
    {
        Double longitudeDouble = Double.parseDouble(longitude);
        if (longitudeDouble > 180 || longitudeDouble < -180)
        {
            double s = Math.signum(longitudeDouble);
            double adjusted = (longitudeDouble % (s * 180)) - (s * 180);
            Log.w("Location", "longitude is out of range! adjusting.. " + longitudeDouble + " -> " + adjusted);
            longitudeDouble = adjusted;
        }
        if (longitudeDouble == 180d) {
            longitudeDouble = -180d;
        }
        return longitudeDouble;
    }

    /**
     * @return altitude in meters
     */
    public String getAltitude()
    {
        return altitude;
    }

    public Double getAltitudeAsDouble()
    {
        if (!useAltitude || altitude.isEmpty())
            return 0.0;
        else return Double.parseDouble(altitude);
    }
    public Integer getAltitudeAsInteger()
    {
        if (!useAltitude || altitude.isEmpty())
            return 0;
        else return getAltitudeAsDouble().intValue();
    }
    public void setUseAltitude( boolean enabled )
    {
        useAltitude = enabled;
    }

    /**
     * @return a "geo" URI describing this Location
     */
    public Uri getUri()
    {
        String uriString = "geo:" + latitude + "," + longitude;
        if (!altitude.isEmpty())
        {
            uriString += "," + altitude;
        }
        return Uri.parse(uriString);
    }

    /**
     * @return a decimal degrees string "latitude, longitude" describing this location
     */
    public String toString()
    {
        return latitude + ", " + longitude;
    }

    /**
     * @param obj another Location object
     * @return true the locations are the same (label, lat, lon, and alt), false they are different somehow
     */
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Location))
        {
            return false;
        } else {
            Location that = (Location)obj;
            return (this.getLabel().equals(that.getLabel()))
                    && (this.getLatitude().equals(that.getLatitude()))
                    && (this.getLongitude().equals(that.getLongitude()))
                    && (this.getAltitude().equals(that.getAltitude()));
        }
    }

    public static DecimalFormat decimalDegreesFormatter()
    {
        DecimalFormat formatter = (DecimalFormat)(NumberFormat.getNumberInstance(Locale.US));
        formatter.applyLocalizedPattern(pattern_latLon);
        return formatter;
    }
}
