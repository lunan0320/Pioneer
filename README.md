[TOC]

### Workflow工作流程

<p align="center">
    <img src="https://cdn.jsdelivr.net/gh/lunan0320/pics@main/images/202408/image-20240815195426801.png" alt="Centered Image">
</p>


------

### Pioneer for Android 指南

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