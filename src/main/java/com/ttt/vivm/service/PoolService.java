package com.ttt.vivm.service;

import com.ttt.vivm.model.Img;
import com.ttt.vivm.model.Pool;
import com.ttt.vivm.repository.ImgRepository;
import com.ttt.vivm.repository.PoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PoolService {
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private ImgRepository imgRepository;
    @Autowired
    private SshService sshService;

    /**
     * 创建镜像池
     * @param poolName 镜像池名称
     */
    public Pool createPool(String poolName) {
        String poolDir = "/var/lib/libvirt/images/" + poolName;
        String command1 = "mkdir " + poolDir;
        String command2 = String.format("virsh -c qemu:///system pool-define-as %s dir --target %s", poolName, poolDir);
        String command3 = "virsh -c qemu:///system pool-autostart " + poolName;
        String command4 = "virsh -c qemu:///system pool-start " + poolName;
        // 创建镜像池
        String result1 = sshService.executeCommand(command1);
        System.out.println("Command: " + command1);
        System.out.println("Result: " + result1);
        String result2 = sshService.executeCommand(command2);
        System.out.println("Command: " + command2);
        System.out.println("Result: " + result2);
        // 设置镜像池自启动，并启动池
        String result3 = sshService.executeCommand(command3);
        System.out.println("Command: " + command3);
        System.out.println("Result: " + result3);
        String result4 = sshService.executeCommand(command4);
        System.out.println("Command: " + command4);
        System.out.println("Result: " + result4);

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
