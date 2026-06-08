package com.example.financialdatawarehouse;

import com.example.financialdatawarehouse.model.TimeSeriesData;
import com.example.financialdatawarehouse.service.TemporalSelectors;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemporalSelectorsTests {
    @Test
    void latestSystemVersionWinsForSameBusinessDate() {
        LocalDate businessDate = LocalDate.of(2026, 6, 1);
        TimeSeriesData older = record(businessDate, "2026-06-01T10:00:00Z", 100.0, false);
        TimeSeriesData newer = record(businessDate, "2026-06-01T11:00:00Z", 110.0, false);

        List<TimeSeriesData> selected = TemporalSelectors.latestPerBusinessDate(List.of(older, newer));

        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getValues()).containsEntry("close", 110.0);
    }

    @Test
    void latestDeletionMarkerSuppressesOlderValue() {
        LocalDate businessDate = LocalDate.of(2026, 6, 1);
        TimeSeriesData older = record(businessDate, "2026-06-01T10:00:00Z", 100.0, false);
        TimeSeriesData deletion = record(businessDate, "2026-06-01T11:00:00Z", 0.0, true);

        List<TimeSeriesData> selected = TemporalSelectors.latestPerBusinessDate(List.of(older, deletion));

        assertThat(selected).isEmpty();
    }

    private TimeSeriesData record(LocalDate businessDate, String systemDate, double close, boolean deletedMarker) {
        TimeSeriesData record = new TimeSeriesData();
        record.setAssetId("BTCUSD");
        record.setDataSourceId("NASDAQ-DATA-LINK.QDL/BITFINEX");
        record.setBusinessDate(businessDate);
        record.setBusinessYear(businessDate.getYear());
        record.setSystemDate(Instant.parse(systemDate));
        record.setValues(Map.of("close", close));
        record.setDeletedMarker(deletedMarker);
        return record;
    }
}
