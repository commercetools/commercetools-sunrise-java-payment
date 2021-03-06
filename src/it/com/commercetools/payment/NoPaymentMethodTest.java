package com.commercetools.payment;

import com.commercetools.payment.actions.OperationResult;
import com.commercetools.payment.actions.ShopAction;
import com.commercetools.payment.domain.CreatePaymentDataBuilder;
import com.commercetools.payment.domain.CreatePaymentTransactionDataBuilder;
import com.commercetools.payment.model.PaymentCreationResult;
import com.commercetools.payment.model.PaymentTransactionCreationResult;
import com.commercetools.payment.service.PaymentAdapterService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mht@dotsource.de
 */
public class NoPaymentMethodTest extends BasePayoneTest {

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        super.setup(2);
    }

    @After
    public void tearDown() throws Exception {
        shutdown();
    }

    @Test
    public void testPaymentCreation() throws ExecutionException, InterruptedException {

        final PaymentCreationResult paymentCreationResult = PaymentAdapterService.of()
                .createPayment(
                        CreatePaymentDataBuilder.of(client, "INTERNAL", "FREE", cart, Long.toString(new Date().getTime()))
                                .build())
                .toCompletableFuture().get();

        assertThat(paymentCreationResult.getHandlingTask().getAction()).isEqualTo(ShopAction.CONTINUE);
        assertThat(paymentCreationResult.getOperationResult()).isEqualTo(OperationResult.SUCCESS);
        assertThat(paymentCreationResult.getRelatedPaymentObject()).isPresent();
        assertThat(paymentCreationResult.getRelatedPaymentObject().get().getPaymentMethodInfo().getPaymentInterface()).isEqualTo("INTERNAL");
        assertThat(paymentCreationResult.getRelatedPaymentObject().get().getAmountPlanned()).isEqualTo(cart.getTotalPrice());
    }

    @Test
    public void testTransactionCreation() throws ExecutionException, InterruptedException {

        final PaymentCreationResult paymentCreationResult = PaymentAdapterService.of()
                .createPayment(
                        CreatePaymentDataBuilder.of(client, "INTERNAL", "FREE", cart, Long.toString(new Date().getTime()))
                                .build())
                .toCompletableFuture().get();

        final String referenceId = paymentCreationResult.getRelatedPaymentObject().get().getId();

        final PaymentTransactionCreationResult paymentTransactionCreationResult = PaymentAdapterService.of()
                .createPaymentTransaction(
                        CreatePaymentTransactionDataBuilder.of(client, referenceId).build()
                        ).toCompletableFuture().get();

        assertThat(paymentTransactionCreationResult.getOperationResult()).isEqualTo((OperationResult.SUCCESS));
        assertThat(paymentTransactionCreationResult.getHandlingTask().getAction()).isEqualTo(ShopAction.CONTINUE);
    }


}
