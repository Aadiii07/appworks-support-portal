package com.appworks.portal.integration;

import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;

/**
 * The boundary between our app and AppWorks. MockAppworksClient is the only
 * implementation right now, since no real WSDL/endpoint/credentials have been
 * provided yet. When those are available, a real implementation (e.g. a SOAP
 * client) can be added and marked @Primary instead — nothing else in the
 * codebase needs to change, because everything depends on this interface.
 */
public interface AppworksClient {
    AppworksResult execute(Customer customer, Environment environment, Metric metric);
}
