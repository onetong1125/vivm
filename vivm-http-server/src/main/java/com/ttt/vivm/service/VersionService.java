package com.ttt.vivm.service;

import com.ttt.vivm.cycle.port.BackupGateway;
import com.ttt.vivm.model.*;
import com.ttt.vivm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class VersionService {
    private final VersionRepository versionRepository;
    private final VersionPoolRepository versionPoolRepository;
    private final VersionImgRepository versionImgRepository;
    private final ImgRepository imgRepository;
    private final PoolService poolService;
    private final BackupGateway backupGateway;
    private final ImageService imageService;

    private static final Logger logger = LoggerFactory.getLogger(VersionService.class);

    public VersionService(
            VersionRepository versionRepository,
            VersionPoolRepository versionPoolRepository,
            VersionImgRepository versionImgRepository,
            ImgRepository imgRepository,
            PoolService poolService,
            BackupGateway backupGateway,
            ImageService imageService) {
        this.versionRepository = versionRepository;
        this.versionPoolRepository = versionPoolRepository;
        this.versionImgRepository = versionImgRepository;
        this.imgRepository = imgRepository;
        this.poolService = poolService;
        this.backupGateway = backupGateway;
        this.imageService = imageService;
    }
    // --------------------------------管理版本-----------------------------------
    /**
     * 根据存储池名创建新备份
     * @param poolName 版本链名称（一定存在）
     */
    public void createNewBackup(String poolName, String versionName, String versionDesc) {
        // 1. 运行脚本创建备份
        backupGateway.createBackup(poolName, versionName);

        // 2. 更新数据库
        // 2.1 向version表添加记录
        Version version = new Version();
        version.setName(versionName);
        version.setDescription(versionDesc);
        version.setCreateTime(LocalDateTime.now());
        versionRepository.save(version);
        logger.info("Version saved");

        // 2.2 向version_pool表添加记录
        Optional<Pool> poolOptional = poolService.findByName(poolName);
        Pool pool = poolOptional.get();
        VersionPoolId versionPoolId = new VersionPoolId(version.getId(), pool.getId());
        VersionPool versionPool = new VersionPool();
        versionPool.setVersionPoolId(versionPoolId);
        versionPool.setVersion(version);
        versionPool.setPool(pool);
        versionPoolRepository.save(versionPool);
        logger.info("VersionPool saved!");

        // 2.3 向version_img表添加记录(添加当前未删除的镜像)
        // 获取未删除的镜像列表
        List<Img> imgs = imageService.findUndeletedImgsByPool(pool);
        // 每个镜像添加version_img记录
        for (Img img : imgs) {
            VersionImgId versionImgId = new VersionImgId(version.getId(), img.getImgId());
            VersionImg versionImg = new VersionImg();
            versionImg.setVersionImgId(versionImgId);
            versionImg.setVersion(version);
            versionImg.setImg(img);
            versionImgRepository.save(versionImg);
        }
        logger.info("VersionImg saved!");
    }


    /**
     * 还原一个已备份的版本
     * @param pool 存储池对象
     * @param version 版本对象
     */
    public void restoreBackup(Pool pool, Version version) {
        // 1. 运行脚本还原备份
        backupGateway.restoreBackup(pool.getName(), version.getName());

        // 2. 更新数据库
        // 2.1 把当前pool里未删除的img都设置为已删除且不可用，删除日期添加now
        List<Img> imgs = imageService.findUndeletedImgsByPool(pool);
        for (Img img : imgs) {
            img.setStatus(Img.FILE_LOCKED);
            img.setIsDelete(Img.IS_DELETE);
            img.setDeleteDate(LocalDateTime.now());
            imgRepository.save(img);
        }
        logger.info("Img saved!");
        // 2.2 把还原的版本中img设置为未删除且可用，删除日期设置为空
        imgs = imageService.findImgsByVersion(version);
        for (Img img : imgs) {
            img.setIsDelete(Img.NOT_DELETE);
            img.setStatus(Img.FILE_UNLOCK);
            img.setDeleteDate(null);
            imgRepository.save(img);
        }
        logger.info("Img saved!");
    }

    // --------------------------------获取版本信息-----------------------------------

    /**
     * 存在版本名为name的备份
     * @param name 版本名
     * @return true存在，false不存在
     */
    public boolean existVersionByName(String name) {
        return versionRepository.existsByName(name);
    }

    /**
     * 判断pool存在version版本
     * @param versionId 版本id
     * @param pool 存储池对象
     * @return true存在，false不存在
     */
    public boolean existsByVersionIdAndPool(int versionId, Pool pool) {
        return versionPoolRepository.existsByVersion_IdAndPool_Id(versionId, pool.getId());
    }

    // --------------------------------获取版本-----------------------------------

    /**
     * 根据id获取版本对象
     * @param id 版本id
     * @return
     */
    public Optional<Version> findById(int id) {
        return versionRepository.findById(id);
    }

    /**
     * 获取所有版本
     * @return 版本列表
     */
    public List<Version> getAllVersions() {
        return versionRepository.findAll();
    }

    /**
     * 根据版本名称搜索版本
     * @param name 版本名
     * @return 版本列表
     */
    public List<Version> searchVersionsByName(String name) {
        return versionRepository.findByNameContaining(name);
    }

    /**
     * 根据描述搜索版本
     * @param desc 描述
     * @return 版本列表
     */
    public List<Version> searchVersionsByDesc(String desc) {
        return versionRepository.findByDescriptionContaining(desc);
    }

    /**
     * 根据存储池名搜索版本
     * @param poolName 存储池名
     * @return 版本列表
     */
    public List<Version> searchVersionsByPoolName(String poolName) {
        List<Pool> pools = poolService.findByNameContaining(poolName);
        List<Version> versions = new ArrayList<>();
        for (Pool pool : pools) {
            versions.addAll(findVersionsByPool(pool));
        }
        return versions;
    }

    /**
     * 根据存储池对象获取其所有版本
     * @param pool 存储池对象
     * @return 版本列表
     */
    public List<Version> findVersionsByPool(Pool pool) {
        List<Version> versions = new ArrayList<>();

        List<VersionPool> versionPools = versionPoolRepository.findByPool(pool);
        for (VersionPool versionPool : versionPools) {
            Optional<Version> versionOptional = versionRepository.findById(versionPool.getVersionPoolId().getVersionId());
            versions.add(versionOptional.get());
        }
        return versions;
    }
}
