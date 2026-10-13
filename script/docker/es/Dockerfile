# ES 7.17.28 单节点 + IK 7.17.28 中文分词（ROADMAP S16）
#
# 版本锁定依据：sd-blog 依赖 easy-es 3.0.2 → co.elastic.clients:elasticsearch-java:7.17.28，
# 服务端选 7.17.28 与客户端精确匹配（ROADMAP S16 原文"ES 8"为笔误偏差，已登记 §6 S16-D1）。
#
# IK 安装通道：直连 github.com release 下载在本机网络实测 404（2026-10-14），
# 经 ghproxy.com 镜像可达；镜像失效时更换 IK_MIRROR 构建参数或预下载 zip 进构建上下文。
FROM elasticsearch:7.17.28

ARG IK_VERSION=7.17.28
ARG IK_MIRROR=https://ghproxy.com
RUN elasticsearch-plugin install --batch \
    "${IK_MIRROR}/https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v${IK_VERSION}/elasticsearch-analysis-ik-${IK_VERSION}.zip"

# IK 扩展配置 + 自定义技术词典：技术词整词匹配（ext_dict 声明见 IKAnalyzer.cfg.xml）
COPY IKAnalyzer.cfg.xml /usr/share/elasticsearch/config/analysis-ik/IKAnalyzer.cfg.xml
COPY tech.dic /usr/share/elasticsearch/config/analysis-ik/tech.dic

# 词典目录属主修正（插件安装可能改写 config 目录属主，确保 elasticsearch 运行用户可读）
RUN chown -R elasticsearch:root /usr/share/elasticsearch/config/analysis-ik
