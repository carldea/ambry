/*
 * Copyright 2021 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.github.ambry.cloud.azure;

import com.azure.core.http.HttpClient;
import com.azure.core.util.Configuration;
import com.azure.identity.ClientSecretCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.github.ambry.config.CloudConfig;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;


/**
 * {@link StorageClient} implementation for AD based authentication using a {@link ClientSecretCredential} instead
 * of the lower level msal4j library. The credential impl internally handles refreshing the token before it expires.
 */
public class ClientSecretCredentialStorageClient extends StorageClient {

  /**
   * Constructor for {@link ClientSecretCredentialStorageClient} object.
   * @param cloudConfig {@link CloudConfig} object.
   * @param azureCloudConfig {@link AzureCloudConfig} object.
   * @param azureMetrics {@link AzureMetrics} object.
   * @param blobLayoutStrategy {@link AzureBlobLayoutStrategy} object.
   */
  public ClientSecretCredentialStorageClient(CloudConfig cloudConfig, AzureCloudConfig azureCloudConfig,
      AzureMetrics azureMetrics, AzureBlobLayoutStrategy blobLayoutStrategy) {
    super(cloudConfig, azureCloudConfig, azureMetrics, blobLayoutStrategy);
  }

  /**
   * Constructor for {@link ClientSecretCredentialStorageClient} object for testing.
   * @param blobServiceClient {@link BlobServiceClient} object.
   * @param blobBatchClient {@link BlobBatchClient} object.
   * @param azureMetrics {@link AzureMetrics} object.
   * @param blobLayoutStrategy {@link AzureBlobLayoutStrategy} object.
   */
  public ClientSecretCredentialStorageClient(BlobServiceClient blobServiceClient, BlobBatchClient blobBatchClient,
      AzureMetrics azureMetrics, AzureBlobLayoutStrategy blobLayoutStrategy) {
    super(blobServiceClient, blobBatchClient, azureMetrics, blobLayoutStrategy);
  }

  /**
   * @param httpClient {@link HttpClient} object.
   * @param configuration {@link Configuration} object.
   * @param retryOptions {@link RequestRetryOptions} object.
   * @param azureCloudConfig {@link AzureCloudConfig} object.
   * @return BlobServiceClient object.
   * @throws MalformedURLException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Override
  protected BlobServiceClient buildBlobServiceClient(HttpClient httpClient, Configuration configuration,
      RequestRetryOptions retryOptions, AzureCloudConfig azureCloudConfig) {
    return new BlobServiceClientBuilder().credential(AzureUtils.getClientSecretCredential(azureCloudConfig))
        .endpoint(azureCloudConfig.azureStorageEndpoint)
        .httpClient(httpClient)
        .retryOptions(retryOptions)
        .configuration(configuration)
        .buildClient();
  }

  @Override
  protected void validateABSAuthConfigs(AzureCloudConfig azureCloudConfig) {
    AzureUtils.validateAzureIdentityConfigs(azureCloudConfig);
  }

  @Override
  protected boolean handleExceptionAndHintRetry(BlobStorageException blobStorageException) {
    // no need to request a retry on 403 since the credential impl handles token refresh internally.
    return false;
  }
}
