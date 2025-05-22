package org.axonframework.samples.bank.command;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.samples.bank.api.bankaccount.*;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.cloud.sleuth.Tracer;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Aggregate
public class BankAccount {

    @AggregateIdentifier
    private String id;
    private long overdraftLimit;
    private long balanceInCents;

    // Transient tracer field untuk custom span tagging
    private transient Tracer tracer;
    
    protected BankAccount() {
    }
    
    // Setter untuk tracer (jika diset secara manual)
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @CommandHandler
    public BankAccount(CreateBankAccountCommand command) {
        apply(new BankAccountCreatedEvent(command.getBankAccountId(), command.getOverdraftLimit()));
    }

    @CommandHandler
    public void deposit(DepositMoneyCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "deposit");
        }
        apply(new MoneyDepositedEvent(id, command.getAmountOfMoney()));
    }

    @CommandHandler
    public void withdraw(WithdrawMoneyCommand command) {
        if (command.getAmountOfMoney() <= balanceInCents + overdraftLimit) {
            if (tracer != null && tracer.getCurrentSpan() != null) {
                tracer.getCurrentSpan().tag("operation", "withdraw");
            }
            apply(new MoneyWithdrawnEvent(id, command.getAmountOfMoney()));
        }
    }

    public void debit(long amount, String bankTransferId) {
        if (amount <= balanceInCents + overdraftLimit) {
            if (tracer != null && tracer.getCurrentSpan() != null) {
                tracer.getCurrentSpan().tag("operation", "debit");
            }
            apply(new SourceBankAccountDebitedEvent(id, amount, bankTransferId));
        } else {
            apply(new SourceBankAccountDebitRejectedEvent(bankTransferId));
        }
    }

    public void credit(long amount, String bankTransferId) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "credit");
        }
        apply(new DestinationBankAccountCreditedEvent(id, amount, bankTransferId));
    }

    @CommandHandler
    public void returnMoney(ReturnMoneyOfFailedBankTransferCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "returnMoney");
        }
        apply(new MoneyOfFailedBankTransferReturnedEvent(id, command.getAmount()));
    }

    @EventSourcingHandler
    public void on(BankAccountCreatedEvent event) {
        this.id = event.getId();
        this.overdraftLimit = event.getOverdraftLimit();
        this.balanceInCents = 0;
    }

    @EventSourcingHandler
    public void on(MoneyAddedEvent event) {
        balanceInCents += event.getAmount();
    }

    @EventSourcingHandler
    public void on(MoneySubtractedEvent event) {
        balanceInCents -= event.getAmount();
    }
}