package org.mifosplatform.billing.currencyexchange.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CurrencyExchangeRepository extends
		JpaRepository<CurrencyExchange, Long>,
		JpaSpecificationExecutor<CurrencyExchange> {

}
