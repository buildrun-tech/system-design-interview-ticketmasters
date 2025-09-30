package tech.buildrun.controller.dto;

import tech.buildrun.entity.SeatEntity;

public record SeatDto(Long seatId, String name, String status) {

    public static SeatDto fromEntity(SeatEntity seatEntity) {
        return new SeatDto(
                seatEntity.id,
                seatEntity.name,
                seatEntity.status.name()
        );
    }
}
