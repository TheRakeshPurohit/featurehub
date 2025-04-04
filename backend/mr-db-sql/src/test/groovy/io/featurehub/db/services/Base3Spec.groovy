package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.Opts
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbPerson
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateEnvironment
import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.Portfolio
import io.featurehub.utils.ExecutorSupplier
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Specification

class Base3Spec extends Specification {
  @Shared Database db
  @Shared ConvertUtils convertUtils
  @Shared Person superPerson
  @Shared DbPerson dbSuperPerson
  @Shared GroupSqlApi groupSqlApi
  @Shared UUID superuser
  @Shared ArchiveStrategy archiveStrategy
  @Shared Organization org
  @Shared PortfolioSqlApi portfolioSqlApi
  @Shared ApplicationSqlApi applicationSqlApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared CacheSource cacheSource
  @Shared FeatureSqlApi featureSqlApi
  @Shared RolloutStrategyValidator rsValidator
  @Shared Portfolio portfolio
  @Shared Application app1
  @Shared Environment env1
  @Shared FeatureMessagingPublisher featureMessagingCloudEventPublisher
  @Shared ExecutorSupplier executorSupplier
  @Shared InternalFeatureApi internalFeatureApi

  String ranName() {
    return RandomStringUtils.randomAlphabetic(10)
  }

  def setupSpec() {
    db = DB.getDefault()

    convertUtils = new ConvertUtils(Mock(WebhookEncryptionService))
    cacheSource = Mock()
    featureMessagingCloudEventPublisher = Mock()
    internalFeatureApi = new InternalFeatureSqlApi(convertUtils, cacheSource, featureMessagingCloudEventPublisher)

    archiveStrategy = new DbArchiveStrategy(cacheSource)
    groupSqlApi = new GroupSqlApi(db, convertUtils, archiveStrategy)

    dbSuperPerson = Finder.findByEmail("irina@featurehub.io")
    if (dbSuperPerson == null) {
      dbSuperPerson = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      db.save(dbSuperPerson)
    }

    db.save(dbSuperPerson);
    superuser = dbSuperPerson.getId()

    def organizationSqlApi = new OrganizationSqlApi(convertUtils)

    // ensure the org is created and we have an admin user in an admin group
    Group adminGroup
    def newOrganisation = !convertUtils.hasOrganisation()
    if (newOrganisation) {
      org = organizationSqlApi.save(new Organization().name("org1"))
    } else {
      org = organizationSqlApi.get()
    }

    superPerson = convertUtils.toPerson(dbSuperPerson, Opts.empty())

    if (newOrganisation) {
      adminGroup = groupSqlApi.createOrgAdminGroup(org.id, 'admin group', superPerson)
    } else {
      adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
    }

    groupSqlApi.addPersonToGroup(adminGroup.id, superuser, Opts.empty())

    rsValidator = Mock()
    rsValidator.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    rsValidator.validateStrategies(_, _, _, _) >> new RolloutStrategyValidator.ValidationFailure()


    featureSqlApi = new FeatureSqlApi(convertUtils, cacheSource, rsValidator, featureMessagingCloudEventPublisher, Mock(CacheSourceFeatureGroupApi))
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)
    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy, new InternalFeatureSqlApi(convertUtils,cacheSource,featureMessagingCloudEventPublisher), Mock(WebhookEncryptionService))
    applicationSqlApi = new ApplicationSqlApi(convertUtils, cacheSource, archiveStrategy, new InternalFeatureSqlApi(convertUtils,cacheSource,featureMessagingCloudEventPublisher))

    portfolio = portfolioSqlApi.createPortfolio(new CreatePortfolio().name(RandomStringUtils.randomAlphabetic(10)).description("desc1"), Opts.empty(), superuser)
    app1 = applicationSqlApi.createApplication(portfolio.id, new CreateApplication().name(RandomStringUtils.randomAlphabetic(10)).description("app1"), superPerson)
    env1 = environmentSqlApi.create(new CreateEnvironment().description(RandomStringUtils.randomAlphabetic(10)).name(RandomStringUtils.randomAlphabetic(10)), app1.id, superPerson)
  }

  def cleanup() {
    db.currentTransaction()?.commit()
  }
}
