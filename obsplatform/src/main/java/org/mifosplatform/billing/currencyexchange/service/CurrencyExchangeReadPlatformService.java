package org.mifosplatform.billing.currencyexchange.service;

import java.util.Collection;
import java.util.List;

import org.mifosplatform.billing.currencyexchange.data.CurrencyExchangeData;

public interface CurrencyExchangeReadPlatformService {

	List<CurrencyExchangeData> getCountryCurrencyDetailsByName(String string);

	Collection<CurrencyExchangeData> retrieveAllCurrencyConfigurationDetails();

	CurrencyExchangeData retrieveSingleCurrencyConfigurationDetails(Long id);

}
