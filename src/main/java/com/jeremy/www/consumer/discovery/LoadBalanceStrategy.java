package com.jeremy.www.consumer.discovery;

import java.util.List;

public interface LoadBalanceStrategy {
    String selectHost(List<String> repos);
}
