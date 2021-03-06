package com.commercetools.payment.payone.methods.transaction;

import com.commercetools.payment.actions.HandlingTask;
import com.commercetools.payment.actions.OperationResult;
import com.commercetools.payment.actions.ShopAction;
import com.commercetools.payment.domain.PaymentTransactionCreationResultBuilder;
import com.commercetools.payment.methods.CreatePaymentTransactionMethod;
import com.commercetools.payment.model.PaymentTransactionCreationResult;
import io.sphere.sdk.payments.Payment;

import static com.commercetools.payment.payone.config.PayoneConfigurationNames.*;

public class PayoneBanktransferInAdvancePaymentTransactionMethodProvider
        extends PayoneCreatePaymentTransactionMethodBase implements CreatePaymentTransactionMethod {

    private PayoneBanktransferInAdvancePaymentTransactionMethodProvider() {
    }

    public static CreatePaymentTransactionMethod of() {
        return new PayoneBanktransferInAdvancePaymentTransactionMethodProvider();
    }

    @Override
    protected PaymentTransactionCreationResult handleSuccessfulServiceCall(Payment updatedPayment) {
        final String payToIBAN = getCustomFieldStringIfExists(updatedPayment, PAID_TO_IBAN);
        final String payToBIC = getCustomFieldStringIfExists(updatedPayment, PAID_TO_BIC);
        final String payToName = getCustomFieldStringIfExists(updatedPayment, PAID_TO_NAME);

        if (null != payToIBAN && null != payToBIC && null != payToName) {
            return PaymentTransactionCreationResultBuilder.of(OperationResult.SUCCESS).payment(updatedPayment)
                    .handlingTask(HandlingTask.of(ShopAction.CONTINUE)
                            .addData(PAID_TO_IBAN, payToIBAN)
                            .addData(PAID_TO_BIC, payToBIC)
                            .addData(PAID_TO_NAME, payToName))
                    .build();
        }

        String errorMessage = "No bank account data available.";
        return handleError(errorMessage, updatedPayment);
    }

}
