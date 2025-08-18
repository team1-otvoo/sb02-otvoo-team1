package com.team1.otvoo.storage;

import java.io.InputStream;

public interface S3ImageStorage {

  void upload(String key, InputStream in, long length, String contentType);

  void delete(String key);

  String getPresignedUrl(String key, String contentType);
}
