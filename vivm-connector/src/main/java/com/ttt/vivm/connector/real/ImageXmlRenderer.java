package com.ttt.vivm.connector.real;

final class ImageXmlRenderer {

    String baseImage(String name, long sizeGiB) {
        return """
                <volume type='file'>
                  <name>%s.qcow2</name>
                  <capacity unit='GiB'>%d</capacity>
                  <target><format type='qcow2'/></target>
                </volume>
                """.formatted(XmlText.escape(name), sizeGiB);
    }
}
