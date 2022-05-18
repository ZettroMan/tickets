package ru.zettro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.zettro.adapters.LocalDateAdapter;
import ru.zettro.adapters.LocalTimeAdapter;
import ru.zettro.model.Ticket;
import ru.zettro.model.TicketList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String originAirportCode = "VVO";
        String destinationAirportCode = "TLV";
        ZoneId originTimeZone = ZoneId.of("Asia/Vladivostok");
        ZoneId destinationTimeZone = ZoneId.of("Asia/Jerusalem");
        String inFileName = "tickets.json";
        BufferedReader bufferedReader;

        try {
            bufferedReader = new BufferedReader(new FileReader(inFileName));
        } catch (FileNotFoundException e) {
            System.out.println("Нет доступа к входному файлу tickets.json или файл не найден.");
            return;
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                .create();

        TicketList ticketList = gson.fromJson(bufferedReader, TicketList.class);

        // System.out.println(ticketList);

        List<Ticket> fullTicketList = ticketList.getTickets();
        List<Ticket> filteredTicketList = fullTicketList.stream().filter(ticket ->
                (ticket.getOrigin().equals(originAirportCode) && ticket.getDestination().equals(destinationAirportCode))).toList();

        int ticketsCount = filteredTicketList.size();
        if (ticketsCount == 0) {
            System.out.println("Нет подходящих рейсов.");
            return;
        }

        List<Long> flightDurations = new ArrayList<>();

        for (Ticket ticket : filteredTicketList) {
            ZonedDateTime departDateTime = ZonedDateTime.of(ticket.getDeparture_date(), ticket.getDeparture_time(), originTimeZone);
            ZonedDateTime arrivalDateTime = ZonedDateTime.of(ticket.getArrival_date(), ticket.getArrival_time(), destinationTimeZone);
            long flightDuration = ChronoUnit.MINUTES.between(departDateTime, arrivalDateTime);
            flightDurations.add(flightDuration);
        }

        long totalFlightDurationInMinutes = flightDurations.stream().reduce(0L, Long::sum);
        long mediumFlightTimeInMinutes = totalFlightDurationInMinutes / ticketsCount;
        System.out.println("Среднее время перелёта составляет " + toHoursAndMinutes(mediumFlightTimeInMinutes));

        flightDurations.sort(null);

        if ((float) ticketsCount / (ticketsCount + 1) >= 0.9f) {
            // если выборка достаточно большая (>=9) - можно посчитать эксклюзивный 90-й процентиль
            float floatRank = 0.9f * (ticketsCount + 1) - 1;
            int rankWholePart = (int) floatRank;
            float percentileExclusive;
            if (rankWholePart == ticketsCount - 1) { // пограничный случай (интерполяция невозможна)
                percentileExclusive = flightDurations.get(rankWholePart);
            } else {
                float rankDecimalPart = floatRank - rankWholePart;
                percentileExclusive = flightDurations.get(rankWholePart) +
                        (flightDurations.get(rankWholePart + 1) - flightDurations.get(rankWholePart)) * rankDecimalPart;
            }
            System.out.printf("90-й процентиль времени полета (эксклюзивный) равен : %.2f мин. (~%s)%n",
                    percentileExclusive, toHoursAndMinutes(Math.round(percentileExclusive)));

        }

        // инклюзивный процентиль можно посчитать в любом случае
        float percentileInclusive;
        if (ticketsCount == 1) { // пограничный случай (интерполяция невозможна)
            percentileInclusive = flightDurations.get(0);
        } else {
            float floatRank = 0.9f * (ticketsCount - 1);
            int rankWholePart = (int) floatRank;
            float rankDecimalPart = floatRank - rankWholePart;
            percentileInclusive = flightDurations.get(rankWholePart) +
                    (flightDurations.get(rankWholePart + 1) - flightDurations.get(rankWholePart)) * rankDecimalPart;
        }

        System.out.printf("90-й процентиль времени полета (инклюзивный) равен : %.2f мин. (~%s)",
                percentileInclusive, toHoursAndMinutes(Math.round(percentileInclusive)));

    }

    private static String toHoursAndMinutes(long minutes) {
        long hours = minutes / 60;
        return hours + " ч. " + (minutes - (hours * 60)) + " мин.";
    }

}

