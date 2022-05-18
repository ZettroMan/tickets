package ru.zettro.model;

import java.util.List;

public class TicketList {
    private List<Ticket> tickets;

    public List<Ticket> getTickets() {
        return tickets;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(Ticket ticket: tickets)
        {
            stringBuilder.append(ticket.toString()).append("\n");
        }
        return "TicketList: \n" + stringBuilder;
    }
}
