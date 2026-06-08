package com.example.financialdatawarehouse.service;

import com.example.financialdatawarehouse.model.TimeSeriesData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TemporalSelectors {
    private TemporalSelectors() {
    }

    public static List<TimeSeriesData> latestPerBusinessDate(List<TimeSeriesData> versions) {
        List<TimeSeriesData> sorted = new ArrayList<>(versions);
        sorted.sort(Comparator
                .comparing(TimeSeriesData::getBusinessDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TimeSeriesData::getSystemDate, Comparator.nullsLast(Comparator.reverseOrder())));

        Map<LocalDate, TimeSeriesData> latestByBusinessDate = new LinkedHashMap<>();
        for (TimeSeriesData record : sorted) {
            LocalDate businessDate = record.getBusinessDate();
            if (businessDate == null || latestByBusinessDate.containsKey(businessDate)) {
                continue;
            }
            latestByBusinessDate.put(businessDate, record.isDeletedMarker() ? null : record);
        }

        return latestByBusinessDate.values().stream()
                .filter(record -> record != null)
                .toList();
    }
}
