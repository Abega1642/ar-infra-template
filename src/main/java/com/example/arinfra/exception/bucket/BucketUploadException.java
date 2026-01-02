package com.example.arinfra.exception.bucket;

import com.example.arinfra.InfraGenerated;

@InfraGenerated
public class BucketUploadException extends BucketOperationException {

  public BucketUploadException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "BUCKET_UPLOAD_FAILED";
  }
}
