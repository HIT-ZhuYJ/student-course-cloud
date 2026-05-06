package com.yun.studentcourse.common.dto;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {

    private List<T> records;
    private long total;
    private long pageNo;
    private long pageSize;

    public PageResult() {
        this.records = Collections.emptyList();
    }

    public PageResult(List<T> records, long total, long pageNo, long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        return new PageResult<>(records, total, pageNo, pageSize);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }
}
