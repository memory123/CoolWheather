package com.example.memory.coolwheather.db;

import org.litepal.crud.DataSupport;

/**
 * Created by memory on 2017/12/26.
 */

public class Province extends DataSupport{
    private int id;
    private String provinceName;
    private String privinceCode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getPrivinceCode() {
        return privinceCode;
    }

    public void setPrivinceCode(String privinceCode) {
        this.privinceCode = privinceCode;
    }
}
