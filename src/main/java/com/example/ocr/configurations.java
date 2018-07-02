package com.example.ocr;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class configurations {
    @Id
    @NotNull
    private String from_doc;
    @Column
    private int line_no;
    @Column
    private int amount_due_index_start;
    @Column
    private int amount_due_index_end;

    public int getAmount_due_index_start() {
        return amount_due_index_start;
    }

    public void setAmount_due_index_start(int amount_due_index_start) {
        this.amount_due_index_start = amount_due_index_start;
    }

    public int getAmount_due_index_end() {
        return amount_due_index_end;
    }

    public void setAmount_due_index_end(int amount_due_index_end) {
        this.amount_due_index_end = amount_due_index_end;
    }

    public String getFrom_company() {
        return from_doc;
    }

    public void setFrom_company(String from_company) {
        this.from_doc = from_company;
    }

    public int getLine_no() {
        return line_no;
    }

    public void setLine_no(int line_no) {
        this.line_no = line_no;
    }
}
