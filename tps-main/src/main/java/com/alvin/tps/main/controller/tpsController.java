package com.alvin.tps.main.controller;

import com.alvin.tps.main.entity.Request;
import com.alvin.tps.main.service.Job;
import com.alvin.tps.main.service.TpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class tpsController {

    @Autowired
    private Job job;

    private TpsService tpsService;

    @PostMapping("/")
    public String execJob(@RequestBody Request request) throws IOException {
        if (tpsService != null && tpsService.running) {
            return "等一会再跑任务吧";
        }

        new Thread(() -> {
            try {
                for (Integer i = request.getThreads_start(); i < request.getThreads_to(); i += request.getThreads_step()) {
                    tpsService = new TpsService(i, request.getTimes());
                    job.init(request);
                    tpsService.run(job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ).start();
        return "任务已启动，请查看日志";
    }

}
