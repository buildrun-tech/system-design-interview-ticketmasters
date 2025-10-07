package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class TicketEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    public UUID externalId;

    @ManyToOne
    @JoinColumn(name = "seat_id")
    public SeatEntity seat;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    public BookingEntity booking;
}
