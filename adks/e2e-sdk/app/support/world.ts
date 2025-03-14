import { setDefaultTimeout, setWorldConstructor, World } from '@cucumber/cucumber';
import {
  Application,
  ApplicationServiceApi,
  AuthServiceApi,
  Configuration,
  Environment,
  Environment2ServiceApi,
  EnvironmentFeatureServiceApi,
  EnvironmentServiceApi,
  Feature,
  FeatureGroup,
  FeatureGroupListGroup,
  FeatureGroupServiceApi,
  FeatureHistoryServiceApi,
  FeatureServiceApi,
  FeatureValue,
  Person,
  PersonServiceApi,
  SystemConfigServiceApi,
  Portfolio,
  PortfolioServiceApi,
  ServiceAccount,
  ServiceAccountPermission,
  ServiceAccountServiceApi,
  TokenizedPerson,
  WebhookServiceApi,
  ApplicationRolloutStrategyServiceApi,
  ApplicationRolloutStrategy,
  FeatureHistoryList,
  ListApplicationRolloutStrategyItem, FeatureHistoryValue
} from '../apis/mr-service';
import { axiosLoggingAttachment, logger } from './logging';
import globalAxios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { Configuration as EdgeConfig, FeatureServiceApi as EdgeService } from '../apis/edge';
import {
  ClientContext,
  EdgeFeatureHubConfig,
  FeatureHubRepository,
  FeatureStateHolder
} from 'featurehub-javascript-node-sdk';
import { expect } from 'chai';
import { edgeHost, mrHost } from './discovery';

let apiKey: string;

// axiosLoggingAttachment([ globalAxios ]);

export class SdkWorld extends World {
  private _portfolio: Portfolio;
  private _application: Application;
  public feature: Feature;
  public environment: Environment;
  public serviceAccountPermission: ServiceAccountPermission;
  public edgeServer: EdgeFeatureHubConfig;
  private _repository: FeatureHubRepository;
  public readonly adminUrl: string;
  public readonly featureUrl: string;
  public readonly adminApiConfig: Configuration;
  public readonly portfolioApi: PortfolioServiceApi;
  public readonly applicationApi: ApplicationServiceApi;
  public readonly environmentApi: EnvironmentServiceApi;
  public readonly environment2Api: Environment2ServiceApi;
  public readonly featureGroupApi: FeatureGroupServiceApi;
  public readonly featureApi: FeatureServiceApi;
  public readonly loginApi: AuthServiceApi;
  public readonly featureHistoryApi: FeatureHistoryServiceApi;
  public readonly personApi: PersonServiceApi;
  public readonly serviceAccountApi: ServiceAccountServiceApi;
  public readonly featureValueApi: EnvironmentFeatureServiceApi;
  public readonly edgeApi: EdgeService;
  public readonly historyApi: FeatureHistoryServiceApi;
  public readonly systemConfigApi: SystemConfigServiceApi;
  public readonly applicationStrategyApi: ApplicationRolloutStrategyServiceApi;

  public readonly webhookApi: WebhookServiceApi;
  private _clientContext: ClientContext;
  public sdkUrlClientEval: string;
  public sdkUrlServerEval: string;
  private scenarioId: string;
  public person: Person
  public featureGroup: FeatureGroup;
  public serviceAccount?: ServiceAccount;
  public applicationStrategies: Record<string,ListApplicationRolloutStrategyItem> = {};
  public featureHistory: FeatureHistoryList;
  public featureHistorySave: Record<string, FeatureHistoryValue> = {};

