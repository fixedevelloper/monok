package com.monokek.notifications.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SseTicketServiceTest {

    @Test
    void issuedTicketConsumesToTheBranchItWasIssuedFor() {
        SseTicketService service = new SseTicketService();

        String ticket = service.issue(7L);

        assertThat(service.consume(ticket)).contains(7L);
    }

    @Test
    void ticketIsSingleUse() {
        SseTicketService service = new SseTicketService();
        String ticket = service.issue(7L);

        service.consume(ticket);

        assertThat(service.consume(ticket)).isEmpty();
    }

    @Test
    void unknownTicketIsRejected() {
        SseTicketService service = new SseTicketService();

        assertThat(service.consume("never-issued")).isEmpty();
    }

    @Test
    void expiredTicketIsRejected() throws Exception {
        SseTicketService service = new SseTicketService(Duration.ofMillis(20));
        String ticket = service.issue(7L);

        Thread.sleep(50);

        assertThat(service.consume(ticket)).isEmpty();
    }
}
