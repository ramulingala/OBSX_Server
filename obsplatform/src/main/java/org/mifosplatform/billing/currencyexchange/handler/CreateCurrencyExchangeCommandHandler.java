package org.mifosplatform.billing.currencyexchange.handler;

import org.mifosplatform.billing.currencyexchange.service.CurrencyExchangeWritePlatformService;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCurrencyExchangeCommandHandler implements
		NewCommandSourceHandler {

	CurrencyExchangeWritePlatformService currencyExchangeWritePlatformService;

	@Autowired
	public CreateCurrencyExchangeCommandHandler(final CurrencyExchangeWritePlatformService currencyExchangeWritePlatformService) {
		this.currencyExchangeWritePlatformService = currencyExchangeWritePlatformService;
	}

	@Transactional
	@Override
	public CommandProcessingResult processCommand(final JsonCommand command) {
		
		return currencyExchangeWritePlatformService.createCurrencyExchange(command);
	}

}
