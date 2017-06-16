###############
# 环境变量
###############

# 主机IP
OS_ADDR
192.168.1.102

# 路由IP
OS_ROUTE
192.168.1.102:6789

# 数据库IP
OS_DB
192.168.1.102:3306

# 组件仓库地址
OS_REPERTORY
D:\repertory

###############
# MySQL
###############
USE mysql;
UPDATE USER SET HOST='%' WHERE USER='root';
FLUSH PRIVILEGES;

###############
# GitHub
###############
https://github.com/ehangogo/coreos.git
ehangogo
yin19921012

###############
# 主节点管理界面
###############


http://localhost:8080/admin/index.html

###############
# 医疗
###############

# 查询接口
root:nodes;
root:bundles;
root:store;


# 数据连接组件
root:install os.moudel.db.provider.jar 3
root:start os.moudel.db

# 日志组件
root:install os.moudel.log.provider.jar 3
root:start os.moudel.log

# 用户管理组件
root:install os.moudel.user.provider.jar 1
root:start os.moudel.user

# 个人体征组件
root:install os.moudel.person.provider.jar 1
root:start os.moudel.person

# 监护人组件
root:install os.moudel.guard.provider.jar 1
root:start os.moudel.guard

# 医疗Web组件
root:install os.health.application.jar 1
root:start os.health

http://localhost:8083/os.health/login.html

###############
# 动态扩容
###############
root:change os.moudel.guard 2

###############
# 异常恢复
###############
root:check

###############
# 滚动升级
###############
root:update os.moudel.log 20

# 主节点
java -jar master.jar
# 从节点
java -jar slave8081.jar


