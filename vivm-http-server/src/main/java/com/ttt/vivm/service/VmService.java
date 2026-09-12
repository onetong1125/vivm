package com.ttt.vivm.service;

import com.ttt.vivm.cycle.port.VirtualizationGateway;
import com.ttt.vivm.model.*;
import com.ttt.vivm.repository.ImgRepository;
import com.ttt.vivm.repository.ImgVmRepository;
import com.ttt.vivm.repository.UserVmRepository;
import com.ttt.vivm.repository.VmRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VmService {
    private final VirtualizationGateway virtualizationGateway;
    private final VmRepository vmRepository;
    private final UserVmRepository userVmRepository;
    private final ImgVmRepository imgVmRepository;
    private final ImgRepository imgRepository;
    private final ImageService imageService;

    private static final Logger logger = LoggerFactory.getLogger(VmService.class);

    public static final String isoFileDir = "/home/xuyitong/isoFile/";

    public VmService(
            VirtualizationGateway virtualizationGateway,
            VmRepository vmRepository,
            UserVmRepository userVmRepository,
            ImgVmRepository imgVmRepository,
            ImgRepository imgRepository,
            ImageService imageService) {
        this.virtualizationGateway = virtualizationGateway;
        this.vmRepository = vmRepository;
        this.userVmRepository = userVmRepository;
        this.imgVmRepository = imgVmRepository;
        this.imgRepository = imgRepository;
        this.imageService = imageService;
    }


    /**
     * 检查虚拟机是否已经存在
     * @param vmName 虚拟机名称
     * @return true:虚拟机存在; false:虚拟机不存在
     */
    public boolean existsVm(String vmName) {
        return vmRepository.existsByVmName(vmName);
    }
    // --------------------------------创建虚拟机-----------------------------------
    // 创建模版虚拟机
    public void createModelVm(String vmName, Img img, String isoName, User currentUser) {
        String isoPath = isoFileDir + isoName;
        VirtualizationGateway.VirtualMachineRuntime runtime = virtualizationGateway.createModelVm(
                vmName, img.getBackImgPath(), isoPath);

        // 保存虚拟机元数据到数据库
        Vm vm = new Vm();
        vm.setVmName(vmName);
        vm.setVmType(Vm.MODEL_VM_TYPE);
        vm.setVmStatus(runtime.running() ? Vm.RUNNING_STATUS : Vm.STOPPED_STATUS);
        vm.setVmCreateTime(LocalDateTime.now());
        vm.setVmSize(runtime.diskCapacityBytes());
        vm.setVmMac(runtime.macAddress());
        vm.setVmVnc(runtime.vncEndpoint());
        vm.setVmUuid(runtime.uuid());
        vmRepository.save(vm);

        // 保存用户和虚拟机的关系数据到数据库
        UserVmId userVmId = new UserVmId(currentUser.getUserId(), vm.getVmId());
        UserVm userVm = new UserVm();
        userVm.setId(userVmId);
        userVm.setUser(currentUser);
        userVm.setVm(vm);
        userVmRepository.save(userVm);

        // 保存虚拟机和虚拟镜像的关系到数据库
        ImgVmId imgVmId = new ImgVmId(img.getImgId(), vm.getVmId());
        ImgVm imgVm = new ImgVm();
        imgVm.setId(imgVmId);
        imgVm.setImg(img);
        imgVm.setVm(vm);
        imgVmRepository.save(imgVm);

        // 如果虚拟机在运行，锁定虚拟镜像
        if (runtime.running()) {
            imageService.lockImageById(img.getImgId());
        }
    }

    // 创建克隆虚拟机
    public void createCloneVm(String vmName, Img img, User currentUser) {
        VirtualizationGateway.VirtualMachineRuntime runtime = virtualizationGateway.createCloneVm(
                vmName, img.getVmImgPath());

        // 保存虚拟机元数据到数据库
        Vm vm = new Vm();
        vm.setVmName(vmName);
        vm.setVmType(Vm.CLONE_VM_TYPE);
        vm.setVmStatus(runtime.running() ? Vm.RUNNING_STATUS : Vm.STOPPED_STATUS);
        vm.setVmCreateTime(LocalDateTime.now());
        vm.setVmSize(runtime.diskCapacityBytes());
        vm.setVmMac(runtime.macAddress());
        vm.setVmVnc(runtime.vncEndpoint());
        vm.setVmUuid(runtime.uuid());
        vmRepository.save(vm);

        // 保存用户和虚拟机的关系数据到数据库
        UserVmId userVmId = new UserVmId(currentUser.getUserId(), vm.getVmId());
        UserVm userVm = new UserVm();
        userVm.setId(userVmId);
        userVm.setUser(currentUser);
        userVm.setVm(vm);
        userVmRepository.save(userVm);

        // 保存虚拟机和虚拟镜像的关系到数据库
        ImgVmId imgVmId = new ImgVmId(img.getImgId(), vm.getVmId());
        ImgVm imgVm = new ImgVm();
        imgVm.setId(imgVmId);
        imgVm.setImg(img);
        imgVm.setVm(vm);
        imgVmRepository.save(imgVm);

        // 如果虚拟机在运行，锁定虚拟镜像
        if (runtime.running()) {
            imageService.lockImageById(img.getImgId());
        }

    }

    // --------------------------------操作虚拟机-----------------------------------
    // 启动虚拟机
    public String startVm(String vmName) {
        VirtualizationGateway.VirtualMachineRuntime runtime = virtualizationGateway.startVm(vmName);

        // 同步数据库中的虚拟机状态
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);

        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            vm.setVmStatus(runtime.running() ? Vm.RUNNING_STATUS : Vm.STOPPED_STATUS);
            vm.setVmVnc(runtime.vncEndpoint());
            vmRepository.save(vm);   // 保存到数据库
            // 对虚拟机对应的虚拟镜像上锁
            int imgId = vm.getImgVm().getImg().getImgId();
            imageService.lockImageById(imgId);
            return "VM " + vmName + " started";
        }
        return "VM " + vmName + " started but doesn't exist in database.";

    }
    // 关闭虚拟机
    public String shutdownVm(String vmName) {
//        Domain domain = libvirtConnector.getConnection().domainLookupByName(vmName);
//        domain.shutdown();
//        return "VM " + vmName + " shutdown";
        return destroyVm(vmName);
    }

    // 强制关闭虚拟机
    public String destroyVm(String vmName) {
        VirtualizationGateway.VirtualMachineRuntime runtime = virtualizationGateway.forceStopVm(vmName);
        // 同步数据库中的虚拟机状态
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);

        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            vm.setVmStatus(runtime.running() ? Vm.RUNNING_STATUS : Vm.STOPPED_STATUS);
            vmRepository.save(vm);   // 保存到数据库
            // 对虚拟机对应的虚拟镜像解锁
            int imgId = vm.getImgVm().getImg().getImgId();
            imageService.unlockImageById(imgId);
            return "VM " + vmName + " destroyed";
        }
        return "VM " + vmName + " destroyed but doesn't exist in database.";
    }

    // 重启虚拟机
    public String restartVm(String vmName) {
        VirtualizationGateway.VirtualMachineRuntime runtime = virtualizationGateway.restartVm(vmName);
        // 同步数据库中的虚拟机状态
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);

        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            vm.setVmStatus(runtime.running() ? Vm.RUNNING_STATUS : Vm.STOPPED_STATUS);
            vm.setVmVnc(runtime.vncEndpoint());
            vmRepository.save(vm);   // 保存到数据库
            // 对虚拟机对应的虚拟镜像上锁
            int imgId = vm.getImgVm().getImg().getImgId();
            imageService.lockImageById(imgId);
            return "VM " + vmName + " restarted";
        }

        return "VM " + vmName + " restarted but doesn't exist in database.";
    }

    /**
     * 删除虚拟机
     * @param vmName 虚拟机名称
     * @return 操作结果
     */
    @Transactional
    public String deleteVm(String vmName) {
        virtualizationGateway.deleteVm(vmName);
        // 从数据库中查找并删除对应的vm记录，user_vm记录，img_vm记录
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);
        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            int vmId = vm.getVmId();
            // 删除user_vm相关记录
            List<UserVm> userVms = userVmRepository.findByVm_VmId(vmId);
            for (UserVm userVm : userVms) {
                userVmRepository.delete(userVm);
            }
            // 对img解锁（如果上锁了）
            int imgId = vm.getImgVm().getImg().getImgId();
            imageService.unlockImageById(imgId);
            // 在数据库删除img_vm相关记录
            Optional<ImgVm> imgVmOptional = imgVmRepository.findByVm_VmId(vmId);
            ImgVm imgVm = imgVmOptional.get();      // 不检查isPresent，否则会有引用非持久化对象报错
            imgVmRepository.delete(imgVm);
            // 删除数据库中vm相关记录
            vmRepository.delete(vm);
            return "VM " + vmName + " undefined";
        }


        return "VM " + vmName + " undefined but didn't exist in database.";
    }

    // --------------------------------获取虚拟机-----------------------------------

    /**
     * 获取可作为模版虚拟机的列表
     * @return
     */
    public List<Vm> getAllAvailableModelVms() {
        return vmRepository.findByVmStatusNot(Vm.RUNNING_STATUS);
    }

    /**
     * 获取所有虚拟机
     * @return
     */
    public List<Vm> getAllVms() {
        return  vmRepository.findAll();
    }

    /**
     * 根据名称搜索虚拟机
     * @param name
     * @return
     */
    public List<Vm> searchVmsByName(String name) {
        return vmRepository.findByVmNameContaining(name);
    }

    /**
     * 根据ID查找虚拟机
     *
     * @param id 虚拟机ID
     * @return 包含虚拟机信息的Optional对象
     */
    public Optional<Vm> findById(int id) {
        return vmRepository.findById(id);
    }
    // --------------------------------获取虚拟机数据-----------------------------------

    /**
     * 查看是否有以img为镜像的正在运行的虚拟机
     * @param img 镜像对象
     * @return true有正在运行的，false无正在运行的
     */
    public boolean hasRunningVm(Img img) {
        List<ImgVm> imgVms = imgVmRepository.findByImg_ImgId(img.getImgId());
        for (ImgVm imgVm : imgVms) {
            if (imgVm.getVm().getVmStatus() == Vm.RUNNING_STATUS) {
                return  true;
            }
        }
        return false;
    }

}
