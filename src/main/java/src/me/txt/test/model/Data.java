package src.me.txt.test.model;

import java.util.Objects;
import java.util.UUID;

public class Data {

    private UUID deviceId;
    private Long unixTime;
    private String payload; // suppose it will be Base64 encoded string


    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public Long getUnixTime() {
        return unixTime;
    }

    public void setUnixTime(Long unixTime) {
        this.unixTime = unixTime;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Objects.equals(deviceId, data.deviceId) && Objects.equals(unixTime, data.unixTime) && Objects.equals(payload, data.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, unixTime, payload);
    }

    @Override
    public String toString() {
        return "Data{" +
                "deviceId=" + deviceId +
                ", unixTime=" + unixTime +
                ", payload='" + payload + '\'' +
                '}';
    }
}
