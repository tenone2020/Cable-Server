/**
 * Copyright (C) 2013 Open WhisperSystems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.textsecuregcm.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.whispersystems.textsecuregcm.configuration.S3Configuration;

import java.net.URL;
import java.util.Date;

public class UrlSigner {

  private static final long   DURATION = 60 * 60 * 1000;

  private final AWSCredentials credentials;
  private final boolean pathstyle;
  private final boolean noaccelerate;
  private final String bucket;
  private final String endpoint;
  private final String region;
  private final String signer;

  public UrlSigner(S3Configuration config) {
    this.credentials  = new BasicAWSCredentials(config.getAccessKey(), config.getAccessSecret());
    this.pathstyle    = config.getPathStyleAccess();
    this.noaccelerate = config.getDisableAccelerate();
    this.bucket       = config.getAttachmentsBucket();
    this.endpoint     = config.getEndpoint();
    this.signer       = config.getSignerAlgorithm();

    if (config.getRegion() != null && !config.getRegion().isEmpty())
      this.region = config.getRegion();
    else
      // Amazon SDK default
      this.region = "us-east-1";
  }

  public URL getPreSignedUrl(long attachmentId, HttpMethod method) {
    AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard();
    ClientConfiguration clientConfiguration = new ClientConfiguration();

    if (signer != null && !signer.isEmpty()) {
      clientConfiguration.setSignerOverride(signer);
      clientBuilder.setClientConfiguration(clientConfiguration);
    }

    clientBuilder.setCredentials(new AWSStaticCredentialsProvider(credentials));
    clientBuilder.setPathStyleAccessEnabled(pathstyle);

    if (endpoint != null && !endpoint.isEmpty()) {
      AwsClientBuilder.EndpointConfiguration endpointConfiguration =
          new AwsClientBuilder.EndpointConfiguration(endpoint, region);
      clientBuilder.setEndpointConfiguration(endpointConfiguration);
    } else {
      clientBuilder.setRegion(region);
    }

    if (!noaccelerate)
      clientBuilder.enableAccelerateMode();

    AmazonS3 client = clientBuilder.build();

    GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, String.valueOf(attachmentId), method);
    
    request.setExpiration(new Date(System.currentTimeMillis() + DURATION));
    request.setContentType("application/octet-stream");

    return client.generatePresignedUrl(request);
  }

}
