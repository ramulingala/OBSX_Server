/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.obsplatform.useradministration.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.obsplatform.infrastructure.core.api.JsonCommand;
import org.obsplatform.infrastructure.core.data.ApiParameterError;
import org.obsplatform.infrastructure.core.data.CommandProcessingResult;
import org.obsplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.obsplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.obsplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.obsplatform.infrastructure.core.service.PlatformEmailSendException;
import org.obsplatform.infrastructure.security.service.PlatformPasswordEncoder;
import org.obsplatform.infrastructure.security.service.PlatformSecurityContext;
import org.obsplatform.organisation.office.domain.Office;
import org.obsplatform.organisation.office.domain.OfficeRepository;
import org.obsplatform.organisation.office.exception.OfficeNotFoundException;
import org.obsplatform.useradministration.domain.AppUser;
import org.obsplatform.useradministration.domain.AppUserRepository;
import org.obsplatform.useradministration.domain.Role;
import org.obsplatform.useradministration.domain.RoleRepository;
import org.obsplatform.useradministration.domain.UserDomainService;
import org.obsplatform.useradministration.exception.RoleNotFoundException;
import org.obsplatform.useradministration.exception.UserNotFoundException;
import org.obsplatform.useradministration.serialization.UsersCommandFromApiJsonDeserializerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
public class AppUserWritePlatformServiceJpaRepositoryImpl implements AppUserWritePlatformService {

    private final static Logger LOGGER = LoggerFactory.getLogger(AppUserWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final UserDomainService userDomainService;
    private final PlatformPasswordEncoder platformPasswordEncoder;
    private final AppUserRepository appUserRepository;
    private final OfficeRepository officeRepository;
    private final RoleRepository roleRepository;
    private final UsersCommandFromApiJsonDeserializerHelper fromApiJsonDeserializer;

    @Autowired
    public AppUserWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context, final AppUserRepository appUserRepository,
            final UserDomainService userDomainService, final OfficeRepository officeRepository, final RoleRepository roleRepository,
            final PlatformPasswordEncoder platformPasswordEncoder, final UsersCommandFromApiJsonDeserializerHelper fromApiJsonDeserializer) {
        this.context = context;
        this.appUserRepository = appUserRepository;
        this.userDomainService = userDomainService;
        this.officeRepository = officeRepository;
        this.roleRepository = roleRepository;
        this.platformPasswordEncoder = platformPasswordEncoder;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "usersByUsername", allEntries = true)})
    public CommandProcessingResult createUser(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final String officeIdParamName = "officeId";
            final Long officeId = command.longValueOfParameterNamed(officeIdParamName);

            final Office userOffice = this.officeRepository.findOne(officeId);
            if (userOffice == null) { throw new OfficeNotFoundException(officeId); }

            final String[] roles = command.arrayValueOfParameterNamed("roles");
            final Set<Role> allRoles = assembleSetOfRoles(roles);

            final AppUser appUser = AppUser.fromJson(userOffice, allRoles, command);
            final Boolean sendPasswordToEmail = command.booleanObjectValueOfParameterNamed("sendPasswordToEmail");
            this.userDomainService.create(appUser, sendPasswordToEmail);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(appUser.getId()) //
                    .withOfficeId(userOffice.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        } catch (final PlatformEmailSendException e) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();

            final String email = command.stringValueOfParameterNamed("email");
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.user.email.invalid",
                    "The parameter email is invalid.", "email", email);
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "usersByUsername", allEntries = true)})
    public CommandProcessingResult updateUser(final Long userId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final AppUser userToUpdate = this.appUserRepository.findOne(userId);
            if (userToUpdate == null) { throw new UserNotFoundException(userId); }

            final Map<String, Object> changes = userToUpdate.update(command, this.platformPasswordEncoder);

            if (changes.containsKey("officeId")) {
                final Long officeId = (Long) changes.get("officeId");
                final Office office = this.officeRepository.findOne(officeId);
                if (office == null) { throw new OfficeNotFoundException(officeId); }

                userToUpdate.changeOffice(office);
            }

            if (changes.containsKey("roles")) {
            	
                final String[] roleIds = (String[]) changes.get("roles");
                final Set<Role> allRoles = assembleSetOfRoles(roleIds);

                userToUpdate.updateRoles(allRoles);
            }

            if (!changes.isEmpty()) {
                this.appUserRepository.saveAndFlush(userToUpdate);
            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(userId) //
                    .withOfficeId(userToUpdate.getOffice().getId()) //
                    .with(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        }
    }

    private Set<Role> assembleSetOfRoles(final String[] rolesArray) {

        final Set<Role> allRoles = new HashSet<Role>();

        if (!ObjectUtils.isEmpty(rolesArray)) {
            for (final String roleId : rolesArray) {
                final Long id = Long.valueOf(roleId);
                final Role role = this.roleRepository.findOne(id);
                if (role == null) { throw new RoleNotFoundException(id); }
                allRoles.add(role);
            }
        }

        return allRoles;
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "usersByUsername", allEntries = true)})
    public CommandProcessingResult deleteUser(final Long userId) {

        final AppUser user = this.appUserRepository.findOne(userId);
        if (user == null || user.isDeleted()) { throw new UserNotFoundException(userId); }

        user.delete();
        this.appUserRepository.save(user);

        return new CommandProcessingResultBuilder().withEntityId(userId).withOfficeId(user.getOffice().getId()).build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final DataIntegrityViolationException dve) {

        final Throwable realCause = dve.getMostSpecificCause();
        if (realCause.getMessage().contains("username_org")) {
            final String username = command.stringValueOfParameterNamed("username");
            final StringBuilder defaultMessageBuilder = new StringBuilder("User with username ").append(username)
                    .append(" already exists.");
            throw new PlatformDataIntegrityException("error.msg.user.duplicate.username", defaultMessageBuilder.toString(), "username",
                    username);
        }

        LOGGER.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }
}