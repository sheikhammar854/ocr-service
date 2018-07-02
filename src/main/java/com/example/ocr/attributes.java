package com.example.ocr;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
@Table(name="attributes")
public class attributes {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String attribute;
    @Column(length = 8000)
    private String value;

    @ManyToOne
    @JoinColumn (name="doc_id")
    @JsonBackReference
    private document doc;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public document getDoc() {
        return doc;
    }

    public void setDoc(document doc) {
        this.doc = doc;
    }
}
