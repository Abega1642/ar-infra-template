package com.example.arInfra.exception.bucket;

public class BucketDirectoryUploadException extends BucketOperationException {

  public BucketDirectoryUploadException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "BUCKET_DIRECTORY_UPLOAD_FAILED";
  }
}
