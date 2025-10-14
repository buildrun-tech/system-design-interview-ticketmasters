package tech.buildrun.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tb_bookings", schema = "public")
public class BookingEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Enumerated(EnumType.STRING)
    public BookingStatus status;

    public Instant bookedAt;

    @CreationTimestamp
    public Instant createdAt;

    @UpdateTimestamp
    public Instant updatedAt;

}
