package com.ttt.vivm.service;

import com.ttt.vivm.connector.LibvirtConnector;
import com.ttt.vivm.model.*;
import com.ttt.vivm.repository.ImgRepository;
import com.ttt.vivm.repository.ImgVmRepository;
import com.ttt.vivm.repository.UserVmRepository;
import com.ttt.vivm.repository.VmRepository;
import jakarta.transaction.Transactional;
import org.libvirt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

import org.w3c.dom.*;

import java.io.StringReader;

@Service
public class VmService {
    @Autowired
    private LibvirtConnector libvirtConnector;
    @Autowired
    private VmRepository vmRepository;
    @Autowired
    private UserVmRepository userVmRepository;
    @Autowired
    private ImgVmRepository imgVmRepository;
    @Autowired
    private ImgRepository imgRepository;
    @Autowired
    private VmTemplateService vmTemplateService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private SshService sshService;

    private static final Logger logger = LoggerFactory.getLogger(VmService.class);

    public static final String isoFileDir = "/home/xuyitong/isoFile/";



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
    public void createModelVm(String vmName, Img img, String isoName, User currentUser) throws LibvirtException {
        String isoPath = isoFileDir + isoName;
        String xml = vmTemplateService.generateModelVmXML(vmName, img.getBackImgPath(), isoPath);
        System.out.println(vmName + "'s XML: " + xml);
        createVmFromXML(xml);

        // 保存虚拟机元数据到数据库
        Connect conn = libvirtConnector.getConnection();
        Domain domain = conn.domainLookupByName(vmName);
        Vm vm = new Vm();
        vm.setVmName(vmName);
        vm.setVmType(Vm.MODEL_VM_TYPE);
        vm.setVmStatus(domain.isActive());   // 0:stopped   1:running
        vm.setVmCreateTime(LocalDateTime.now());
        vm.setVmSize(domain.blockInfo("vda").getCapacity());
        vm.setVmMac(extractVmMac(domain));
        vm.setVmVnc(extractVmVnc(domain));
        vm.setVmUuid(domain.getUUIDString());
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
        if (domain.isActive() == Vm.RUNNING_STATUS) {
            imageService.lockImageById(img.getImgId());
        }
    }

    // 创建克隆虚拟机
    public void createCloneVm(String vmName, Img img, User currentUser) throws LibvirtException {
        String xml = vmTemplateService.generateCloneVmXML(vmName, img.getVmImgPath());
        createVmFromXML(xml);

        // 保存虚拟机元数据到数据库
        Connect conn = libvirtConnector.getConnection();
        Domain domain = conn.domainLookupByName(vmName);
        Vm vm = new Vm();
        vm.setVmName(vmName);
        vm.setVmType(Vm.CLONE_VM_TYPE);
        vm.setVmStatus(domain.isActive());   // 0:stopped   1:running
        vm.setVmCreateTime(LocalDateTime.now());
        vm.setVmSize(domain.blockInfo("vda").getCapacity());
        vm.setVmMac(extractVmMac(domain));
        vm.setVmVnc(extractVmVnc(domain));
        vm.setVmUuid(domain.getUUIDString());
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
        if (domain.isActive() == Vm.RUNNING_STATUS) {
            imageService.lockImageById(img.getImgId());
        }

    }

    // 通用方法：根据 XML 创建虚拟机
    private void createVmFromXML(String xml) throws LibvirtException {
        Connect conn = libvirtConnector.getConnection();
        conn.domainDefineXML(xml).create();
    }

    // --------------------------------操作虚拟机-----------------------------------
    // 启动虚拟机
    public String startVm(String vmName) throws LibvirtException {
        Map<String, String> response = new HashMap<>();
        Domain domain = libvirtConnector.getConnection().domainLookupByName(vmName);
        // 启动虚拟机
        domain.create();

        // 同步数据库中的虚拟机状态
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);

        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            vm.setVmStatus(domain.isActive());  // 更新状态
            vm.setVmVnc(extractVmVnc(domain));    // 更新VNC
            vmRepository.save(vm);   // 保存到数据库
            // 对虚拟机对应的虚拟镜像上锁
            int imgId = vm.getImgVm().getImg().getImgId();
            imageService.lockImageById(imgId);
            return "VM " + vmName + " started";
        }
        return "VM " + vmName + " started but doesn't exist in database.";

    }
    // 关闭虚拟机
    public String shutdownVm(String vmName) throws LibvirtException {
//        Domain domain = libvirtConnector.getConnection().domainLookupByName(vmName);
//        domain.shutdown();
//        return "VM " + vmName + " shutdown";
        return destroyVm(vmName);
    }

    // 强制关闭虚拟机
    public String destroyVm(String vmName) throws LibvirtException {
        Domain domain = libvirtConnector.getConnection().domainLookupByName(vmName);
        // 强制关机虚拟机
        domain.destroy();
        // 同步数据库中的虚拟机状态
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);

        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            vm.setVmStatus(domain.isActive()); // 更新状态
            vmRepository.save(vm);   // 保存到数据库
            // 对虚拟机对应的虚拟镜像解锁
            int imgId = vm.getImgVm().getImg().getImgId();
            imageService.unlockImageById(imgId);
            return "VM " + vmName + " destroyed";
        }
        return "VM " + vmName + " destroyed but doesn't exist in database.";
    }

    // 重启虚拟机
    public String restartVm(String vmName) throws LibvirtException {
        Domain domain = libvirtConnector.getConnection().domainLookupByName(vmName);
        domain.reboot(0);       // 不延时，直接重启虚拟机
        // 同步数据库中的虚拟机状态
        Optional<Vm> vmOptional = vmRepository.findByVmName(vmName);

        if (vmOptional.isPresent()) {
            Vm vm = vmOptional.get();
            vm.setVmStatus(domain.isActive());  // 更新状态
            vm.setVmVnc(extractVmVnc(domain));    // 更新VNC端口号
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
     * @throws LibvirtException Libvirt API异常
     */
    @Transactional
    public String deleteVm(String vmName) throws LibvirtException {
        Domain domain = libvirtConnector.getConnection().domainLookupByName(vmName);
        if (domain.isActive() == 1) { // 先检查 VM 是否运行
            domain.destroy();           // 强制关闭虚拟机
        }
        // 先在Libvirt中删除虚拟机配置
        domain.undefine();
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

    // --------------------------------私有方法-----------------------------------
    private String extractVmVnc(Domain domain) throws LibvirtException {
        String command = "virsh -c qemu:///system vncdisplay " + domain.getName();
        String result = sshService.executeCommand(command);
        logger.info("Command: {}", command);
        logger.info("VNC port result: {}", result);
        if (result != null && result.startsWith(":")) {
            int vncPort = Integer.parseInt(result.substring(1).trim());
            vncPort += 5900;
            return "219.223.251.95:" + vncPort;
        }
        return null; // 没找到VNC端口
    }

    private String extractVmMac(Domain domain) throws LibvirtException {
        try {
            String xmlDesc = domain.getXMLDesc(0);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xmlDesc)));

            NodeList interfaces = doc.getElementsByTagName("interface");
            for (int i = 0; i < interfaces.getLength(); i++) {
                Element iface = (Element) interfaces.item(i);
                Element macElement = (Element) iface.getElementsByTagName("mac").item(0);
                if (macElement != null) {
                    return macElement.getAttribute("address");
                }
            }
        } catch (LibvirtException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("------Exception in VmService.extarctVmMac------");
            e.printStackTrace();
        }
        return null;
    }
}
