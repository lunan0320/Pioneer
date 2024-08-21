[TOC]

### 1. Workflow工作流程

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815195426801.png" alt="Centered Image">
</p>


------

### 2. Pioneer for Android 指南

#### 简介

​	自2019 年末，新冠病毒肆虐全球，寻找新冠感染者的密切接触者成为一大难题。一个地区出现感染者后，如果不及时找到密切接触者，将会造成很严重的后果。但是，寻找密切接触者有时需要耗费大量的人力财力。

​	本款app 旨在准确快速地**追踪病毒感染者的密切接触者**，做好病毒传播初期的防控工作。

1. App 的追踪系统基于**BLE 蓝牙**构建，采用**低功耗**的工作模式，并且可以在后台持续运行，保证准确完整地记录用户的接触信息。
2. 本款app 采用**去中心化**的方式，很好地保护了用户的隐私信息。
3. 安全性方面，本款app 特别使用了**国密算法SM3**，为国内用户提供安全可靠的**加密认证**方式。对于可能预见到的一系列攻击手段，本款app 均已提供防御措施，app 的安全性非常高。

#### 硬件要求

​	Pioneer for Android 对设备最低的系统版本要求是`Android 5.0`，因此要安装并部署Pioneer 必须保证iPhone 的系统版本不低于Android 5.0。

- 如果您使用的iPhone 设备的Android 系统版本低于5.0，不管是从达到Pioneer 的系统版本的需求还是希望您的iPhone 设备得益于Android 高版本的安全补丁而更加安全，我们都建议您更新Android 系统版本以使用Pioneer for Android。
- 对于部署的软件要求，为了工程代码的稳定性，建议使用`Android Studio`打开Pioneer for Android 项目工程并进行查看和部署。

#### 部署指南

1. 安卓项目以压缩包的形式发送，将压缩包解压到一个路径名全为英文的文件下。
1. 打开Android Studio。点击`open`，找到项目文件位置，打开文件，这样就可以查看安卓app 的源码了。
1. 用`Android Studio`将项目打包成apk 文件，把`apk` 文件传送到手机上，即可下载app。

#### 使用指南

##### 1.注册

1. 打开app 后，首先会进入登录界面。首次打开app，用户还没有注册，不能登录，必须要进行`注册`，点击注册按钮即可进入注册界面。

   <p align="center">     <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815191752087.png" width="200" height="350" alt="Centered Image"> </p>

2. 在注册界面，用户可以输入手机号进行注册。如果提示“注册成功”，用户就可以用注册的手机号在登陆界面进行登录。首次登陆成功后app 会保存账号，以后关闭app 再打开就可以直接进入主界面。如果提示“`手机号已被注册`”，说明这个手机号已经被注册过了（即用户在第一次注册成功后，若对同一手机号再次点击注册，则会给出上述提示）。

3. 如果提示`SecretKey` 重复，已重新生成，请再次点击”说明用户app 自动生成的SecretKey 和其他人的重复了，这时app会自动在本地在生成一个新的SecretKey 并覆盖之前的。用户可以再次点击注册用手机号和新的SecretKey 绑定进行注册。

##### 2.发现

1. 用户登录后，app 才开始运行。App 会`自动广播`同时进行扫描，探测周围的开启蓝牙的设备，如果app 通过`UUID` 找到安装相同app 的设备，就会建立连接并交换数据。
2. 用户在“发现”界面可以看到`蓝牙扫描信息`和收到的其他用户的数据包的短名称。列表中会显示数据包的接收时间和最后一次更新时间，当数据包不再更新时，就会显示“停止更新”。点击列表中的数据包，就可以显示出数据包的有效期和`ContactIdentifier`。

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815191846920.png" width="200" height="350" alt="Centered Image">
</p>

##### 3.感染者信息报告

1. 在`Token`界面有一个Token按钮，点击按钮就可以进入Token 验证界面。
2. Token 验证是用来**上传感染者信息**的，如果用户被确诊为感染者，医院的医护人员会交给用户一个Token 验证码。
3. 用户输入Token 验证码进行验证并向服务器上传个人信息。用户如果随意输入字符串进行Token 验证，就会被提示“Token输入有误，请重新输入!”。

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815193205024.png" width="200" height="350" alt="Centered Image">
</p>

