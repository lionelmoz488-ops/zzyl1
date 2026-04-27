package com.zzyl.nursing.task;

import com.zzyl.nursing.service.IReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateReservationStatusJob {
    @Autowired
    private IReservationService reservationService;

    public void updateReservationStatus() {
        reservationService.updateReservationStatus();
    }
}
