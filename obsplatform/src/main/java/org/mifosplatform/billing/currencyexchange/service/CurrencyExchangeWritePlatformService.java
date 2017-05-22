package org.mifosplatform.billing.currencyexchange.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;

public interface CurrencyExchangeWritePlatformService {

	CommandProcessingResult createCurrencyExchange(JsonCommand command);

	CommandProcessingResult updateCurrencyExchange(Long entityId,JsonCommand command);

	CommandProcessingResult deleteCurrencyExchange(Long entityId);

}
