package com.ttt.vivm.service;

import com.ttt.vivm.model.Img;
import com.ttt.vivm.model.ImgChild;
import com.ttt.vivm.model.ImgChildId;
import com.ttt.vivm.repository.ImgChildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChildService {
    @Autowired
    private ImgChildRepository imgChildRepository;

    /**
     * 根据父镜像id返回是否有未删除子镜像
     * @param parentId 父镜像id
     * @return true子镜像都已删除 false有未删除子镜像
     */
    public boolean allChildrenDeletedByParentId(int parentId) {
        List<ImgChild> imgChildren = imgChildRepository.findByParentImg_ImgId(parentId);
        for (ImgChild imgChild : imgChildren) {
            Img child = imgChild.getChildImg();
            if (child.getIsDelete() == Img.NOT_DELETE) {
                return false;
            }
        }
        return true;
    }
}
