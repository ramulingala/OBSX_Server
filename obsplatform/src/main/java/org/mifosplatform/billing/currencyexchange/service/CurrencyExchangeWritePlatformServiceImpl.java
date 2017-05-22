package org.mifosplatform.billing.currencyexchange.service;

import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.mifosplatform.billing.chargecode.service.ChargeCodeWritePlatformServiceImp;
import org.mifosplatform.billing.currencyexchange.domain.CurrencyExchange;
import org.mifosplatform.billing.currencyexchange.domain.CurrencyExchangeRepository;
import org.mifosplatform.billing.currencyexchange.exception.DuplicateCurrencyConfigurationException;
import org.mifosplatform.billing.currencyexchange.serialization.CurrencyExchangeCommandFromApiJsonDeserializer;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author hugo
 * 
 */
@Service
public class CurrencyExchangeWritePlatformServiceImpl implements CurrencyExchangeWritePlatformService {

	private final static Logger LOGGER = LoggerFactory.getLogger(ChargeCodeWritePlatformServiceImp.class);
	private final PlatformSecurityContext context;
	private final CurrencyExchangeCommandFromApiJsonDeserializer fromApiJsonDeserializer;
	private final CurrencyExchangeRepository currecnyExchangeRepository;

	@Autowired
	public CurrencyExchangeWritePlatformServiceImpl(final PlatformSecurityContext context,
			final CurrencyExchangeCommandFromApiJsonDeserializer apiJsonDeserializer,
			final CurrencyExchangeRepository currecnyExchangeRepository) {

		this.context = context;
		this.fromApiJsonDeserializer = apiJsonDeserializer;
		this.currecnyExchangeRepository = currecnyExchangeRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * #createCountryCurrency(org.mifosplatform.infrastructure.core.api.JsonCommand
	 * )
	 */
	@Transactional
	@Override
	public CommandProcessingResult createCurrencyExchange(final JsonCommand command) {

		try {

			this.context.authenticatedUser();
			this.fromApiJsonDeserializer.validateForCreate(command.json());
			final CurrencyExchange countryCurrency = CurrencyExchange.fromJson(command);
			this.currecnyExchangeRepository.save(countryCurrency);
			return new CommandProcessingResult(countryCurrency.getId());

		} catch (final DataIntegrityViolationException dve) {
			handleDataIntegretyIssue(dve, command);
			return new CommandProcessingResult(Long.valueOf(-1L));

		}

	}

	private void handleDataIntegretyIssue(final DataIntegrityViolationException dve, final JsonCommand command) {

		final Throwable realCause = dve.getMostSpecificCause();
		if (realCause.getMessage().contains("country_key")) {
			final String name = command.stringValueOfParameterNamed("country");
			throw new PlatformDataIntegrityException(
					"error.msg.countrycurrency.duplicate.configuration",
					"Country is already configured with'" + name + "'",
					"country", name);
			
		} else if (realCause.getMessage().contains("country_ISD")) {
			final String name = command.stringValueOfParameterNamed("countryISD");
			throw new PlatformDataIntegrityException(
					"error.msg.countrycurrency.duplicate.configuration",
					"Country is already configured with'" + name + "'",
					"countryISD", name);
		}
		
		

		LOGGER.error(dve.getMessage(), dve);
		throw new PlatformDataIntegrityException(
				"error.msg.could.unknown.data.integrity.issue",
				"Unknown data integrity issue with resource: "
						+ realCause.getMessage());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see #updateCountryCurrency(java.lang.Long,
	 * org.mifosplatform.infrastructure.core.api.JsonCommand)
	 */
	@Transactional
	@Override
	public CommandProcessingResult updateCurrencyExchange(final Long entityId,final JsonCommand command) {
		try {
			this.context.authenticatedUser();
			this.fromApiJsonDeserializer.validateForCreate(command.json());
			final CurrencyExchange countryCurrency = retrieveCodeById(entityId);
			final Map<String, Object> changes = countryCurrency.update(command);
			if (!changes.isEmpty()) {
				this.currecnyExchangeRepository.saveAndFlush(countryCurrency);
			}
			return new CommandProcessingResultBuilder().withCommandId(command.commandId())
					.withEntityId(countryCurrency.getId()).with(changes)
					.build();
		} catch (DataIntegrityViolationException dve) {
			if (dve.getCause() instanceof ConstraintViolationException) {
				handleDataIntegretyIssue(dve, command);
			}
			return new CommandProcessingResult(Long.valueOf(-1));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see #deleteCountryCurrency(java.lang.Long)
	 */
	@Transactional
	@Override
	public CommandProcessingResult deleteCurrencyExchange(final Long entityId) {

		this.context.authenticatedUser();
		final CurrencyExchange countryCurrency = retrieveCodeById(entityId);
		countryCurrency.delete();
		this.currecnyExchangeRepository.save(countryCurrency);
		return new CommandProcessingResultBuilder().withEntityId(entityId).build();
	}

	private CurrencyExchange retrieveCodeById(final Long currencyConfigId) {
		final CurrencyExchange countryCurrency = this.currecnyExchangeRepository.findOne(currencyConfigId);
		if (countryCurrency == null) {
			throw new DuplicateCurrencyConfigurationException(currencyConfigId);
		}
		return countryCurrency;
	}

}
