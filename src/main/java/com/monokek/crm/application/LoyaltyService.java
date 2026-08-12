package com.monokek.crm.application;

import com.monokek.common.ApiException;
import com.monokek.crm.domain.Customer;
import com.monokek.crm.domain.CustomerRepository;
import com.monokek.crm.domain.LoyaltyTransaction;
import com.monokek.crm.domain.LoyaltyTransactionRepository;
import com.monokek.crm.web.dto.LoyaltyTransactionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * New functionality: the {@code customers.points} balance plus its
 * {@code loyalty_transactions} ledger — the schema clearly supports an
 * earn/redeem loyalty scheme (a balance column and a typed transaction log),
 * but nothing in the Laravel source ever wrote to either.
 */
@Service
public class LoyaltyService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    public LoyaltyService(CustomerRepository customerRepository, LoyaltyTransactionRepository loyaltyTransactionRepository) {
        this.customerRepository = customerRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionDto> history(Long customerId) {
        return loyaltyTransactionRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    @Transactional
    public int earn(Long customerId, int points) {
        Customer customer = findOrThrow(customerId);
        customer.earnPoints(points);
        customerRepository.save(customer);
        record(customer, points, "earn");
        return customer.getPoints();
    }

    @Transactional
    public int redeem(Long customerId, int points) {
        Customer customer = findOrThrow(customerId);
        customer.redeemPoints(points);
        customerRepository.save(customer);
        record(customer, points, "redeem");
        return customer.getPoints();
    }

    private void record(Customer customer, int points, String type) {
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setCustomer(customer);
        transaction.setPoints(points);
        transaction.setType(type);
        loyaltyTransactionRepository.save(transaction);
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Client introuvable"));
    }

    private LoyaltyTransactionDto toDto(LoyaltyTransaction transaction) {
        return new LoyaltyTransactionDto(
                transaction.getId(), transaction.getPoints(), transaction.getType(),
                transaction.getCreatedAt() == null ? null : transaction.getCreatedAt().format(DATE_TIME));
    }
}
