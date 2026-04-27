package com.dsc.sharededitor.domain.crdt;

import java.util.Objects;

public class YItemId {

    private String clientId;
    private long clock;

    public YItemId() {}

    public YItemId(String clientId, long clock) {
        this.clientId = clientId;
        this.clock = clock;
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public long getClock() { return clock; }
    public void setClock(long clock) { this.clock = clock; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YItemId other)) return false;
        return clock == other.clock && Objects.equals(clientId, other.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, clock);
    }

    @Override
    public String toString() {
        return clientId + "@" + clock;
    }
}
