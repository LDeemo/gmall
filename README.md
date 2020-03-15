# Gmall服务端口号

gmall-user-service用户服务的service层8070
gmall-user-web用户服务的web层8080

gmall-manage-service用户服务的service层8071
gmall-manage-web用户服务的web层8081

gmall-item-service前台的商品详情服务 8072(没做,重用了manage-service)
gmall-item-web前台的商品详情展示 8082

gmall-search-web搜索服务的前台 8083
gmall-search-service搜索服务的后台 8073

gmall-cart-web搜索服务的前台 8084
gmall-cart-service搜索服务的后台 8074

gmall-passport-web用户认证中心 8085
gmall-user-service用户服务的service层8070

gmall-order-web订单 8086
gmall-order-service订单服务 8076

gmall-payment支付 8087

gware-manage仓库 9001

## 微服务模块
### 后台管理模块
#### AttrController 保存平台属性 获取平台属性和属性值列表
#### CatalogController 获取分类列表
#### SkuController 保存sku
#### SpuController 保存spu 文件上传 获取spu列表 spu销售属性列表 spu图片列表
#### SkuService 获取sku(返回给item) 获取sku(发出消息写入ES)
#### spuService 获取spu列表 图片列表 销售属性列表属性值列表

### 用户模块
#### UserController 保存用户 得到用户收货地址
#### UserService  获取用户收货地址 登录(为passport提供服务) 第三方登录 添加token

### 商品详情模块
#### ItemController 返回商品详情(thymeleaf渲染)

### 搜索模块(首页)
#### SearchController 返回搜索结果商品列表(关键字或者三级分类ID) 面包屑
#### SearchService ES查询商品
#### mq消息监听器consumer 监听sku保存保持事务一致性

### 购物车模块
#### CartController 检查购物车返回最新购物车数据 获取购物车列表 添加购物车
#### CartService 刷新购物车缓存

### 认证中心
#### PassportController 登录并发放token 第三方登录并发放token 认证token

### 订单模块
#### OrderController 提交订单(到支付模块) 结算订单
#### OrderService 获取订单 生成订单码(防止重复提交订单) 检查订单码 保存订单 更新订单(mq发出消息通知仓库)
#### mq消息监听器consumer 更新订单信息(更新外部订单号)

### 支付模块
#### PaymentController 提交到Alipay Alipay同步回调方法
#### PaymentService 保存支付信息 更新支付信息(发出支付成功消息,更新订单) 向Alipay发送延迟消息队列查询订单支付状态
#### mq消息监听器consumer 监听订单检查状态 通过外部订单号检查支付状态

### 仓库系统
#### gware-manage 不属于电商系统,仅整合

### 秒杀系统
#### 一个Demo