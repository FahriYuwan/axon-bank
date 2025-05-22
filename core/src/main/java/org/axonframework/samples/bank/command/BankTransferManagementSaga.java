package org.axonframework.samples.bank.command;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.callbacks.LoggingCallback;
import org.axonframework.eventhandling.saga.EndSaga;
import org.axonframework.eventhandling.saga.SagaEventHandler;
import org.axonframework.eventhandling.saga.StartSaga;
import org.axonframework.samples.bank.api.bankaccount.*;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCreatedEvent;
import org.axonframework.samples.bank.api.banktransfer.MarkBankTransferCompletedCommand;
import org.axonframework.samples.bank.api.banktransfer.MarkBankTransferFailedCommand;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;

import static org.axonframework.commandhandling.GenericCommandMessage.asCommandMessage;

@Saga
public class BankTransferManagementSaga {

    private transient CommandBus commandBus;
    private transient Tracer tracer;

    @Autowired
    public void setCommandBus(CommandBus commandBus) {
        this.commandBus = commandBus;
    }
    
    @Autowired
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    private String sourceBankAccountId;
    private String destinationBankAccountId;
    private long amount;

    @StartSaga
    @SagaEventHandler(associationProperty = "bankTransferId")
    public void on(BankTransferCreatedEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "transfer_start");
        }
        this.sourceBankAccountId = event.getSourceBankAccountId();
        this.destinationBankAccountId = event.getDestinationBankAccountId();
        this.amount = event.getAmount();

        DebitSourceBankAccountCommand command = new DebitSourceBankAccountCommand(
                event.getSourceBankAccountId(),
                event.getBankTransferId(),
                event.getAmount());
        commandBus.dispatch(asCommandMessage(command), LoggingCallback.INSTANCE);
    }

    @SagaEventHandler(associationProperty = "bankTransferId")
    @EndSaga
    public void on(SourceBankAccountNotFoundEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "transfer_fail_sourceNotFound");
        }
        MarkBankTransferFailedCommand markFailedCommand = new MarkBankTransferFailedCommand(event.getBankTransferId());
        commandBus.dispatch(asCommandMessage(markFailedCommand), LoggingCallback.INSTANCE);
    }

    @SagaEventHandler(associationProperty = "bankTransferId")
    @EndSaga
    public void on(SourceBankAccountDebitRejectedEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "transfer_fail_debitRejected");
        }
        MarkBankTransferFailedCommand markFailedCommand = new MarkBankTransferFailedCommand(event.getBankTransferId());
        commandBus.dispatch(asCommandMessage(markFailedCommand), LoggingCallback.INSTANCE);
    }

    @SagaEventHandler(associationProperty = "bankTransferId")
    public void on(SourceBankAccountDebitedEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "transfer_debited");
        }
        CreditDestinationBankAccountCommand command = new CreditDestinationBankAccountCommand(
                destinationBankAccountId,
                event.getBankTransferId(),
                event.getAmount());
        commandBus.dispatch(asCommandMessage(command), LoggingCallback.INSTANCE);
    }

    @SagaEventHandler(associationProperty = "bankTransferId")
    @EndSaga
    public void on(DestinationBankAccountNotFoundEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "transfer_fail_destNotFound");
        }
        ReturnMoneyOfFailedBankTransferCommand returnMoneyCommand = new ReturnMoneyOfFailedBankTransferCommand(
                sourceBankAccountId,
                amount);
        commandBus.dispatch(asCommandMessage(returnMoneyCommand), LoggingCallback.INSTANCE);

        MarkBankTransferFailedCommand markFailedCommand = new MarkBankTransferFailedCommand(
                event.getBankTransferId());
        commandBus.dispatch(asCommandMessage(markFailedCommand), LoggingCallback.INSTANCE);
    }

    @SagaEventHandler(associationProperty = "bankTransferId")
    @EndSaga
    public void on(DestinationBankAccountCreditedEvent event) {
        if (tracer != null && tracer.getCurrentSpan() != null) {
            tracer.getCurrentSpan().tag("operation", "transfer_completed");
        }
        MarkBankTransferCompletedCommand command = new MarkBankTransferCompletedCommand(event.getBankTransferId());
        commandBus.dispatch(asCommandMessage(command), LoggingCallback.INSTANCE);
    }
}