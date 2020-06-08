package com.alvin.tps.main.service;

import com.alvin.tps.main.entity.Request;

public interface Job {
    void execute() throws Exception;

    void init(Request request);
}
