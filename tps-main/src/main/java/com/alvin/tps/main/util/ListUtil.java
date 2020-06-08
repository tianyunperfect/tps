package com.alvin.tps.main.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ListUtil<T> {
    public static <T> T getRandomOne(List<T> list) {
        checkList(list);
        return list.get(new Random().nextInt(list.size()));
    }

    /**
     * 检查list
     *
     * @param list 列表
     */
    private static <T> void checkList(List<T> list) {
        if (list == null || list.size() <= 0) {
            throw new RuntimeException("集合不能为空");
        }
    }

    public static <T> List<T> getRandomNum(List<T> list, Integer num) {
        checkList(list);
        ArrayList<T> ts = new ArrayList<>();
        Random random = new Random();
        int size = list.size();
        for (int i = 0; i < num; i++) {
            ts.add(list.get(random.nextInt(size)));
        }
        return ts;
    }
}
