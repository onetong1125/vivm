package com.ttt.vivm.cycle.port;

public interface ImageStorageGateway {

    void createPool(String poolName, String directory);

    StoredImage createBaseImage(String poolName, String imageName, long sizeGiB);

    String createDeltaImage(String poolName, String imageName, String backingImagePath);

    void deleteImage(String poolName, String imageName);

    record StoredImage(long capacityBytes, String path) {
    }
}