##### 4.用户界面

​	在“我”界面。用户可以查看自己手机的**型号**和当前这个**时段**，即当前所处的这**6 分钟内**的数据包的短名称。

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815193830859.png" width="200" height="350" alt="Centered Image">
</p>

##### 5.安全报告

1. App 每天早上8 点会定时**从服务器下载感染者信息**在本地进行**匹配**。当找到匹配时，会弹出提示信息“传感器检测到您与感染者有过接触，请及时到医院进行核酸检测。”

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815193247585.png" width="300" height="100" alt="Centered Image">
</p>



<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815193300689.png" width="300" height="200" alt="Centered Image">
</p>

2. app 运行在前台就会在前台弹出提示，运行在后台就会在状态栏中弹出提示。

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815193315492.png" width="500" height="200" alt="Centered Image">
</p>

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815193223637.png" alt="Centered Image">
</p>

------

### 3. Pioneer for IOS 指南

#### 简介

​	Pioneer for iOS 是低成本病毒追踪系统Pioneer的终端，其服务于iOS 系统下（主要是iPhone 设备）的联系人追踪并将用户的风险接触信息安全地报告给Pioneer 服务器，同时用户也可以随时从服务器中了解到自身的近期安全状态，以便快速做出反应终止风险的继续传播。

#### 主要特点

1. 安全的隐私保障。
2. 有效的交互保障。Pioneer 主要使用低功耗蓝牙传感器，可选地辅以iOS 的iBeacon 和位置传感器，并通过多种技术方法使设备之间的检测率达到90%以上，同时也提供93%以上的连续性测量以给出较为准确的接触数据。
3. 较高的覆盖范围。Pioneer for iOS 对iPhone 系统版本的最低要求是iOS 12.0，正如iOS 框架Network 要求的那样，这可以达到94%的适用率，即可支持最早的iPhone 5S 之后的iPhone 手机。针对iOS-iOS 的部署率可以达到94% × 94% =88.36%。
4. 较低的功耗使用。经过基于iPhone XS 的电池标准的测试，Pioneer for iOS 每小时耗电量低于3%。
5. 持续的安全报告。Pioneer for iOS 会定期（至少每天）与Pioneer 服务器进行安全的信息交互，主要是从服务器中下载风险者身份信息并与用户自身数据库中的交互信息匹配，匹配完成后将以通知的方式告知用户自身的安全报告，使用户详细了解到自身的安全状态。

#### 硬件要求

​	Pioneer for iOS 对设备最低的系统版本要求是iOS 12.0，因此要安装并部署Pioneer 必须保证iPhone 的系统版本不低于iOS 12.0。如果您使用的iPhone 设备的iOS 系统版本低于12.0，不管是从达到Pioneer 的系统版本的需求还是希望您的iPhone 设备得益于iOS 高版本的安全补丁而更加安全，我们都建议您更新iOS系统版本以使用Pioneer for iOS。

> 对于部署的软件要求，为了工程代码的稳定性，建议使用Xcode12.0 以上的版本打开Pioneer for iOS 项目工程并进行查看和部署。

#### 部署指南

​	完成部署首先需要拿到Pioneer 的项目工程包，其中包括Pioneer Framework 和Pioneer for iOS 文件夹。

1. 使用Xcode 打开Pioneer for iOS -> Pioneer for iOS -> Pioneer for ios.xcodeproj 工程项目，并选择Trust and Open。
2. 在TARGETS 菜单中的Signing & Capabilities 中编辑Team 以使用您的开发者账号以及Bundle Identifier。
3. 选择项目目录中的“Pioneer.xcodeproj”，在Signing & Capabilities 中编辑Team以使用您的开发者账号以及Bundle Identifier。
4. 将您的iPhone 设备连接至您的Mac 电脑并信任，Product，Run Pioneer for iOS即可将Pioneer 安装在您希望的iPhone 设备上。
5. 在iPhone 设备上信任您的开发者账号即可开始使用Pioneer for iOS。

