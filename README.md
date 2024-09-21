说明：来自谷歌翻译，我觉得不错就翻译了方便日后研究...
# [废弃] Radon Java 字节码混淆器

## Radon 不再维护

需要注意的是，Radon仅用于实验。如果您的软件因为使用 Radon 保护而无法在生产中使用，那么这完全取决于您。

如果您对某些工作原理有疑问，可以加入我的 [Discord](https://discord.gg/RfuxTea) 服务器并询问。

该项目不太可能是生产安全的，也不会没有错误。如果您需要一些指向生产安全（或多或少）且错误较少且维护良好的 Java 字节码混淆器的指针，我建议您使用以下其中一个：

* [Proguard](https://www.guardsquare.com/en/products/proguard) (（主要仅对类/方法/字段名称进行混淆）)
* [Skidfuscator](https://github.com/terminalsin/skidfuscator-java-obfuscator) - [Discord](https://discord.gg/QJC9g8fBU9)
* [Zelix KlassMaster](http://www.zelix.com/)


既然我们讨论的是混淆， 如果你还没有看过 [Recaf](https://github.com/Col-E/Recaf) ([Discord](https://discord.gg/Bya5HaA))，可以考虑看一下。贡献者积极维护 Recaf，以掌握各种现存的混淆技术。对于那些热衷于 Java 逆向工程的人，我强烈建议学习使用 Recaf。

[Skidfuscator](https://github.com/terminalsin/skidfuscator-java-obfuscator) 可能是我见过的最有前途的 Java 字节码混淆项目之一，并且比大多数混淆器（包括 Radon）拥有更完善的代码库管理。我建议任何想要进入 Java 混淆领域的人都花时间学习 Skidfuscator 的工作原理。

另一个资源（略显过时，但仍然包含大量信息）是 GenericException 的 [SkidSuite](https://github.com/GenericException/SkidSuite) 存储库。其中记录了一些在过去几年中变得越来越常见的廉价混淆技术，任何对 Java 逆向工程感兴趣的人都应该知道。

此外，这里还有一些其他的 Java 混淆器/保护器，您可以为了娱乐或学习而查看一下（尽管我不一定推荐使用它们）：

Allatori -
avaj - FOSS。
BisGuard - 
Bozar - FOSS 积分。
Branchlock -
Caesium - FOSS。
ClassGuard -
DashO -
JBCO - FOSS。
JObf - FOSS。
JObfuscator -
NeonObf - 
Obzcure Discord
Paramorphism - Discord
qProtect -
Sandmark - FOSS。
SkidSuite2 - FOSS。
Stringer - 商业版。
yGuard - FOSS。
zProtect - Discord - 

* [Allatori](http://www.allatori.com/) - Commericial. 商业。在行业中颇受欢迎的选择。
* [avaj](https://github.com/cg-dot/avaj) - FOSS. 有一种很好的方法可以生成字符串常量的解密子程序。还有一些 CFG 扁平化，这总是令人欣喜。
* [BisGuard](http://www.bisguard.com/) - Commercial. 商业版。完全依赖类加密（至少我上次检查时是这样的），因此保护措施还有很大的改进空间。
* [Bozar](https://github.com/vimasig/Bozar) - Points for FOSS. 有一些我看到在 Minecraft 社区中使用的廉价技巧。
* [Branchlock](https://branchlock.net/) - Commercial.  商业。在 Minecraft 社区中出现过几次。
* [Caesium](https://github.com/sim0n/Caesium) - FOSS. 有一个转换器，可以将众所周知的 HTML 注入到任何解析 HTML 标签的 Java 逆向工程工具中。
* [ClassGuard](https://zenofx.com/classguard/) - Commercial. 商业。主要依赖于本机库中的类加密和硬编码 AES 密钥。如果您想在博客上展示某件事，那么 IDA/Binary Ninja/Ghidra 练习非常简单。
* [DashO](https://www.preemptive.com/products/dasho/overview) - Commercial.  商业版。在工业界中出现过几次，在流混淆方面有一些有趣的想法（尽管可能已经过时）。
* [JBCO](http://www.sable.mcgill.ca/JBCO/) - FOSS. 一些有趣的流混淆技术在现代 Java 中仍然有效。基于 [Soot](https://github.com/soot-oss/soot)  库，这也是值得一试的。 
* [JObf](https://github.com/superblaubeere27/obfuscator) - FOSS. 相当过时。一些已完成的转换出现在 Minecraft 社区中，因此值得花点时间看看。
* [JObfuscator](https://www.pelock.com/products/jobfuscator) - Commericial.  商业版。之前从未见过这种用法，所以我无法给出任何评论。
* [NeonObf](https://github.com/MoofMonkey/NeonObf) -大部分是 FOSS 的要点。由较容易破解的混淆技术组成。NeonObf 也是 Radon 名称的灵感来源。
* [Obzcure](https://obzcu.re/) [Discord](https://discordapp.com/invite/fUCPxq8)（已停用）- 商业。基于 Web 的混淆服务，灵感来自 Radon 和SkidSuite2。以前使用的名称是“SpigotProtect”，因此如果您仔细查看，您可能会看到一些 [SkidSuite2](https://github.com/GenericException/SkidSuite/tree/master/archive/skidsuite-2) 插件使用此产品的混淆功能。 
* [Paramorphism](https://paramorphism.serenity.enterprises/) - [Discord](https://discordapp.com/invite/k9DPvEy) （已停用）- 商业。是当时最不寻常和最独特的混淆器之一，它是一个活跃的项目，更多地依赖于 JVM 加载 JAR 档案的不同寻常的方式，包括具有重复名称的 zip 条目和伪造目录 [fake directory trick](https://github.com/x4e/fakedirectory) 技巧。在人们开始从 Paramorphism 中窃取想法之前，它曾经更常用。
* [qProtect](https://mdma.dev/) - Commericial.  商业版。将许多更常见的混淆技术实现到单个工具中。在 Minecraft 社区中出现过几次。
* [Sandmark](http://sandmark.cs.arizona.edu) - FOSS. 由亚利桑那大学的 Christian Collberg 领导的非常古老的混淆器研究项目。这里有一些关于水印的有趣想法，一些流混淆想法也很好。
* [SkidSuite2](https://github.com/GenericException/SkidSuite/tree/master/archive/skidsuite-2) - FOSS. 一些非常基本的混淆技术，没有什么特别的。
* [Stringer](https://jfxstore.com/stringer/) - Commercial.商业版-因其基于 AES 的复杂加密/解密程序和价格而臭名昭著。实际上并没有提供很多保护，但有时会出现在行业中。
* [yGuard](https://www.yworks.com/products/yguard) - FOSS. 据我所知，功能上与 Pr​​oGuard 相同。
* [zProtect](https://zprotect.dev/) - [Discord](https://discord.com/invite/dnGKGuwvGH) - Commercial. 商业版。较新的混淆器。我还没有看到过它的任何样本，所以我对它没有意见。

## 构建说明
运行以下命令（希望一切顺利）：
```
./gradlew build
```
或者如果你使用的是 Windows：
```
gradlew.bat build
```

如果该方法不起作用，请改用以下方法：
```
./gradlew clean shadowJar
```

PS 对于那些想知道为什么没有任何预构建版本的人，如果你不知道如何使用 Gradle，你真的应该使用混淆器吗？;) [结束讽刺]

## 常问问题
问：这是否无法破解/无法混淆？
答：不能。没有什么是无法反混淆或逆向工程的。此外，Radon 的反混淆远非难事。如果以 1 到 10 的等级来衡量 Radon 的反混淆难度，我认为在最佳情况下是 2。

问：为什么它是开源的？
答：我制作 Radon 是为了尝试混淆，熟悉 JVM 字节码指令集，并作为代码库，以备有人想捣鼓。此外，我强烈支持 FOSS 思想。

问：Radon 开源了不是会让它更容易去混淆吗？
答：很可能。

问：Spring 应用程序能用这个来混淆吗？
答：开箱即用，不能。从 Radon 3 开始，我永远不会添加对 Spring 或多版本 JAR 的支持。

问：'... 在类路径中未找到“org/somelib/TableFactoryBuilder”' 是什么意思？
答：Radon 根据被混淆的 JAR 的类层次结构在内部确定如何构造类文件中的某些实体。因此，Radon 需要访问项目使用的库类。请确保添加适当的库，这样就不会出现这种情况。

问：我可以使用所有变压器来获得最大程度的保护吗？
答：可以，但请注意，这很可能会导致程序崩溃和/或文件大小和/或性能开销很大。

问：为什么某些变压器组合会破坏程序？
答：Radon 旨在成为一个实验项目，而不是商业产品保护器。并非所有功能都旨在协同工作。

问：运行 Radon 3 需要哪个版本的 Java？
答：Java 11+。

问：我的软件需要用哪个版本的 Java 编写才能与 Radon 配合使用？答：理论上，只要库与类文件版本兼容
，Radon 可以混淆用任何 Java 版本编写的软件。asm

问：Radon 支持 Android 吗？
答：虽然从技术上讲答案是肯定的，但我不会为与 Android 应用有关的问题提供支持。

问：Radon 是否集成了 Gradle、Maven 或 Ant？
答：没有。如果需要，欢迎您提交 PR 来添加此类功能。

问：Radon 3 会有 GUI 吗？
答：有，但是 GUI 在我的优先级中很低，因此可能还需要一段时间才能真正制作出来。

问：会有 Radon 4 吗？
答：可能不会。

## 执照
GNU 通用公共许可证 v3.0（癌症许可证）
