package com.team1.otvoo.storage;

import java.io.IOException;
import java.io.InputStream;

public interface S3ImageStorage {

  String upload(String key, InputStream in, long length, String contentType);

  void delete(String key);

  String getPresignedUrl(String key, String contentType);

  byte[] download(String key) throws IOException;
}
