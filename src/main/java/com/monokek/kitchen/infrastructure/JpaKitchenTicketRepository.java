package com.monokek.kitchen.infrastructure;

import com.monokek.kitchen.domain.KitchenTicket;
import com.monokek.kitchen.domain.KitchenTicketRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaKitchenTicketRepository extends KitchenTicketRepository, JpaRepository<KitchenTicket, Long> {
}
