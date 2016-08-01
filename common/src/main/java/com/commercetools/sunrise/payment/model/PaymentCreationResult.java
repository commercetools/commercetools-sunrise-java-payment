package com.commercetools.sunrise.payment.model;

import com.commercetools.sunrise.payment.actions.HandlingTask;
import com.commercetools.sunrise.payment.actions.OperationResult;
import io.sphere.sdk.payments.Payment;

import java.util.Optional;

/**
 * Provides all information a payment create operation creates for the calling shop system.
 * Created by mgatz on 7/20/16.
 */
public interface PaymentCreationResult {
    /**
     * @return the basic operation result
     */
    OperationResult getOperationResult();

    /**
     * If the operation succeeded then this is the payment object that has been created.
     * @return the created payment object
     */
    Optional<Payment> getRelatedPaymentObject();

    /**
     * Get the task that describes what actions the shop should do next.
     * @return the task description object
     */
    HandlingTask getHandlingTask();
}