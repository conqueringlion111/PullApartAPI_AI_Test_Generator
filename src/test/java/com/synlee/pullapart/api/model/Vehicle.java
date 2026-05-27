package com.pullapart.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Vehicle {

    @JsonProperty("Locations")
    private List<Integer> locations;
    @JsonProperty("MakeID")
    private int makeId;
    @JsonProperty("Models")
    private List<Integer> models;
    @JsonProperty("Years")
    private List<Integer> years;

    public Vehicle() {

    }

    public Vehicle(List<Integer> locations, int makeId, List<Integer> models, List<Integer> years) {
        this.locations = locations;
        this.makeId = makeId;
        this.models = models;
        this.years = years;
    }

    public List<Integer> getLocations() {
        return locations;
    }

    public void setLocations(List<Integer> locations) {
        locations = locations;
    }

    public int getMakeId() {
        return makeId;
    }

    public void setMakeID(int makeId) {
        makeId = makeId;
    }

    public List<Integer> getModels() {
        return models;
    }

    public void setModels(List<Integer> models) {
        models = models;
    }

    public List<Integer> getYears() {
        return years;
    }

    public void setYears(List<Integer> years) {
        years = years;
    }

}
