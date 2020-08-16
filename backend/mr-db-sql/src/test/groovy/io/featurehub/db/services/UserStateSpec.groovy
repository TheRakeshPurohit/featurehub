package io.featurehub.db.services

import io.ebean.Database
import io.featurehub.db.api.Opts
import io.featurehub.db.api.UserStateApi
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.HiddenEnvironments
import spock.lang.Shared

class UserStateSpec extends BaseSpec {
  @Shared PersonSqlApi personSqlApi
  @Shared DbPortfolio portfolio1
  @Shared ApplicationSqlApi appApi
  @Shared EnvironmentSqlApi envApi
  @Shared Application app1
  @Shared Group groupInPortfolio1
  @Shared Environment env1
  @Shared UserStateApi userStateApi

  def setupSpec() {
    baseSetupSpec()
    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy)

    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    envApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    userStateApi = new UserStateSqlApi(convertUtils, database)

    // now set up the environments we need
    DbOrganization organization = Finder.findDbOrganization()
    portfolio1 = new DbPortfolio.Builder().name("port-user-spec").whoCreated(dbSuperPerson).organization(organization).build()
    database.save(portfolio1)

    // create the portfolio group
    groupInPortfolio1 = groupSqlApi.createPortfolioGroup(portfolio1.id.toString(), new Group().name("p1-user-spec-admin").admin(true), superPerson)
    groupSqlApi.addPersonToGroup(groupInPortfolio1.id, superPerson.id.id, Opts.empty())

    app1 = appApi.createApplication(portfolio1.id.toString(), new Application().name('app1-user-spec'), superPerson)
    assert app1 != null && app1.id != null

    env1 = envApi.create(new Environment().name("dev").description("desc"), app1, superPerson)
  }

  def "Basic CRUD on HiddenEnvironments works as expected"() {
    given: "I create a config for a person"
      userStateApi.saveHiddenEnvironments(superPerson, new HiddenEnvironments().addEnvironmentIdsItem(env1.id), app1.id)
    when: "I get it"
      def he = userStateApi.getHiddenEnvironments(superPerson, app1.id)
    and: "save an empty one"
      userStateApi.saveHiddenEnvironments(superPerson, new HiddenEnvironments(), app1.id)
    then:
      userStateApi.getHiddenEnvironments(superPerson, app1.id) == null
      he != null
      he.environmentIds.size() == 1
      he.environmentIds[0] == env1.id
  }

  def "Invalid db save is ignored"() {
    given: "a new user state"
      def us = new UserStateSqlApi(Mock(Conversions), Mock(Database))
    when: "i save"
      us.saveHiddenEnvironments(null, null, null)
    then:
      0 * database.save(any())
  }
}
