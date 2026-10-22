package cn.bitoffer.mqtry.service;

import cn.bitoffer.mqtry.model.Count;

public interface CountService {

    void save(Count example);

    void msgAll(String svrName, String msg);

    void incrCount(Integer num);
    void flowArrived();
    void incrManyTimes(Integer num);




}
