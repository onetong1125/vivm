CREATE TABLE img (
    img_id INT NOT NULL AUTO_INCREMENT,
    back_img_path VARCHAR(255),
    create_date DATETIME,
    delete_date DATETIME,
    description VARCHAR(255),
    img_name VARCHAR(255),
    img_size BIGINT,
    img_type INT,
    is_delete INT,
    os_type VARCHAR(255),
    status INT,
    tag VARCHAR(255),
    vm_img_path VARCHAR(255),
    PRIMARY KEY (img_id)
);

CREATE TABLE pool (
    id INT NOT NULL AUTO_INCREMENT,
    dir VARCHAR(255),
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE `user` (
    user_id INT NOT NULL AUTO_INCREMENT,
    balance DECIMAL(9,2) NOT NULL,
    email VARCHAR(255) NOT NULL,
    oss_space BIGINT NOT NULL,
    role INT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_pwd VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_name UNIQUE (user_name)
);

CREATE TABLE version (
    id INT NOT NULL AUTO_INCREMENT,
    create_time DATETIME(6),
    description VARCHAR(255),
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE vm (
    vm_id INT NOT NULL AUTO_INCREMENT,
    vm_create_time DATETIME,
    vm_mac VARCHAR(255),
    vm_name VARCHAR(255) NOT NULL,
    vm_size BIGINT,
    vm_status INT NOT NULL,
    vm_type INT NOT NULL,
    vm_uuid VARCHAR(255),
    vm_vnc VARCHAR(255),
    PRIMARY KEY (vm_id),
    CONSTRAINT uk_vm_name UNIQUE (vm_name)
);

CREATE TABLE img_child (
    img_child_id INT NOT NULL,
    img_id INT NOT NULL,
    PRIMARY KEY (img_child_id, img_id),
    CONSTRAINT fk_img_child_child FOREIGN KEY (img_child_id) REFERENCES img (img_id),
    CONSTRAINT fk_img_child_parent FOREIGN KEY (img_id) REFERENCES img (img_id)
);

CREATE TABLE img_pool (
    img_id INT NOT NULL,
    pool_id INT NOT NULL,
    PRIMARY KEY (img_id, pool_id),
    CONSTRAINT fk_img_pool_image FOREIGN KEY (img_id) REFERENCES img (img_id),
    CONSTRAINT fk_img_pool_pool FOREIGN KEY (pool_id) REFERENCES pool (id)
);

CREATE TABLE img_vm (
    img_id INT NOT NULL,
    vm_id INT NOT NULL,
    PRIMARY KEY (img_id, vm_id),
    CONSTRAINT fk_img_vm_image FOREIGN KEY (img_id) REFERENCES img (img_id),
    CONSTRAINT fk_img_vm_vm FOREIGN KEY (vm_id) REFERENCES vm (vm_id)
);

CREATE TABLE user_img (
    img_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (img_id, user_id),
    CONSTRAINT fk_user_img_image FOREIGN KEY (img_id) REFERENCES img (img_id),
    CONSTRAINT fk_user_img_user FOREIGN KEY (user_id) REFERENCES `user` (user_id)
);

CREATE TABLE user_vm (
    user_id INT NOT NULL,
    vm_id INT NOT NULL,
    PRIMARY KEY (user_id, vm_id),
    CONSTRAINT fk_user_vm_user FOREIGN KEY (user_id) REFERENCES `user` (user_id),
    CONSTRAINT fk_user_vm_vm FOREIGN KEY (vm_id) REFERENCES vm (vm_id)
);

CREATE TABLE version_img (
    img_id INT NOT NULL,
    version_id INT NOT NULL,
    PRIMARY KEY (img_id, version_id),
    CONSTRAINT fk_version_img_image FOREIGN KEY (img_id) REFERENCES img (img_id),
    CONSTRAINT fk_version_img_version FOREIGN KEY (version_id) REFERENCES version (id)
);

CREATE TABLE version_pool (
    pool_id INT NOT NULL,
    version_id INT NOT NULL,
    PRIMARY KEY (pool_id, version_id),
    CONSTRAINT fk_version_pool_pool FOREIGN KEY (pool_id) REFERENCES pool (id),
    CONSTRAINT fk_version_pool_version FOREIGN KEY (version_id) REFERENCES version (id)
);
