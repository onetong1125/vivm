package com.ttt.vivm.service;

import com.ttt.vivm.cycle.port.ImageStorageGateway;
import com.ttt.vivm.model.Img;
import com.ttt.vivm.model.Pool;
import com.ttt.vivm.repository.ImgRepository;
import com.ttt.vivm.repository.PoolRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PoolService {
    private final PoolRepository poolRepository;
    private final ImgRepository imgRepository;
    private final ImageStorageGateway imageStorageGateway;

    public PoolService(
            PoolRepository poolRepository,
            ImgRepository imgRepository,
            ImageStorageGateway imageStorageGateway) {
        this.poolRepository = poolRepository;
        this.imgRepository = imgRepository;
        this.imageStorageGateway = imageStorageGateway;
    }

    /**
     * 创建镜像池
     * @param poolName 镜像池名称
     */
    public Pool createPool(String poolName) {
        String poolDir = "/var/lib/libvirt/images/" + poolName;
        imageStorageGateway.createPool(poolName, poolDir);

        // 向pool表插入新纪录
        Pool pool = new Pool();
        pool.setName(poolName);
        pool.setDir(poolDir);
        poolRepository.save(pool);
        return pool;
    }

    /**
     * 根据存储池名获取存储池对象
     * @param name 池名
     * @return
     */
    public Optional<Pool> findByName(String name) {
        return poolRepository.findByName(name);
    }

    /**
     * 搜索存储池名
     * @param name
     * @return
     */
    public List<Pool> findByNameContaining(String name) {
        return poolRepository.findByNameContaining(name);
    }
    /**
     * 根据虚拟镜像id找到存储池名称
     * @param imageId 虚拟镜像id
     * @return poolName
     */
    public String findPoolNameByImageId(int imageId) {
        Optional<Img> imgOptional = imgRepository.findById(imageId);
        if (imgOptional.isPresent()) {
            Img img = imgOptional.get();
            return img.getImgPool().getPool().getName();
        }
        return "";
    }

    /**
     * 获取所有存储池对象
     * @return 存储池对象列表
     */
    public List<Pool> getAllPools() {
        return poolRepository.findAll();
    }
}
