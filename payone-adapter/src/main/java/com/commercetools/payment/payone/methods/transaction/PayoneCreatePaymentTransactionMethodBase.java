package com.commercetools.payment.payone.methods.transaction;

import com.commercetools.payment.payone.config.PayoneConfigurationProvider;
import com.commercetools.payment.actions.HandlingTask;
import com.commercetools.payment.actions.OperationResult;
import com.commercetools.payment.actions.ShopAction;
import com.commercetools.payment.domain.PaymentTransactionCreationResultBuilder;
import com.commercetools.payment.methods.CreatePaymentTransactionMethodBase;
import com.commercetools.payment.model.CreatePaymentTransactionData;
import com.commercetools.payment.model.HttpRequestResult;
import com.commercetools.payment.model.PaymentTransactionCreationResult;
import com.commercetools.payment.utils.PaymentConnectorHelper;
import com.commercetools.payment.utils.PaymentLookupHelper;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentStatus;
import io.sphere.sdk.payments.TransactionType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static com.commercetools.payment.payone.config.PayoneConfigurationNames.HANDLE_URL;
import static com.commercetools.payment.payone.config.PayoneConfigurationNames.REDIRECT_URL;

/**
 * Created by mgatz on 8/1/16.
 */
public abstract class PayoneCreatePaymentTransactionMethodBase extends CreatePaymentTransactionMethodBase {

    private String handleUrl;

    @Override
    public Function<CreatePaymentTransactionData, CompletionStage<PaymentTransactionCreationResult>> create() {
        return cptd -> {
            setHandleUrl(cptd.getConfigByName(HANDLE_URL));
            return createPaymentTransaction(cptd, getDefaultTransactionType(cptd))
                    .thenCompose(payment -> {
                        return afterTransactionCreation(cptd, payment);

                    }).exceptionally(t -> {
                        return handleExceptions(t);
                    });
        };
    }

    protected void setHandleUrl(String handleUrl) {
        this.handleUrl = handleUrl;
    }

    protected TransactionType getDefaultTransactionType(CreatePaymentTransactionData cptd) {
        return PayoneConfigurationProvider.of().load().getTransactionType(cptd.getPayment().getPaymentMethodInfo().getMethod());
    }

    protected String buildHandleUrl(Payment p) {

        return this.handleUrl + p.getId();
    }

    protected CompletionStage<PaymentTransactionCreationResult> afterTransactionCreation(CreatePaymentTransactionData cptd, Payment payment) {
        // call PSP connector to handle the payment synchronously
        HttpRequestResult result = PaymentConnectorHelper.of().sendHttpGetRequest(
                buildHandleUrl(payment));
        if (!result.getResponse().isPresent()) {
            return CompletableFuture.supplyAsync(() ->
                    PaymentTransactionCreationResultBuilder.ofError(
                            "Payone handle call (URL: "
                                    + result.getRequest().getUrl()
                                    + ") failed!",
                            result.getException().get()));
        } else if (result.getResponse().get().getStatusCode() != HttpStatusCode.OK_200) {
            return CompletableFuture.supplyAsync(() ->
                    PaymentTransactionCreationResultBuilder.ofError(
                            "Payone handle call (URL: "
                                    + result.getRequest().getUrl()
                                    + ") returned wrong HTTP status: "
                                    + result.getResponse().get().getStatusCode()
                                    + "! Check Payone Connector log files!"));
        }
        return PaymentLookupHelper.of(cptd.getSphereClient()).findPayment(payment.getId()) // update the payment object
                .thenApply(updatedPayment -> {
                    return handleSuccessfulServiceCall(updatedPayment);
                });
    }

    PaymentTransactionCreationResult handleError(String errorMessage, Payment payment) {
        if (payment != null && payment.getPaymentStatus() != null) {
            PaymentStatus paymentStatus = payment.getPaymentStatus();
            errorMessage = errorMessage + String.format("Error code: %s, Error message: %s", paymentStatus.getInterfaceCode(), paymentStatus.getInterfaceText());
        }
        return PaymentTransactionCreationResultBuilder.ofError(errorMessage, null, payment);
    }

    /**
     * Will be called after the Payone services handle URL request has finished successfully.
     *
     * @param updatedPayment the refreshed payment object
     * @return the result object
     */
    protected abstract PaymentTransactionCreationResult handleSuccessfulServiceCall(Payment updatedPayment);

    protected PaymentTransactionCreationResult redirectSuccessfulServiceCall(Payment updatedPayment) {
        String redirectURL = getCustomFieldStringIfExists(updatedPayment, REDIRECT_URL);
        if (null != redirectURL) {
            return PaymentTransactionCreationResultBuilder.of(OperationResult.SUCCESS)
                    .payment(updatedPayment)
                    .handlingTask(HandlingTask.of(ShopAction.REDIRECT).redirectUrl(redirectURL)).build();
        }
        String errorMessage = "Payment provider redirect URL is not available.";
        return handleError(errorMessage, updatedPayment);
    }

    protected PaymentTransactionCreationResult handleExceptions(Throwable t) {
        return PaymentTransactionCreationResultBuilder.ofError("An exception occured that could not be handled: ", t);
    }
}