package com.joshuaogwang.mzalendopos.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "audits")
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String entityName;
    private String entityId;
    private String action;
    private String oldData;
    private String newData;
    @CreatedDate
    private LocalDate timeStamp;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private User user;

    @Override
    public String toString() {
        // create JSON object
        return "{\"id\":\"" + id + "\",\"entityName\":\"" + entityName + "\",\"entityId\":\"" + entityId + "\",\"action\":\"" + action + "\",\"oldData\":\"" + oldData + "\",\"newData\":\"" + newData + "\",\"timeStamp\":\"" + timeStamp + "\",\"user\":\"" + user + "\"}";
    }
}
