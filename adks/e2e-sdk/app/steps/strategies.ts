import {Given, Then, When} from '@cucumber/cucumber';
import { SdkWorld } from '../support/world';
import DataTable from '@cucumber/cucumber/lib/models/data_table';
import {
  ApplicationRolloutStrategy,
  CreateApplicationRolloutStrategy, ListApplicationRolloutStrategyItem,
  RolloutStrategy,
  RolloutStrategyInstance
} from '../apis/mr-service';
import waitForExpect from 'wait-for-expect';
import { expect } from 'chai';
import {makeid} from "../support/random";

Then(/^I (cannot|can) create custom flag rollout strategies$/, async function(can: string, table: DataTable) {
  const world = this as SdkWorld;

  const fv = await world.getFeatureValue();
  const s: Array<RolloutStrategy> = [];
  for(const row of table.hashes()) {
    const rs = new RolloutStrategy({
      percentage: parseFloat(row['percentage']),
      name: row['name'],
      value: row['value'] === 'true'
    });

    s.push(rs);
  }

  fv.rolloutStrategies = s;
  try {
    await world.updateFeature(fv, can === 'cannot' ? 422 : 200);
  } catch (e) {
    console.log('error is', e);
  }
});

Given('I create an application strategy tagged {string}', async function(strategyKey: string) {
  const world = this as SdkWorld;

  expect(world.application, 'You must have an application to create an application strategy').to.not.be.undefined;

  const data = await world.applicationStrategyApi.createApplicationStrategy(world.application.id, new CreateApplicationRolloutStrategy({
    name: strategyKey, disabled: false, attributes: []
  }));
  expect(data.status).to.eq(201);

  const all = await world.applicationStrategyApi.listApplicationStrategies(world.application.id);
  expect(all.status).to.eq(200);

  world.applicationStrategies = {};

  all.data.items.forEach(s => world.applicationStrategies[s.strategy.name] = s);
});

export function validateWorldForApplicationStrategies(world: SdkWorld, strategy: ListApplicationRolloutStrategyItem, strategyKey: string) {
  expect(strategy, `The strategy referenced by key ${strategyKey} does not exist`).to.not.be.undefined;
  expect(world.environment).to.not.be.undefined;
  expect(world.feature).to.not.be.undefined;
}

When('the application strategy {string} should be used in {int} environment with {int} feature', async function(key: string, envCount: number, featureCount: number) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[key];
  validateWorldForApplicationStrategies(world, strategy, key);
  const appStrategy =  await world.applicationStrategyApi.getApplicationStrategy(world.application.id,
    strategy.strategy.id, undefined, true);
  expect(appStrategy.status).to.eq(200);
  // expect(appStrategy.data.usage).to.not.be.undefined;
  // expect(appStrategy.data.usage.length).to.eq(envCount);
  // if (envCount > 0) {
  //   expect(appStrategy.data.usage[0].featuresCount).to.eq(featureCount);
  // }

  const listStrat = await world.applicationStrategyApi.listApplicationStrategies(world.application.id, undefined, true);
  expect(listStrat.status).to.eq(200);
  // const s = listStrat.data.items.find(str => str.id == strategy.id);
  // expect(s.usage.length).to.eq(envCount);
  // if (envCount > 0) {
  //   expect(s.usage[0].featuresCount).to.eq(featureCount);
  // }
});



When('I delete the application strategy called {string} from the current environment feature value', async function (strategyKey: string) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const featureValue = await world.getFeatureValue();
  featureValue.rolloutStrategyInstances = featureValue.rolloutStrategyInstances.filter(rsi => rsi.strategyId != strategy.strategy.id);
  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.rolloutStrategyInstances.find(rsi =>
    rsi.strategyId === strategy.strategy.id)).to.be.undefined;
});

When('I attach application strategy {string} to the current environment feature value', async function (strategyKey: string) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const featureValue = await world.getFeatureValue();

  featureValue.rolloutStrategyInstances.push(new RolloutStrategyInstance({ strategyId: strategy.strategy.id,
    value: true }));

  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.rolloutStrategyInstances.find(rsi =>
    rsi.strategyId === strategy.strategy.id && rsi.value)).to.not.be.undefined;
});

When('I swap the order of {string} and {string} they remain swapped', async function (key1: string, key2: string) {
  const world = this as SdkWorld;

  const strategy1 = world.applicationStrategies[key1];
  validateWorldForApplicationStrategies(world, strategy1, key1);
  const strategy2 = world.applicationStrategies[key2];
  validateWorldForApplicationStrategies(world, strategy2, key2);

  const featureValue = await world.getFeatureValue();
  const key1Index = featureValue.rolloutStrategyInstances.findIndex(s => s.strategyId == strategy1.strategy.id);
  expect(key1Index, `cannot find strategy ${key1} in strategies`).to.not.eq(-1);
  const key2Index= featureValue.rolloutStrategyInstances.findIndex(s => s.strategyId == strategy2.strategy.id);
  expect(key2Index, `cannot find strategy ${key2} in strategies`).to.not.eq(-1);

  const rsi = featureValue.rolloutStrategyInstances[key1Index];
  featureValue.rolloutStrategyInstances[key1Index] = featureValue.rolloutStrategyInstances[key2Index];
  featureValue.rolloutStrategyInstances[key2Index] = rsi;

  const updatedValue = await world.updateFeature(featureValue);
  expect(updatedValue.rolloutStrategyInstances[key1Index].strategyId, `Strategy 2 did not swap`).to.eq(strategy2.strategy.id);
  expect(updatedValue.rolloutStrategyInstances[key2Index].strategyId).to.eq(strategy1.strategy.id);
});

Then('there is an application strategy called {string} in the current environment feature value', async function (strategyKey: string) {
  const world = this as SdkWorld;

  const strategy = world.applicationStrategies[strategyKey];
  validateWorldForApplicationStrategies(world, strategy, strategyKey);

  const featureValue = await world.getFeatureValue();

  expect(featureValue.rolloutStrategyInstances.find(rsi =>
    rsi.strategyId === strategy.strategy.id && rsi.value)).to.not.be.undefined;
});

Then("the edge repository has a feature {string} with a strategy", async function(key: string, table: DataTable) {
  const world = this as SdkWorld;

  await waitForExpect(async () => {
    for (const row of table.hashes()) {
      const ctx = await world.context.attribute_values((row['fieldName'] as string), (row['values'] as string).split(',').map(t => t.trim())).build();
      expect(ctx.feature(key).isSet()).to.be.true;
      const value = ctx.feature(key).value.toString();
      const expectedValue = row['expectedValue'].toString();
      expect(value, `expected ${expectedValue} but got ${value}`).to.eq(expectedValue);
    }
  }, 5000, 300);
});
