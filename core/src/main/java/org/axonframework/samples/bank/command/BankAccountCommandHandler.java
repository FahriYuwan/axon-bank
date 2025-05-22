package org.axonframework.samples.bank.command;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.Aggregate;
import org.axonframework.commandhandling.model.AggregateNotFoundException;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.samples.bank.api.bankaccount.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;

import static org.axonframework.eventhandling.GenericEventMessage.asEventMessage;

public class BankAccountCommandHandler {

    private Repository<BankAccount> repository;
    private EventBus eventBus;

    // Injeksi Tracer untuk menambahkan tag tambahan pada span
    private Tracer tracer;

    @Autowired
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    public BankAccountCommandHandler(Repository<BankAccount> repository, EventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
    }

    @CommandHandler
    public void handle(DebitSourceBankAccountCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "debitSource");
            tracer.getCurrentSpan().tag("accountId", command.getBankAccountId());
        }
        try {
            Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
            bankAccountAggregate.execute(bankAccount -> bankAccount.debit(command.getAmount(), command.getBankTransferId()));
        } catch (AggregateNotFoundException exception) {
            eventBus.publish(asEventMessage(new SourceBankAccountNotFoundEvent(command.getBankTransferId())));
        }
    }

    @CommandHandler
    public void handle(CreditDestinationBankAccountCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "creditDestination");
            tracer.getCurrentSpan().tag("accountId", command.getBankAccountId());
        }
        try {
            Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
            bankAccountAggregate.execute(bankAccount -> bankAccount.credit(command.getAmount(), command.getBankTransferId()));
        } catch (AggregateNotFoundException exception) {
            eventBus.publish(asEventMessage(new DestinationBankAccountNotFoundEvent(command.getBankTransferId())));
        }
    }
}