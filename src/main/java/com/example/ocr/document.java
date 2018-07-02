package com.example.ocr;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="document")
public class document {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    @Column
    private String name;


    @OneToMany(mappedBy = "doc",fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<attributes> attr = new HashSet<attributes>();

    public void addAttribute(attributes a){
        this.attr.add(a);
    }

    public Set<attributes> getAttributes() {
        return attr;
    }

    public void setAttributes(Set<attributes> attributes) {
        this.attr = attributes;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



}