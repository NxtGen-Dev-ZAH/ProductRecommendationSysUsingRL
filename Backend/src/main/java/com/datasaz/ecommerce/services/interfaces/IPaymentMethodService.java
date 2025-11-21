package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.PaymentMethod;

import java.util.List;

public interface IPaymentMethodService {


    /**
     * This method is used to get all payment methods.
     *
     * @return a list of all payment methods
     */
    List<PaymentMethod> getAllPaymentMethods();

    /**
     * This method is used to get a payment method by its ID.
     *
     * @param id the ID of the payment method
     * @return the payment method with the specified ID
     */
    PaymentMethod getPaymentMethodById(Long id);

    /**
     * This method is used to create a new payment method.
     *
     * @param paymentMethod the payment method to be created
     * @return the created payment method
     */
    PaymentMethod createPaymentMethod(PaymentMethod paymentMethod);

    /**
     * This method is used to update an existing payment method.
     *
     * @param id            the ID of the payment method to be updated
     * @param paymentMethod the updated payment method
     * @return the updated payment method
     */
    PaymentMethod updatePaymentMethod(Long id, PaymentMethod paymentMethod);

    /**
     * This method is used to delete a payment method by its ID.
     *
     * @param id the ID of the payment method to be deleted
     */
    void deletePaymentMethod(Long id);
}