package com.joshuaogwang.mzalendopos.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Supplier name is required")
    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String contactPerson;

    @Column(nullable = true)
    private String phone;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = true)
    private String address;

    @Column(nullable = true)
    private String tin;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<PurchaseOrder> purchaseOrders = new ArrayList<>();
}
