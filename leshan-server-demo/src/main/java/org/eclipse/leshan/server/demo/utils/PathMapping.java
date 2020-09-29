package org.eclipse.leshan.server.demo.utils;

import java.util.HashMap;
import java.util.Map;

public enum PathMapping {

    //Local Testing Purpose
    TEMPERATURE_SENSOR_VALUE  ("/3303/0/5700"),
    TEMPERATURE_SENSOR_UNITS  ("/3303/0/5701"),
    TEMPERATURE_MIN_MEASURED_VALUE  ("/3303/0/5601"),
    TEMPERATURE_MAX_MEASURED_VALUE  ("/3303/0/5602"),

    //EQL DICE POC Purpose
    INTERNAL_DATA_DELIVERY_LP  ("/10262/0/2"),
    WATER_FLOW_READINGS_INSTANCE_0_LP ("/10266/0/6029"),
    WATER_FLOW_READINGS_INSTANCE_1_LP ("/10266/1/6029"),
    DAILY_MAX_FLOW_RATE_READING_LP ("/10267/0/6029"),
    TEMPERATURE_READINGS_LP ("/10268/0/6029"),
    PRESSURE_READINGS_LP ("/10269/0/6029"),
    BATTERY_LEVEL_READINGS_LP ("/10270/0/6029"),
    COMMUNICATIONS_ACTIVITY_TIME_READINGS_LP ("/10271/0/6029"),
    EVENT_DATA_DELIVERY_LEL ("/10263/0/2"),
    WATER_METER_CUSTOMER_LEAKAGE_ALARM_LP ("/10272/0/6025"),
    WATER_METER_REVERSE_FLOW_ALARM_INSTANCE_0_LP ("/10273/0/6025"),
    WATER_METER_REVERSE_FLOW_ALARM_INSTANCE_1_LP ("/10273/1/6025"),
    WATER_METER_EMPTY_PIPE_ALARM_LP ("/10274/0/6025"),
    WATER_METER_TAMPER_ALARM_LP ("/10275/0/6025"),
    WATER_METER_HIGH_PRESSURE_ALARM_LP ("/10276/0/6025"),
    WATER_METER_LOW_PRESSURE_ALARM_LP ("/10277/0/6025"),
    HIGH_TEMPERATURE_ALARM_LP ("/10278/0/6025"),
    LOW_TEMPERATURE_ALARM_LP ("/10279/0/6025"),
    WATER_NETWORK_LEAK_ALARM_LP ("/10280/0/6025"),
    LOW_BATTERY_ALARM_LP ("/10281/0/6025"),
    DAUGHTER_BOARD_FAILURE_ALARM_LP ("/10282/0/6025"),
    DEVICE_REBOOT_EVENT_LP ("/10283/0/6025"),
    TIME_SYNCHRONISATION_EVENT_LP ("/10284/0/6025")
    ;

    private final String pathCode;

    PathMapping(String pathCode) { this.pathCode = pathCode; }

    public static PathMapping getPathMapping(String pathCode) {
        for (PathMapping l : PathMapping.values()) {
            if (l.pathCode.equals(pathCode)) return l;
        }
        throw new IllegalArgumentException("PathMapping not found. Amputated?");
    }

    public String getPathCode() {
        return pathCode;
    }
}
