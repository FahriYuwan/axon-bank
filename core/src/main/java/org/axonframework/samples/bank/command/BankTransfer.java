package org.axonframework.samples.bank.command;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCompletedEvent;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCreatedEvent;
import org.axonframework.samples.bank.api.banktransfer.BankTransferFailedEvent;
import org.axonframework.samples.bank.api.banktransfer.CreateBankTransferCommand;
import org.axonframework.samples.bank.api.banktransfer.MarkBankTransferCompletedCommand;
import org.axonframework.samples.bank.api.banktransfer.MarkBankTransferFailedCommand;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.cloud.sleuth.Tracer;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Aggregate
public class BankTransfer {

    @AggregateIdentifier
    private String bankTransferId;
    private String sourceBankAccountId;
    private String destinationBankAccountId;
    private long amount;
    private Status status;
    
    // Transient tracer untuk custom instrumentation
    private transient Tracer tracer;

    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    protected BankTransfer() {
    }

    @CommandHandler
    public BankTransfer(CreateBankTransferCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "createTransfer");
        }
        apply(new BankTransferCreatedEvent(
                command.getBankTransferId(),
                command.getSourceBankAccountId(),
                command.getDestinationBankAccountId(),
                command.getAmount()));
    }

    @CommandHandler
    public void handle(MarkBankTransferCompletedCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "markTransferCompleted");
        }
        apply(new BankTransferCompletedEvent(command.getBankTransferId()));
    }

    @CommandHandler
    public void handle(MarkBankTransferFailedCommand command) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "markTransferFailed");
        }
        apply(new BankTransferFailedEvent(command.getBankTransferId()));
    }

    @EventHandler
    public void on(BankTransferCreatedEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("event", "BankTransferCreated");
        }
        this.bankTransferId = event.getBankTransferId();
        this.sourceBankAccountId = event.getSourceBankAccountId();
        this.destinationBankAccountId = event.getDestinationBankAccountId();
        this.amount = event.getAmount();
        this.status = Status.STARTED;
    }

    @EventHandler
    public void on(BankTransferCompletedEvent event) {
        this.status = Status.COMPLETED;
    }

    @EventHandler
    public void on(BankTransferFailedEvent event) {
        this.status = Status.FAILED;
    }

    public String getSourceBankAccountId() {
        return sourceBankAccountId;
    }

    public String getDestinationBankAccountId() {
        return destinationBankAccountId;
    }

    public long getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    private enum Status {
        STARTED,
        FAILED,
        COMPLETED
    }
}