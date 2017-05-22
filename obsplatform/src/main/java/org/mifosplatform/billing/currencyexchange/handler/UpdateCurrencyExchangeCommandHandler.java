/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.billing.currencyexchange.handler;

import org.mifosplatform.billing.currencyexchange.service.CurrencyExchangeWritePlatformService;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCurrencyExchangeCommandHandler implements
		NewCommandSourceHandler {

	private final CurrencyExchangeWritePlatformService writePlatformService;

	@Autowired
	public UpdateCurrencyExchangeCommandHandler(
			final CurrencyExchangeWritePlatformService writePlatformService) {
		this.writePlatformService = writePlatformService;
	}

	@Transactional
	@Override
	public CommandProcessingResult processCommand(final JsonCommand command) {

		return this.writePlatformService.updateCurrencyExchange(
				command.entityId(), command);
	}
}