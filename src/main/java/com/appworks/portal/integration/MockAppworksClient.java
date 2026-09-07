package com.appworks.portal.integration;

import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real AppWorks call until WSDL/endpoint/credentials exist.
 * @Primary so it's the one Spring injects wherever AppworksClient is required.
 *
 * Deliberately deterministic: the value is derived from a hash of
 * (customer code, environment name, metric service key), not randomized. This
 * means the same customer/environment/metric combination always produces the
 * same value and therefore the same PASS/WARNING/FAIL outcome, run after run.
 * That's useful for predictable manual testing, but it also means the mock
 * data will never show a customer's health changing over time in a demo — if
 * that matters for a presentation, this would need to add some variation.
 */
@Component
@Primary
public class MockAppworksClient implements AppworksClient {

    @Override
    public AppworksResult execute(Customer customer, Environment environment, Metric metric) {
        int seed = Math.abs((customer.getCode() + environment.getName() + metric.getServiceKey()).hashCode());
        double value = (seed % 10000) / 100.0;
        return new AppworksResult(value, "Mock AppWorks response: value=" + value);
    }
}
