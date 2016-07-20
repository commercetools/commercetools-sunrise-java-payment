package com.commercetools.sunrise.payment.domain;

import com.commercetools.sunrise.payment.model.CreatePaymentDataProviderImpl;
import io.sphere.sdk.customers.Customer;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Provides a wrapper for all data that could possibly be needed by a {@link PaymentServiceProvider} implementation
 * to create a new payment object.
 *
 * Created by mgatz on 7/18/16.
 */
public interface CreatePaymentDataProvider {

    // TODO add support for more data like cart, http request, etc.

    static CreatePaymentDataProvider of() {
        return new CreatePaymentDataProviderImpl();
    }

    /**
     * @return the attached customer
     */
    @Nullable
    Optional<Customer> getCustomer();

    /**
     * Add a customer object to this data provider instance.
     * @param c the customer object to be added
     * @return the object itself
     */
    CreatePaymentDataProvider plusCustomer(Customer c);
}