package com.pullapart.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Locations {

    private int locationID;
    private String locationName;
    private String address1;
    private String address2;
    private String cityName;
    private String stateName;
    private String zip;
    private int siteTypeID;

    public Locations() {

    }

    public Locations(int locationID, String locationName, String address1, String address2, String cityName, String stateName, String zip,
                    int siteTypeID) {

        this.locationID = locationID;
        this.locationName = locationName;
        this.address1 = address1;
        this.address2 = address2;
        this.cityName = cityName;
        this.stateName = stateName;
        this.zip = zip;
        this.siteTypeID = siteTypeID;
    }
    // Getters & Setters
    public int getLocationID() { return locationID; }
    public void setLocationID(int locationID) { this.locationID = locationID; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }

    public void setAddress2(String address2) { this.address2 = address2; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public int getSiteTypeID() { return siteTypeID; }
    public void setSiteTypeID(int siteTypeID) { this.siteTypeID = siteTypeID; }

}
