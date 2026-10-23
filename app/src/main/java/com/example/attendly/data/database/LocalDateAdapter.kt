package com.example.attendly.data.database

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek

class LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value?.toString())
    }
    override fun read(reader: JsonReader): LocalDate? {
        return LocalDate.parse(reader.nextString())
    }
}

class LocalTimeAdapter : TypeAdapter<LocalTime>() {
    override fun write(out: JsonWriter, value: LocalTime?) {
        out.value(value?.toString())
    }
    override fun read(reader: JsonReader): LocalTime? {
        return LocalTime.parse(reader.nextString())
    }
}

class DayOfWeekAdapter : TypeAdapter<DayOfWeek>() {
    override fun write(out: JsonWriter, value: DayOfWeek?) {
        out.value(value?.name)
    }
    override fun read(reader: JsonReader): DayOfWeek? {
        return DayOfWeek.valueOf(reader.nextString())
    }
}
