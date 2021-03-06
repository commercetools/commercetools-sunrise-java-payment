package com.commercetools.payment.methods;

import com.commercetools.payment.domain.PaymentTransactionCreationResultBuilder;
import com.commercetools.payment.model.CreatePaymentTransactionData;
import com.commercetools.payment.model.PaymentTransactionCreationResult;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public abstract class CreatePaymentTransactionMethodBase implements CreatePaymentTransactionMethod {

    /**
     * Create a new payment transaction using the passed creation data.
     * @param data the data wrapper object
     * @param defaultType the default {@link TransactionType} that will be used if the data wrapper does not provide a special value
     * @return the completion stage that will return the created payment object
     */
    protected CompletionStage<Payment> createPaymentTransaction(CreatePaymentTransactionData data, TransactionType defaultType) {
        TransactionDraft transactionDraft = createTransactionDraftBuilder(data, defaultType).build();
        List<UpdateAction<Payment>> commands = new ArrayList<>();
        commands.add(AddTransaction.of(transactionDraft));

        commands.addAll(addPaymentUpdateActions(data));

        return data.getSphereClient().execute(PaymentUpdateCommand.of(data.getPayment(), commands));
    }

    /**
     * Methods allows adding payment object update actions, that will be executed before adding the new transaction.
     * @param data the data wrapper object
     * @return list of update actions
     */
    protected List<UpdateAction<Payment>> addPaymentUpdateActions(CreatePaymentTransactionData data) {
        return new ArrayList<>(); // returning a real instance allows overriding methods to reuse this with "super"
    }

    /**
     * Default implementation of the payment transaction draft builder usage.
     * Can be overriden to manipulate data attached to the transaction.
     * @param data the data wrapper object
     * @param defaultType the default {@link TransactionType} that will be used if the data wrapper does not provide a special value
     * @return builder of {@link TransactionDraft} initialized with {@code data} and {@code defaultType}
     */
    protected TransactionDraftBuilder createTransactionDraftBuilder(CreatePaymentTransactionData data, TransactionType defaultType) {
        return TransactionDraftBuilder.of(
                data.getTransactionType().orElse(defaultType),
                data.getPayment().getAmountPlanned())
                .state(TransactionState.INITIAL);
    }

    @Nullable
    protected static String getCustomFieldStringIfExists(@Nullable Custom custom, @Nullable String fieldName) {
        return Optional.ofNullable(custom)
                .map(Custom::getCustom)
                .map(customFields -> customFields.getFieldAsString(fieldName))
                .orElse(null);
    }

    /**
     * @param paymentWithoutRedirect updated payment reference
     * @return {@link PaymentTransactionCreationResult} with
     * {@link com.commercetools.payment.actions.OperationResult#SUCCESS}
     * and {@link com.commercetools.payment.actions.ShopAction#CONTINUE}.
     */
    protected PaymentTransactionCreationResult defaultSuccessTransactionCreationResult(Payment paymentWithoutRedirect) {
        return PaymentTransactionCreationResultBuilder
                .ofSuccess(paymentWithoutRedirect)
                .build();
    }
}
