package com.lucasdominato.securefilemanager.mapper;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DateMapper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public String asString(LocalDate date) {
        return date != null ? date.format(formatter) : null;
    }

    public LocalDate asLocalDate(String date) {
        return date != null ? LocalDate.parse(date, formatter) : null;
    }
}