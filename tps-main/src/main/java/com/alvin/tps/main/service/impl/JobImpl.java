package com.alvin.tps.main.service.impl;

import com.alvin.tps.main.entity.Request;
import com.alvin.tps.main.service.Job;
import com.alvin.tps.main.util.HttpUtil;
import com.alvin.tps.main.util.ListUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class JobImpl implements Job {

    private List<String> queryList = new ArrayList<>();
    private List<String> modelNames = new ArrayList<>();

    private Integer queryNum;

    @Value("${url}")
    private String url;

    public JobImpl() {
        try {
            String queryPath = "tps_data/queryList.txt";
            String encode = "utf-8";
            String absolutePath = new File(queryPath).getAbsolutePath();
            System.out.println(absolutePath);
            queryList = FileUtils.readLines(new File(queryPath), encode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute() throws Exception {
        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("modelName", ListUtil.getRandomOne(modelNames));
            map.put("queryStr", ListUtil.getRandomOne(queryList));
            String s = new HttpUtil().setUrlStr(url).jsonPost(map);
            //System.out.println(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(Request request) {
        this.modelNames = request.getModelNames();
        this.queryNum = request.getQueryNum();
    }
}
