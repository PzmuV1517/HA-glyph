package com.PzmuV1517.ha_glyph.model;

public class HomeAssistantEntity {
    private String entity_id;
    private String state;
    private Attributes attributes;

    public static class Attributes {
        private String friendly_name;
        private String device_class;

        public String getFriendlyName() {
            return friendly_name;
        }

        public String getDeviceClass() {
            return device_class;
        }
    }

    public String getEntityId() {
        return entity_id;
    }

    public String getState() {
        return state;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public String getFriendlyName() {
        return attributes != null ? attributes.getFriendlyName() : entity_id;
    }

    public boolean isOn() {
        return "on".equals(state);
    }
}
