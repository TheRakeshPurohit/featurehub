package io.featurehub.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.sse.model.FeatureState;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

public class FeatureStatePlaceHolder extends FeatureStateBaseHolder {
  private final ObjectMapper mapper;

  public FeatureStatePlaceHolder(Executor executor, List<FeatureValueInterceptorHolder> valueInterceptors, String key,
                                 ObjectMapper mapper) {
    super(executor, valueInterceptors, key);

    this.mapper = mapper;
  }

  @Override
  protected FeatureStateHolder setFeatureState(FeatureState state) {
    return this;
  }

  @Override
  protected FeatureStateHolder copy() {
    return this;
  }

  @Override
  public String getString() {
    FeatureValueInterceptor.ValueMatch value = findIntercept();

    return (value == null) ? null : value.value;
  }

  @Override
  public Boolean getBoolean() {
    FeatureValueInterceptor.ValueMatch value = findIntercept();

    return (value == null) ? Boolean.FALSE : Boolean.parseBoolean(value.value);
  }

  @Override
  public BigDecimal getNumber() {
    FeatureValueInterceptor.ValueMatch value = findIntercept();

    return (value == null) ? null : new BigDecimal(value.value);
  }

  @Override
  public String getRawJson() {
    FeatureValueInterceptor.ValueMatch value = findIntercept();

    return (value == null) ? null : value.value;
  }

  @Override
  public <T> T getJson(Class<T> type) {
    FeatureValueInterceptor.ValueMatch value = findIntercept();

    try {
      return (value == null) ? null : mapper.readValue(value.value, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
