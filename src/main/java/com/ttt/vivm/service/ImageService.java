package com.ttt.vivm.service;

import com.jcraft.jsch.JSchException;
import com.ttt.vivm.connector.LibvirtConnector;
import com.ttt.vivm.model.*;
import com.ttt.vivm.repository.*;
import org.libvirt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.io.StringReader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ImageService {
    @Autowired
    private LibvirtConnector libvirtConnector;
    @Autowired
    private ImageTemplateService ImagetemplateService;
    @Autowired
    private ImgRepository imgRepository;
    @Autowired
    private UserImgRepository userImgRepository;
    @Autowired
    private ImgVmRepository imgVmRepository;
    @Autowired
    private ImgChildRepository imgChildRepository;
    @Autowired
    private ImgPoolRepository imgPoolRepository;
    @Autowired
    private VersionImgRepository versionImgRepository;
    @Autowired
    private SshService sshService;
    @Autowired
    private PoolService poolService;
    @Autowired
    private ChildService childService;

    /**
     * 查看某镜像是否存在
     * @param imgName 镜像名
     * @return true：镜像存在；false：镜像不存在
     */
    public boolean existsImage(String imgName) {
        return imgRepository.existsByImgName(imgName);
    }

    // --------------------------------创建虚拟镜像-----------------------------------

    /**
     * 创建基础镜像
     * @param poolName 存储池名称
     * @param imageName 基础镜像名称
     * @param sizeGB 镜像大小
     * @param currentUser 执行操作的用户
     * @return 创建的基础镜像的路径
     * @throws LibvirtException Libvirt API抛出的异常
     */
    public Img createBaseImage(String poolName, String imageName, long sizeGB, User currentUser) throws Exception {
        Connect conn = libvirtConnector.getConnection();
        Pool poolModel = poolService.createPool(poolName);
        StoragePool pool = conn.storagePoolLookupByName(poolName);

        // 生成 XML
        String xml = ImagetemplateService.generateBaseImageXML(imageName, sizeGB, "GiB");

        // 创建基础镜像
        StorageVol vol = pool.storageVolCreateXML(xml, 0);

        // 保存虚拟镜像元数据到数据库
        Img img = new Img();
        img.setImgName(imageName);
        img.setImgSize(vol.getInfo().capacity);             // 存储卷的总容量，以字节为单位
        img.setBackImgPath(vol.getPath());
        img.setImgType(Img.PUBLIC_IMG_TYPE);
        img.setCreateDate(LocalDateTime.now());
        img.setIsDelete(Img.NOT_DELETE);
        img.setStatus(Img.FILE_UNLOCK);
        imgRepository.save(img);

        // 保存用户和虚拟镜像的关系到数据库
        UserImgId userImgId = new UserImgId(currentUser.getUserId(), img.getImgId());
        UserImg userImg = new UserImg();
        userImg.setId(userImgId);
        userImg.setUser(currentUser);
        userImg.setImg(img);
        userImgRepository.save(userImg);

        // 保存虚拟镜像和存储池的关系到数据库
        ImgPoolId imgPoolId = new ImgPoolId(img.getImgId(), poolModel.getId());
        ImgPool imgPool = new ImgPool();
        imgPool.setId(imgPoolId);
        imgPool.setPool(poolModel);
        imgPool.setImg(img);
        imgPoolRepository.save(imgPool);

        System.out.println("Base image created: " + vol.getPath());
        return img;
    }

    /**
     * 创建一个增量镜像
     * @param poolName 增量镜像所存放的Libvirt存储池
     * @param vmImageName 要创建的增量镜像名
     * @param backImagePath 一定存在的。基础镜像路径
     * @param currentUser 进行操作的用户
     * @return 增量镜像的路径
     * @throws JSchException ssh异常
     */
    // 不能通过XML创建，直接运行命令
    public void createDeltaImage(String poolName, String vmImageName, String backImagePath, User currentUser) throws Exception {
        String vmImagePath = String.format("/var/lib/libvirt/images/%s/%s.qcow2", poolName, vmImageName);
        String command1 = String.format("qemu-img create -f qcow2 -b %s %s", backImagePath, vmImagePath);
        String command2 = String.format("virsh -c qemu:///system pool-refresh %s", poolName);

        // 创建克隆镜像
        String result1 = sshService.executeCommand(command1);
        System.out.println("Command: " + command1);
        System.out.println("Result: " + result1);
        // 刷新Libvirt池
        String result2 = sshService.executeCommand(command2);
        System.out.println("Command: " + command2);
        System.out.println("Result: " + result2);

        // 更新数据库
        // 保存虚拟镜像元数据到数据库
        Img img = new Img();
        img.setImgName(vmImageName);
        img.setBackImgPath(backImagePath);
        img.setVmImgPath(vmImagePath);
        img.setImgType(Img.PRIVATE_IMG_TYPE);
        img.setCreateDate(LocalDateTime.now());
        img.setIsDelete(Img.NOT_DELETE);
        img.setStatus(Img.FILE_UNLOCK);
        imgRepository.save(img);
        System.out.println("Delta image created: " + vmImagePath);

        // 保存虚拟镜像和存储池的关系到数据库
        Optional<Pool> poolOptional = poolService.findByName(poolName);
        if (poolOptional.isEmpty()) {
            throw new Exception("pool " + poolName + " doesn't exists in DB!");
        }
        Pool poolModel = poolOptional.get();
        ImgPoolId imgPoolId = new ImgPoolId(img.getImgId(), poolModel.getId());
        ImgPool imgPool = new ImgPool();
        imgPool.setId(imgPoolId);
        imgPool.setPool(poolModel);
        imgPool.setImg(img);
        imgPoolRepository.save(imgPool);

        // 保存用户和虚拟镜像的关系到数据库
        UserImgId userImgId = new UserImgId(currentUser.getUserId(), img.getImgId());
        UserImg userImg = new UserImg();
        userImg.setId(userImgId);
        userImg.setUser(currentUser);
        userImg.setImg(img);
        userImgRepository.save(userImg);
        System.out.println("UserImg saved!");

        // 保存虚拟镜像的父子关系到数据库
        // 获取父镜像对象
        String fileName = Paths.get(backImagePath).getFileName().toString(); // 获取文件名
        String parentImageName = fileName.replaceFirst("[.][^.]+$", ""); // 去掉扩展名
        Optional<Img> parentOptional = imgRepository.findByImgName(parentImageName);
        if (parentOptional.isPresent()) {
            Img parent = parentOptional.get();
            ImgChildId imgChildId = new ImgChildId(parent.getImgId(), img.getImgId());
            ImgChild imgChild = new ImgChild();
            imgChild.setId(imgChildId);
            imgChild.setParentImg(parent);
            imgChild.setChildImg(img);
            imgChildRepository.save(imgChild);
            System.out.println("ImgChild saved. parent: " + parent.getImgName() + ", child: " + img.getImgName());
        }
    }
    // --------------------------------操作虚拟镜像-----------------------------------

    /**
     * 根据镜像id删除虚拟镜像
     * @param imgId 虚拟镜像id
     * @throws Exception 不合理的删除异常
     */
    public void deleteImageByImgId(int imgId) throws Exception{
        System.out.println("-----------------ImageService: deleteImageByImgId-----------------");
        Optional<Img> imgOptional = imgRepository.findById(imgId);
        if (imgOptional.isEmpty()) {
            throw new Exception("Image doesn't exist!");
        }
        Img img = imgOptional.get();
        if (imgVmRepository.existsByImg_ImgId(imgId)) {
            throw new Exception("This Image is in use! Please delete the vm first.");
        }
        if (childService.allChildrenDeletedByParentId(imgId)) {
            // 如果没有未删除子镜像，删除文件
            // 找到此镜像的存储池
            String poolName = poolService.findPoolNameByImageId(imgId);
            String command1 = String.format("virsh -c qemu:///system vol-delete %s.qcow2 --pool %s", img.getImgName(), poolName);
            String command2 = String.format("virsh -c qemu:///system pool-refresh %s", poolName);

            // 删除镜像
            String result1 = sshService.executeCommand(command1);
            System.out.println("Command: " + command1);
            System.out.println("Result: " + result1);
            // 刷新Libvirt池
            String result2 = sshService.executeCommand(command2);
            System.out.println("Command: " + command2);
            System.out.println("Result: " + result2);
        }
        // 标识数据库状态为已删除且不可用，记录删除时间
        img.setIsDelete(Img.IS_DELETE);
        img.setStatus(Img.FILE_LOCKED);
        img.setDeleteDate(LocalDateTime.now());
        imgRepository.save(img);

    }

    /**
     * 根据虚拟镜像id对镜像上锁
     * @param imgId 镜像id
     */
    public void lockImageById(int imgId) {
        Optional<Img> imgOptional = imgRepository.findById(imgId);
        if (imgOptional.isPresent()) {
            Img img = imgOptional.get();
            img.setStatus(Img.FILE_LOCKED);
            imgRepository.save(img);
        }
    }

    /**
     * 根据虚拟镜像id对镜像解锁
     * @param imgId 镜像id
     */
    public void unlockImageById(int imgId) {
        Optional<Img> imgOptional = imgRepository.findById(imgId);
        if (imgOptional.isPresent()) {
            Img img = imgOptional.get();
            img.setStatus(Img.FILE_UNLOCK);
            imgRepository.save(img);
        }
    }
    // --------------------------------获取虚拟镜像-----------------------------------

    /**
     * 根据id获取虚拟镜像
     * @param imgId 虚拟镜像id
     * @return
     */
    public Optional<Img> findById(int imgId) {
        return imgRepository.findById(imgId);
    }

    /**
     * 获取所有虚拟镜像
     * @return 虚拟镜像列表
     */
    public List<Img> getAllImages() {
        return  imgRepository.findAll();
    }

    /**
     * 根据名称搜索虚拟镜像
     * @param name 要搜索的名称（包含）
     * @return
     */
    public List<Img> searchImagesByName(String name) {
        return imgRepository.findByImgNameContaining(name);
    }

    /**
     * 根据镜像名获取虚拟镜像（精准）
     * @param name 镜像名
     * @return
     */
    public Optional<Img> findByImgName(String name) {
        return imgRepository.findByImgName(name);
    }

    /**
     * 根据镜像名获取镜像路径（模版/克隆镜像）
     * @param imgName 镜像名
     * @return 镜像文件路径
     * @throws Exception 镜像名不存在
     */
    public String getImagePathByImgName(String imgName) throws Exception{
        Optional<Img> imgOptional = imgRepository.findByImgName(imgName);
        if (imgOptional.isPresent()) {
            Img img = imgOptional.get();
            // 公开镜像作为模版镜像，只有一个全量镜像文件
            if (img.getImgType() == Img.PUBLIC_IMG_TYPE) {
                System.out.println("Parent image path: " + img.getBackImgPath());
                return img.getBackImgPath();
            }
            // 克隆镜像返回增量文件路径
            else {
                System.out.println("Parent image path: " + img.getVmImgPath());
                return img.getVmImgPath();
            }
        }
        throw new Exception("parent image name doesn't exist");
    }

    /**
     * 根据镜像id获取vm文件路径（克隆镜像）
     * @param id 镜像在数据库中的img_id
     * @return vm文件路径
     */
    public String getVmImagePathById(int id) {
        Optional<Img> imgOptional = imgRepository.findById(id);
        if (imgOptional.isPresent()) {
            return imgOptional.get().getVmImgPath();
        }
        return "";
    }

    /**
     * 根据镜像id获取镜像是否可用
     * @param id 镜像在数据库中的img_id
     * @return true可用，false不可用
     */
    public boolean accessibleImageById(int id) {
        Optional<Img> imgOptional = imgRepository.findById(id);
        if (imgOptional.isPresent()) {
            Img img = imgOptional.get();
            return img.getStatus() != Img.FILE_LOCKED;
        }
        return false;

    }

    /**
     * 根据虚拟镜像名称查看镜像是否已删除
     * @param imgName 虚拟镜像名称
     * @return true已删除或不存在，false未删除
     */
    public boolean isDeletedByImgName(String imgName) {
        Optional<Img> imgOptional = imgRepository.findByImgName(imgName);
        if (imgOptional.isPresent()) {
            Img img = imgOptional.get();
            return img.getIsDelete() == Img.IS_DELETE;
        }
        // 不存在等同 已删除
        return true;
    }

    /**
     * 根据存储池对象获取其中的虚拟镜像列表
     * @param pool 存储池对象
     * @return 虚拟镜像列表
     */
    public List<Img> findImgsByPool(Pool pool) {
        List<Img> imgs = new ArrayList<>();
        List<ImgPool> imgPools = imgPoolRepository.findByPool_Id(pool.getId());
        for (ImgPool imgPool : imgPools) {
            imgs.add(imgPool.getImg());
        }
        return imgs;
    }

    /**
     * 根据pool获取当前未删除的镜像列表
     * @param pool 存储池对象
     * @return 虚拟镜像列表
     */
    public List<Img> findUndeletedImgsByPool(Pool pool) {
        List<Img> imgs = new ArrayList<>();
        List<ImgPool> imgPools = imgPoolRepository.findByPool_Id(pool.getId());
        for (ImgPool imgPool : imgPools) {
            Img img = imgPool.getImg();
            if (img.getIsDelete() == Img.NOT_DELETE) {
                imgs.add(img);
            }
        }
        return imgs;
    }

    /**
     * 根据版本对象获取在此版本里的镜像列表
     * @param version 版本对象
     * @return
     */
    public List<Img> findImgsByVersion(Version version) {
        List<VersionImg> versionImgs = versionImgRepository.findByVersion_Id(version.getId());
        List<Img> imgs = new ArrayList<>();
        for (VersionImg versionImg : versionImgs) {
            imgs.add(versionImg.getImg());
        }
        return imgs;
    }

    private static String extractBaseImagePath(String xmlConfig) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlConfig)));

        NodeList disks = doc.getElementsByTagName("disk");
        for (int i = 0; i < disks.getLength(); i++) {
            Element disk = (Element) disks.item(i);
            if ("file".equals(disk.getAttribute("type"))) {
                Element source = (Element) disk.getElementsByTagName("source").item(0);
                return source.getAttribute("file");  // 获取文件路径
            }
        }
        throw new Exception("no disk image found.");
    }
}