  constructor(props: any) {
    super(props);

    if (process.env.REMOTE_BEARER_TOKEN) {
      apiKey = process.env.REMOTE_BEARER_TOKEN;
      console.log('api key is ', apiKey);
    }

    this.adminUrl = mrHost();
    this.featureUrl = edgeHost();

    this.adminApiConfig = new Configuration({ basePath: this.adminUrl, apiKey: apiKey, axiosInstance: globalAxios.create(), accessToken: apiKey });
    this.portfolioApi = new PortfolioServiceApi(this.adminApiConfig);
    this.personApi = new PersonServiceApi(this.adminApiConfig);
    this.applicationApi = new ApplicationServiceApi(this.adminApiConfig);
    this.environmentApi = new EnvironmentServiceApi(this.adminApiConfig);
    this.environment2Api = new Environment2ServiceApi(this.adminApiConfig);
    this.featureGroupApi = new FeatureGroupServiceApi(this.adminApiConfig);
    this.featureApi = new FeatureServiceApi(this.adminApiConfig);
    this.loginApi = new AuthServiceApi(this.adminApiConfig); // too noisy in logs
    this.serviceAccountApi = new ServiceAccountServiceApi(this.adminApiConfig);
    this.featureValueApi = new EnvironmentFeatureServiceApi(this.adminApiConfig);
    this.webhookApi = new WebhookServiceApi(this.adminApiConfig);
    this.historyApi = new FeatureHistoryServiceApi(this.adminApiConfig);
    this.systemConfigApi = new SystemConfigServiceApi(this.adminApiConfig);
    this.applicationStrategyApi = new ApplicationRolloutStrategyServiceApi(this.adminApiConfig);
    this.featureHistoryApi = new FeatureHistoryServiceApi(this.adminApiConfig);

    const edgeConfig = new EdgeConfig({ basePath: this.featureUrl, axiosInstance: this.adminApiConfig.axiosInstance});
    this.edgeApi = new EdgeService(edgeConfig);

    axiosLoggingAttachment([this.adminApiConfig.axiosInstance]);
    const self = this;
    this.attachBaggageInterceptors(() => {
      return self.baggageHeader();
    }, [this.adminApiConfig.axiosInstance])
  }

  private attachBaggageInterceptors(baggageHeader: () => string | undefined, axiosInstances: Array<AxiosInstance>): void {
    axiosInstances.forEach((axios) => {
      axios.interceptors.request.use((reqConfig: InternalAxiosRequestConfig) => {
        const header = baggageHeader();

        if (header) {
          reqConfig.headers['baggage'] = header;
        }

        return reqConfig;
      }, (error) => Promise.reject(error));
    });
  }

  private baggageHeader(): string | undefined {
    const headers = [];

    if (this.scenarioId) {
      headers.push(`cucumberScenarioId=${this.scenarioId}`);
    }

    return headers.length == 0 ? undefined : headers.join(',');
  }

  public setScenarioId(id: string) {
    this.scenarioId = id;
    logger.info('session id is %s', this.scenarioId);
    this.attach(`scenarioId=${id}`, 'text/plain');
  }

  public reset(): void {
    this._application = undefined;
    this._portfolio = undefined;
    this.feature = undefined;
    this.environment = undefined;
    this.serviceAccountPermission = undefined;
    this.edgeServer = undefined;
  }

  public set repository(r: FeatureHubRepository) {
    this._repository = r;
  }

  public get repository() {
    return this._repository;
  }

  public featureState(key: string): FeatureStateHolder {
    if (this._clientContext) {
      return this._clientContext.feature(key);
    } else {
      return this.repository.feature(key);
    }
  }

  public get context(): ClientContext {
    if (!this._clientContext) {
      this._clientContext = this.edgeServer.newContext();
    }

    return this._clientContext;
  }

  public resetContext() {
    this._clientContext = this.edgeServer.newContext();
  }

  public set portfolio(p: Portfolio) {
    this._portfolio = p;
  }

  public get portfolio() {
    return this._portfolio;
  }

  public set application(a: Application) {
    this._application = a;
  }

  public get application() {
    return this._application;
  }

  public async getSelf() {
    this.person = (await this.personApi.getPerson('self',)).data;
  }

  public set apiKey(val: TokenizedPerson) {
    this.adminApiConfig.accessToken = val.accessToken;
    this.person = val.person;
    apiKey = val.accessToken;
    logger.info('Successfully logged in');
  }

  async getFeatureValue(): Promise<FeatureValue> {
    try {
      const fValueResult = await this.featureValueApi.getFeatureForEnvironment(this.environment.id, this.feature.key);
      return fValueResult.data;
    } catch (e: any) {
      expect(e.response.status).to.eq(404); // null value

      if (e.response.status === 404) {
        return new FeatureValue({key: this.feature.key});
      }
    }
  }

  async updateFeature(fValue: FeatureValue, status: number = 200) : Promise<FeatureValue> {
    fValue.whenUpdated = undefined;
    fValue.whoUpdated = undefined;
    const uResult = await this.featureValueApi.updateFeatureForEnvironment(this.environment.id, this.feature.key, fValue);
    expect(uResult.status).to.eq(status);
    return uResult.data;
  }
}

setDefaultTimeout(30 * 1000);
setWorldConstructor(SdkWorld);
