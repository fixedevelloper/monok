package com.monokek.cashier.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cash_registers")
@Getter
@Setter
@NoArgsConstructor
public class CashRegister extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References company.Branch by id only — see module package-info. */
    private Long branchId;

    private String name;
}
