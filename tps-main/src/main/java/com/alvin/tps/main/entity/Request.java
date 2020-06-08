package com.alvin.tps.main.entity;

import lombok.Data;

import java.util.List;

@Data
public class Request {
    private Integer threads_start;
    private Integer threads_to;
    private Integer threads_step; // 线程每次增加多少
    private Integer times; // 每次并发维持时间
    private List<String> modelNames;
    private Integer queryNum;
}
