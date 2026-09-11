package com.ttt.vivm.service;

import org.springframework.stereotype.Service;

@Service
public class ImageTemplateService {
    // 基础镜像模板
    private static final String BASE_IMAGE_TEMPLATE = """
        <volume type='file'>
          <name>{{name}}.qcow2</name>
          <capacity unit='{{unit}}'>{{size}}</capacity>
          <target>
            <format type='qcow2'/>
          </target>
        </volume>
        """;

    // 差异镜像模板
    private static final String DELTA_IMAGE_TEMPLATE = """
        <volume type='file'>
          <name>{{name}}.qcow2</name>
          <capacity unit='{{unit}}'>10</capacity>  <!-- 设置虚拟磁盘大小 -->
          <allocation unit='{{unit}}'>0</allocation>  <!-- 让它按需分配 -->
          <target>
            <format type='qcow2'/>
            <backingStore>
              <path>{{base_image_path}}</path>
              <format type='qcow2'/>
            </backingStore>
          </target>
        </volume>
        """;

    public String generateBaseImageXML(String name, long size, String unit) {
        return BASE_IMAGE_TEMPLATE
                .replace("{{name}}", name)
                .replace("{{size}}", String.valueOf(size))
                .replace("{{unit}}", unit);
    }

    public String generateDeltaImageXML(String name, String baseImagePath, String unit) {
        return DELTA_IMAGE_TEMPLATE
                .replace("{{name}}", name)
                .replace("{{base_image_path}}", baseImagePath)
                .replace("{{unit}}", unit);
    }
}
