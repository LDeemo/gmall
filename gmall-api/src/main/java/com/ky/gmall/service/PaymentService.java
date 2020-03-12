package com.ky.gmall.service;

import com.ky.gmall.beans.PaymentInfo;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);
}
