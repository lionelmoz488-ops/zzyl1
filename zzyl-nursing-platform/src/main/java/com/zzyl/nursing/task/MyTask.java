package com.zzyl.nursing.task;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyTask {
//    @Scheduled(cron = "0/5 * * * * ?")
    public void excuteTask() {
        log.info("定时任务开始执行，{}", LocalDateTime.now());
    }
}
