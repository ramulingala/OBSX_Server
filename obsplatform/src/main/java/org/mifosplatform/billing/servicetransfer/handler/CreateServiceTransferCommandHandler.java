package org.mifosplatform.billing.servicetransfer.handler;

import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.portfolio.property.service.PropertyWriteplatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateServiceTransferCommandHandler implements NewCommandSourceHandler {

	private final PropertyWriteplatformService propertyWriteplatformService;

	@Autowired
	public CreateServiceTransferCommandHandler(final PropertyWriteplatformService propertyWriteplatformService) {

		this.propertyWriteplatformService = propertyWriteplatformService;
	}

	@Transactional
	@Override
	public CommandProcessingResult processCommand(final JsonCommand command) {

		return this.propertyWriteplatformService.createServiceTransfer(command.entityId(),command);
	}
}

