package io.featurehub.mr.resources;

import io.featurehub.db.FilterOptType;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.mr.api.ServiceAccountServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceAccountResource implements ServiceAccountServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccountResource.class);
  private final AuthManagerService authManager;
  private final ServiceAccountApi serviceAccountApi;

  @Inject
  public ServiceAccountResource(AuthManagerService authManager, ServiceAccountApi serviceAccountApi) {
    this.authManager = authManager;
    this.serviceAccountApi = serviceAccountApi;
  }

  @Override
  public ServiceAccount createServiceAccountInPortfolio(String id, ServiceAccount serviceAccount, CreateServiceAccountInPortfolioHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      try {
        return serviceAccountApi.create(id, person, serviceAccount, new Opts().add(FillOpts.Permissions, holder.includePermissions));
      } catch (ServiceAccountApi.DuplicateServiceAccountException e) {
        log.warn("Attempt to create duplicate service account {}", serviceAccount.getName());
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    }

    throw new ForbiddenException();
  }

  @Override
  public Boolean deleteServiceAccount(String id, DeleteServiceAccountHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      if (serviceAccountApi.delete(person, id)) {
        return true;
      };

      throw new NotFoundException();
    }

    throw new ForbiddenException();
  }

  @Override
  public ServiceAccount getServiceAccount(String id, GetServiceAccountHolder holder, SecurityContext securityContext) {
    if ("self".equals(id)) {
      ServiceAccount account = authManager.serviceAccount(securityContext);
      id = account.getId();
    }

    ServiceAccount info = serviceAccountApi.get(id,
      new Opts().add(FillOpts.Permissions, holder.includePermissions).add(FilterOptType.Application, holder.byApplicationId));

    if (info == null) {
      throw new NotFoundException();
    }

    Person person = authManager.from(securityContext);
    if (!authManager.isPortfolioAdmin(info.getPortfolioId(), person) && !authManager.isOrgAdmin(person)) {
      throw new ForbiddenException();
    }


    return info;
  }

  @Override
  public ServiceAccount resetApiKey(String id, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    ServiceAccount info = serviceAccountApi.get(id,  Opts.empty());

    if (!authManager.isPortfolioAdmin(info.getPortfolioId(), person)) {
      throw new ForbiddenException();
    }

    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      ServiceAccount sa = serviceAccountApi.resetApiKey(id);

      if (sa == null) {
        throw new NotFoundException();
      }

      return sa;
    }

    throw new ForbiddenException();
  }

  @Override
  public List<ServiceAccount> searchServiceAccountsInPortfolio(String id, SearchServiceAccountsInPortfolioHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    List<ServiceAccount> serviceAccounts = serviceAccountApi.search(id, holder.filter, holder.applicationId,
          person,
          new Opts().add(FillOpts.Permissions, holder.includePermissions).add(FillOpts.SdkURL, holder.includeSdkUrls));

    if (serviceAccounts == null) {
      return new ArrayList<>();
    }

    serviceAccounts.sort(Comparator.comparing(ServiceAccount::getName));

    return serviceAccounts;
  }

  @Override
  public ServiceAccount updateServiceAccount(String serviceAccountId, ServiceAccount serviceAccount, UpdateServiceAccountHolder holder, SecurityContext securityContext) {
    Person person = authManager.from(securityContext);

    Set<String> envIds =
      serviceAccount.getPermissions().stream().map(ServiceAccountPermission::getEnvironmentId).collect(Collectors.toSet());

    if (envIds.size() < serviceAccount.getPermissions().size()) {
      throw new BadRequestException("Duplicate environment ids were passed.");
    }

    if (serviceAccount.getPortfolioId() == null) {
      throw new BadRequestException("No portfolio passed");
    }

    if (authManager.isPortfolioAdmin(serviceAccount.getPortfolioId(), person) || authManager.isOrgAdmin(person) ) {
      ServiceAccount result = null;

      try {
        result = serviceAccountApi.update(serviceAccountId, person, serviceAccount, new Opts().add(FillOpts.Permissions, holder.includePermissions));
      } catch (OptimisticLockingException e) {
        throw new WebApplicationException(422);
      }

      if (result == null) {
        throw new NotFoundException();
      }

      return result;
    }

    throw new ForbiddenException();
  }
}