#### 使用指南

##### 1.注册

​	Pioneer for iOS 在安装到iPhone 设备之后，首次使用必须注册。目前Pioneer系统要求用户使用手机号作为自身的保密身份标识，因此Pioneer 用户需要使用自己的手机号与服务器建立连接以进行注册。之后用户的注册状态会永久化储存在应用中，下次APP 开启后无需再次注册。

##### 2.发现

在Pioneer for iOS 的正常工作开始之前，应用会申请使用以下的用户隐私权限：

​	蓝牙权限，包括后台使用权限。

​	位置权限，包括后台使用权限，但鉴于不同用户的对此的意见，这并没有作为Pioneer 的必须权限。可选的，是否需要与最终Pioneer 的发行版本有关。

​	通知权限。Pioneer 定期给出的用户安全报告需要定期通知给用户。

​	Pioneer 在通过注册并获得蓝牙使用权限后即可按照标准的工作流程开始工作，并且无需一直占用前台，应用的内部技术实现保证了Pioneer 可以无限期地运行在后台而不被操作系统终止。但是请注意如果用户之后显示地关闭了该应用（例如用户在iPhone 近期运行应用菜单中上滑关闭了Pioneer 等），Pioneer for iOS将终止工作。

##### 3.安全状态

​	发现界面右上角的图标用于指示用户的安全状态，该图标会根据用户的接触

安全状态自动变化，当用户为风险者时就会变化为红色以提示用户。正常的安全

状态下，该图标的颜色是绿色的。

##### 4.用户界面

​	用户可以在个人信息界面看到注册该Pioneer 设备的手机号码和虚拟的唯一身份标识信息。同时Pioneer 允许用户自行查看应用记录的永久化数据，在菜单栏目中点击本地数据库即可查看数据库记录的用户交互信息。

##### 5.感染者信息报告

​	当用户在医疗机构被确诊时，医疗机构将向Pioneer 服务器申请一次性感染者身份信息上传令牌。用户将在医疗机构的帮助下，在Pioneer for iOS 中的Token界面输入一次性Token 令牌，接着Pioneer 将会收集用户的近14 天身份信息作为感染者的身份信息报告给Pioneer 服务器。

##### 6.安全报告

​	Pioneer 会定期（至少每天）从Pioneer 服务器下载风险者身份信息并与本地的数据库交互身份信息进行匹配，匹配结束后会根据用户的接触安全状况出具安全报告，并通过系统通知推送给用户是用户了解自身的安全状况。同时用户也可以进入Pioneer 用户界面查看自身的安全报告。

​	此外，如果匹配时发现了用户与风险者进行过接触（本地数据库中有与风险者的交互记录），Pioneer 后台会自动将用户的近14 天身份信息作为风险者的身份信息报告给Pioneer 服务器。同时通知用户已经与风险者有过密切接触，提醒用户去医疗机构进行检查。

------

### 4.Pioneer Server 服务器指南

#### 使用说明

1. 服务器已经成功部署到远端服务器上，APP 时可直接与服务器进行交互。
2. 远端系统为Linux 操作系统，配置Java JDK 15 环境变量。
3. C/S 交互方式下采用基于SSL/TLS 协议的HTTPS 加密传输协议。
4. 后端数据库采用的是MySQL Database，默认端口3306。在MySQL 中创建数据库Pioneer 作为与客户端APP 交互的数据库。
5. 数据库采用AES 加密存储，并对加密内容十六进制编码。
6. 数据库并发读取，采用MVCC 多版本并发控制。采用InnoDB 搜索引擎，解决了脏读、写阻塞、读并发等问题。
7. 线程池的方式来实现对于多用户请求的安全处理。
8. 设置线程连接超时、会话超时的模式，超时后自动与客户端断开连接。
9. 服务器每天0：00 定时更新MatchingKeys。每一天结束后，在0：00 时刻，服务器会自动对Infected_users 和Contacted_users 表中的MatchingKey 进行更新。更新的目的是为了使数据库中保存的永远是有效的MatchingKey。