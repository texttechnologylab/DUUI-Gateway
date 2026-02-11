![DUUIGatewayImage](page/docs/images/DUUI.svg)
<span style="font-size:5em; display:inline;">Gateway</span>

![GitHub License](https://img.shields.io/github/license/Texttechnologylab/DUUI-Gateway)  [![Paper-SoftwareX](http://img.shields.io/badge/paper-SoftwareX--2026-fb44ce.svg)](https://doi.org/10.1016/j.softx.2026.102549)




[![Discord-Server](http://img.shields.io/badge/Join-Discord_Server-fc0098.svg)](https://discord.gg/DxsgfbK7Jh)


### About
The **Docker Unified UIMA Interface – Gateway** (**DUUIgateway** for short) is a web and REST-based software solution for encapsulating and utilising the [Docker Unified UIMA Interface](https://github.com/texttechnologylab/DockerUnifiedUIMAInterface), a Big Data NLP framework for the automatic processing of heterogeneous NLP tools, based on UIMA and using microservices such as Docker or Kubernetes.

**DUUI** as well as **DUUIgateway** are developed and maintained at the **Texttechnologylab** ([TTLab](https://www.texttechnologylab.org/)) at the Goethe University Frankfurt.


## Introduction

Automatic analysis of large text corpora is a complex task. This complexity particularly concerns the question of time efficiency. Furthermore, efficient, flexible, and extensible textanalysis requires the continuous integration of every new text analysis tools. Since there are currently, in the area of NLP and especially in the application context of UIMA, only very few to no adequate frameworks for these purposes, which are not simultaneously outdated or can no longer be used for security reasons, this work will present a new approach to fill this gap.

*DUUIgateway* is a tool that completely encapsulates DUUI and allows its use in a functional web interface as well as by integrating an API.


## Team

- Cederic Borkowski [<img src="./page/docs/images/github.svg" height="20px">](https://github.com/CedricBorko)
- Prof. Dr. Alexander Mehler  [<img src="./page/docs/images/github.svg" height="20px">](https://github.com/amehler) [<img src="./page/docs/images/rg.svg" height="20px">](https://www.researchgate.net/profile/Alexander-Mehler-2)
- Giuseppe Abrami [<img src="./page/docs/images/github.svg" height="20px">](https://github.com/abrami) [<img src="./page/docs/images/rg.svg" height="20px">](https://www.researchgate.net/profile/Giuseppe-Abrami)
- Dawit Terefe [<img src="./page/docs/images/github.svg" height="20px">](https://github.com/dterefe)


## Usage & Support

To use DUUIgateway, you only need Docker or podman to run a Compose setup. After successful setup, extensive documentation is available in DUUIgateway (cf. [Documentation](https://texttechnologylab.github.io/DUUI-Gateway)).

# Cite
If you want to use the project please quote this as follows:

Cedric Borkowski, Giuseppe Abrami, Dawit Terefe, Daniel Baumartz and Alexander Mehler. 2026. DUUIgateway: A Web Service for Platform-independent, Ubiquitous Big Data NLP. SoftwareX, 34:102549. [PDF](https://www.sciencedirect.com/science/article/pii/S2352711026000439/pdfft?md5=def3fdbbf7b93bcbbac0ad6b96c444f1&pid=1-s2.0-S2352711026000439-main.pdf) [LINK](https://doi.org/10.1016/j.softx.2026.102549)

## BibTeX
```
@article{Borkowski:et:al:2026,
  title     = {DUUIgateway: A Web Service for Platform-independent, Ubiquitous Big Data NLP},
  journal   = {SoftwareX},
  volume = {34},
  pages = {102549},
  year = {2026},
  issn = {2352-7110},
  doi = {https://doi.org/10.1016/j.softx.2026.102549},
  url = {https://www.sciencedirect.com/science/article/pii/S2352711026000439},
  author    = {Borkowski, Cedric and Abrami, Giuseppe and Terefe, Dawit and Baumartz, Daniel
               and Mehler, Alexander},
  keywords  = {duui, neglab},
  abstract  = {Distributed processing of unstructured text data is a challenge
               in the rapidly changing and evolving natural language processing
               (NLP) landscape. This landscape is characterized by heterogeneous
               systems, models, and formats, and especially by the increasing
               influence of AI systems. While many of these systems handle text
               data, there are also unified systems that process multiple input
               and output formats, while allowing for distributed corpus processing.
               However, there are hardly any user-friendly interfaces that allow
               existing NLP frameworks to be used flexibly and extended in a
               user-controlled manner. Due to this gap and the increasing importance
               of NLP for various scientific disciplines, there has been a demand
               for a web and API based flexible software solution for deploying,
               managing and monitoring NLP systems. Such a solution is provided
               by Docker Unified UIMA-gateway. We introduce DUUIgateway and evaluate
               its API and user-driven approach to encapsulation. We also describe
               how these features improve the usability and accessibility of
               the NLP framework DUUI. We illustrate DUUIgateway in the field
               of process modeling in higher education and show how it closes
               the latter gap in NLP by making a variety of systems for processing
               text and multimodal data accessible to non-experts.}
}


```


For support, please contact our [team](#team) or use our dedicated [![Discord-Server](http://img.shields.io/badge/Discord-Server-fc0098.svg)](https://discord.gg/DxsgfbK7Jh)

